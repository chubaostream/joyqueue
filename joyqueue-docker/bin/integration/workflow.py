#
#  Copyright [2020] JD.com, Inc. TIG. ChubaoStream team.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# !/usr/bin/env python3


import subprocess
from pathlib import Path
import os
import datetime
import logging
import time
import json
import fcntl
from model.task import Task
from model.workspace import Workspace


FAILED_TO_LOCK_LOCAL_WORKSPACE = 501
FAILED_TO_PREPARE_PRESSURE = 502
FAILED_TO_PREPARE_MQ_DOCKER = 503
FAILED_TO_INVOKE_MQ_DOCKER = 504
ONLINE_MODE = 'online'


class Workflow:

    def __init__(self, config):
        self.config = config
        self.score_dir = config.get_argv().score_file
        self.benchmark_config = config.get_argv().benchmark_config
        self.task = Task(config)
        self.workspace = Workspace(config)
        self.ssh_user = config.get_value('User', 'SSH')
        self.ssh_passwd_file = config.get_value('PasswdFile', 'SSH')
        self.ssh_port = config.get_value('Port', 'SSH')
        self.subnet_ip = config.get_value('ip', 'Subnet')
        self.subnet_name = config.get_value('name', 'Subnet')
        self.mode = config.get_value('Mode')
        self.pwd = os.path.dirname(__file__)
        self.mq_tag = str(time.time()).replace('.', '_')
        self._benchmark_pressure_tag_id = None
        self.logs_dir = os.path.join(self.pwd, '../logs/')
        self.logger = logging.getLogger(__name__)
        self.lockfile = self.__lockfile()
        self.logger.setLevel(logging.DEBUG)
        handler = logging.FileHandler("log.txt")
        handler.setLevel(logging.DEBUG)
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)
        self.logger.addHandler(handler)

    def run(self):
        start = time.time()
        result = 'unknown'
        try:
            self.__lock_local_workspace()
            self.__preclean_container()
            self.__create_subnet()
            self.__prepare_pressure_worker()
            self.__start_mq_cluster_workers()
            self.__check_mq_cluster_state()
            self.__start_pressure_worker()
            self.__collect_data_from_pressure_worker()
            result = self.__collect_data()
            self.logger.info('Workflow successful!')
        except WorkflowError as err:
            self.logger.error('Failed to execute workflow.')
        finally:
            self.__cleanup()
            self.__remove_subnet()
        self.logger.info('clean up done!')
        end = time.time()
        self.__save_result(result)
        self.logger.info('Time used: %s', datetime.timedelta(seconds=(end - start)))

    def __create_subnet(self):
        subnet = """
                subnet=$(docker network ls|grep {name})
                if [ -n "$subnet" ];then
                    echo 'subnet {name} exist'
                else 
                    docker network create --subnet {ip} {name}
                fi
                """.format(ip=self.subnet_ip, name=self.subnet_name)
        _, out, _ = self.__run_local_script(subnet)
        self.logger.info('create network {},result {}'.format(self.subnet_name, out))

    def __remove_subnet(self):
        subnet_rm = """
                      docker network rm {}
                    """.format(self.subnet_name)
        self.__run_local_script(subnet_rm)

    def __save_result(self, result):
        try:
            if result == 'unknown':
                return None
            path = Path(self.score_dir)
            if not path.exists():
                path.mkdir(parents=True)
            filename = '{}/{}'.format(self.score_dir, self.workspace.result_file)
            fh = open(filename, mode='w')
            fh.write(str(result))
        except IOError:
            self.logger.error('failed to save result into {}'.format(filename))
        fh.close

    def __prepare_pressure_worker(self):
        self.logger.info('prepare pressure docker')
        path = Path(self.workspace.home)
        if not path.exists():
            path.mkdir(parents=True)
        script = """
            cd {home}
            # empty dir
            if [[ $(ls -A {home}) && -d {home}/{repo_name} ]]; then 
                rm -r {home}/{repo_name}
            else
                echo '{home} is clean'
                ls -l 
            fi      
            git clone {repo}
            cd {repo_name}
            git checkout cloud_test
            if [[ "$(ls -A {benchmark})" ]]; then 
                cp -r {benchmark}/* ./
            else
                echo 'use default benchmark config'
            fi
            # license format 
            mvn com.mycila:license-maven-plugin:3.0:format -f pom.xml
            mvn -P docker install 
        """.format(home=self.workspace.home,
                   repo=self.task.pressure_repo,
                   repo_name=self.task.pressure_repo_name,
                   benchmark=self.benchmark_config).rstrip()
        code, outs, _ = self.__run_local_script(script)
        if code != 0:
            raise WorkflowError(
                'Failed to prepare pressure docker {}'.format(self.task.pressure_repo),
                error_code=FAILED_TO_PREPARE_PRESSURE)

    def __lockfile(self):
        path = Path(self.workspace.home)
        if not path.exists():
            path.mkdir(parents=True)
        return open('{}/_filelock'.format(self.workspace.home), 'w')

    def __start_pressure_worker(self):
        self._benchmark_pressure_tag_id = int(time.time())
        script = """
                    docker run --network {subnet} --name {image_name}_{tag_id} {image_namepsace}/{image_name} {entry} -d {drivers}  {workloads}
                 """.format(tag_id=self._benchmark_pressure_tag_id,
                            image_namepsace=self.task.pressure_docker_namespace,
                            image_name=self.task.pressure_repo_name,
                            entry=self.task.pressure_shell_entry,
                            drivers=self.__list_drivers(),
                            subnet=self.subnet_name,
                            workloads=self.__list_workloads())
        code, outs, _ = self.__run_local_script(script)
        if code != 0:
            raise WorkflowError(
                'Failed to start pressure docker {}'.format(self.task.pressure_repo),
                error_code=FAILED_TO_PREPARE_PRESSURE)

    def __collect_data_from_pressure_worker(self):
        script = """
            containerId=$(docker ps -a|grep {repo_name}_{tag_id}|awk '{{print $1}}')
            if [[ -n $containerId ]]; then
                docker cp  $containerId:/benchmark {home}
            else 
                echo '{repo_name} docker not found,failed to collect pressure result'
                exit 1    
            fi
        """.format(repo_name=self.task.pressure_repo_name,
                   tag_id=self._benchmark_pressure_tag_id,
                   home=self.workspace.home)
        self.__run_local_script(script)

    def __collect_data(self, sub_dir='benchmark'):
        self.logger.info('>>> Collect data.')
        results_dir = '{}/{}'.format(self.workspace.home, sub_dir)
        path = Path(results_dir)
        if not path.exists():
            return 'benchmark result not exist'
        files = os.listdir(results_dir)
        scores = "Openmessaging benchmark:\n\n"
        for name in files:
            if name.endswith('.json'):
                fh = open('{}/{}'.format(results_dir, name), 'r')
                score = fh.read()
                jsonScore = json.loads(score)
                scores += self.__format_perf(jsonScore)
        return scores

    def __list_workloads(self):
        workloads_dir = "{}/workloads".format(self.benchmark_config)
        files = os.listdir(workloads_dir)
        self.logger.info(' workloads/'.join(files))
        return ' workloads/'+' workloads/'.join(files)

    def __list_drivers(self):
        drivers = []
        drivers_dir = "{}/".format(self.benchmark_config)
        files = os.listdir(drivers_dir)
        for file in files:
            if file.startswith('driver'):
                drivers.append((file+'/')+(','+file+'/').join(os.listdir( drivers_dir + '/'+file)))
        return ','.join(drivers)

    def __start_mq_cluster_workers(self):
        for h in self.workspace.cluster_hosts:
            self.__invoke_mq_worker(h)

    def __check_mq_cluster_state(self):
        for h in self.workspace.cluster_hosts:
            code, outs, _ = self.__check_mq_stat(h)
            if code != 0:
                raise WorkflowError(
                    'Failed to start {} mq docker {}'.format(h, self.task.mq_repo_name),
                    error_code=FAILED_TO_INVOKE_MQ_DOCKER)

    def __check_mq_stat(self, host, max_attempts=60, sleep=5):
        cidfile ='run_{}.cid'.format(host)
        script = """
                full_cid=$(cat {cidfile})
                echo $full_cid
                containerId=$(docker ps --filter id=$full_cid|grep {docker_namespace}/{repo_name}|awk '{{print $1}}')
                if [[ -z "$containerId" ]]; then
                    echo 'docker container {repo_name} no exist,something wrong'
                    exit 1
                else
                    ATTEMPTS=0
                    MAX_ATTEMPTS={max_attempts}
                    containerId=$(echo "$containerId"|sed ':a;N;$!ba;s/\\n/ /g') 
                    # echo "docker exec $containerId cat {mq_log_file}|grep {started_flag} "
                    while true; do
                        started=$(docker exec $containerId cat {mq_log_file}|grep {started_flag})
                        echo "check state: $started"
                        if [[ -n $started ]]; then
                           exit 0
                        elif [[ $ATTEMPTS -eq $MAX_ATTEMPTS ]]; then
                            echo "mq instance not started after $ATTEMPTS attempts."
                            docker cp $containerId:logs/debug.log {workspace_home}
                            exit 1
                        fi   
                        ATTEMPTS=$((ATTEMPTS+1))
                        sleep {sleep} 
                    done  
                fi
        """.format(workspace_home=self.workspace.home, docker_namespace=self.task.mq_docker_namespace,
                   repo_name=self.task.mq_repo_name,
                   mq_home=self.task.mq_home,
                   mq_log_file=self.task.mq_log_file,
                   max_attempts=max_attempts,
                   sleep=sleep,
                   cidfile=cidfile,
                   started_flag=self.task.mq_start_flag,
                   tag=self.mq_tag).rstrip()
        return self.__run_local_script(script)

    def __invoke_mq_worker(self, host):
        tag_id = int(time.time())
        script = """
                docker run --network {subnet} --ip {ip}  --expose={expose} --cidfile run_{ip}.cid \
                --name {mq_docker_name}_{tag_id}  -d {mq_docker_namespace}/{mq_docker_name} {entry}
        """.format(mq_home=self.task.mq_home,
                   mq_docker_namespace=self.task.mq_docker_namespace,
                   mq_docker_name=self.task.mq_repo_name,
                   subnet=self.subnet_name,
                   ip=host,
                   expose=self.task.expose_port,
                   tag_id=tag_id,
                   entry=self.task.mq_start_shell_entry).rstrip()

        code, outs, _ = self.__run_local_script(script)
        if code != 0:
            raise WorkflowError(
                'Failed to invoke mq docker {} on {}'.format(self.task.mq_repo_name, host),
                error_code=FAILED_TO_INVOKE_MQ_DOCKER)

    def __shutdown_mq_worker(self, host):
        script = self.__shutdown_mq_worker_script(host)
        code, outs, _ = self.__run_local_script(script)
        if code != 0:
            self.logger.error("failed to stop {} mq worker".format(host))

    def __shutdown_mq_worker_script(self, host):
        cidfile ='run_{}.cid'.format(host)
        script = """
                docker ps 
                full_cid=$(cat {cidfile})
                echo $full_cid
                containerId=$(docker ps --filter id=$full_cid|grep {docker_namespace}/{repo_name}|awk '{{print $1}}')
                if [[ -z "$containerId" ]]; then
                    echo 'docker container {repo_name} no exist'
                else
                    echo "{repo_name} container:$containerId"
                    docker stop $containerId
                    docker rm -f $containerId
                fi  
            """.format(docker_namespace=self.task.mq_docker_namespace,
                       repo_name=self.task.mq_repo_name,
                       cidfile=cidfile,
                       tag=self.mq_tag).rstrip()
        return script

    def __preclean_container(self):
        clean = """
              ls -l ./
              docker container prune -f
              mq_containerId=$(docker ps |grep {mq_repo_name}|awk '{{print $1}}'|uniq)
              pressure_containerId=$(docker ps |grep {pressure_repo_name}|awk '{{print $1}}'|uniq)
              if [ -n "$mq_containerId" ];then
                    docker stop $(mq_containerId)
                    docker rm $(mq_containerId)
              else
                    echo "no mq container is running!"
              fi  
              if [ -n "$pressure_containerId" ];then
                    docker stop $(pressure_containerId) 
                    docker rm $(pressure_containerId) 
              else
                    echo "no benchmark container is running!"
              fi
               
              rm *.cid
              """.format(mq_docker_namespace=self.task.mq_docker_namespace,
                         mq_repo_name=self.task.mq_repo_name,
                         pressure_repo_name=self.task.pressure_repo_name)
        self.__run_local_script(clean)

    def __cleanup_local_docker(self):
        # script = self.__shutdown_cleanup_mq_worker_script()
        script = """
                # docker system prune -f
                mq_image_id=$(docker images |grep {mq_repo_name}|awk '{{print $3}}'|uniq)
                if [[ -n $mq_image_id ]]; then
                    docker rmi -f $mq_image_id
                else
                    echo 'docker image {mq_repo_name} not exist'
                fi
                pressure_image_id=$(docker images |grep {pressure_repo_name}|awk '{{print $3}}'|uniq)
                if [[ -n $pressure_image_id ]]; then
                    docker rmi -f $pressure_image_id
                else
                    echo 'docker image {pressure_repo_name} not exist'
                fi
                docker images
                docker ps -a 
        """.format(mq_repo_name=self.task.mq_repo_name,
                   pressure_repo_name=self.task.pressure_repo_name,
                   mq_home=self.task.mq_home).rstrip()
        self.__run_local_script(script)

    def __cleanup_workspace(self):
        script = """
                rm *.cid
                if [[ -d {workspace_home}/{pressure_repo_name} ]]; then
                    rm -r {workspace_home}/{pressure_repo_name}
                fi
                if [[ -d {workspace_home}/benchmark ]]; then
                    rm -r {workspace_home}/benchmark
                fi
                ls -l {workspace_home}
        """.format(workspace_home=self.workspace.home,pressure_repo_name=self.task.pressure_repo_name, mq_home=self.task.mq_home).rstrip()
        self.__run_local_script(script)

    def __shutdown_mq_worker_cluster(self):
        for h in self.workspace.cluster_hosts:
            self.__shutdown_mq_worker(h)

    def __run_script(self, bash, script):
        self.logger.debug('Script to execute:\n%s\n', script)
        with subprocess.Popen(
                bash,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT
        ) as proc:
            outs, errs = proc.communicate(script.encode('utf-8'))
            code = proc.returncode
            self.logger.debug('Return code = %s', code)
            self.logger.debug('The output is as following:\n%s',str(outs, encoding= 'utf-8'))
            return code, outs, errs

    def __run_local_script(self, script):
        return self.__run_script(['/bin/bash'], script)

    def __lock_local_workspace(self):
        self.logger.info('>>> Try lock local workspace.')
        try:
            fcntl.flock(self.lockfile.fileno(), fcntl.LOCK_EX)
            self.logger.info('>>> Get local workspace lock successful!.')
        except IOError as err:
            self.logger.info('>>> Lock local workspace .', err)

    def __format_perf(self, performance):
        p = performance
        table_name = p['driver'] + ' ' + p['workload']+'\n\n'
        pub = p['publishRate']
        con = p['consumeRate']
        pub_latencyMax = p['publishLatencyMax']
        pub_latencyAvg = p['publishLatencyAvg']
        pub_latency50pct = p['publishLatency50pct']
        pub_latency75pct = p['publishLatency75pct']
        pub_latency95pct = p['publishLatency95pct']
        pub_latency99pct = p['publishLatency99pct']
        pub_latency999pct = p['publishLatency999pct']
        pub_latency9999pct = p['publishLatency9999pct']
        header = ['Pub rate(msg/s)', 'Cons rate(msg/s)',  'Tp50(ms)', 'Tp75(ms)', 'Tp95(ms)', 'Tp99(ms)', 'Tp999(ms)', 'Tp9999(ms)', 'Tpavg(ms)', 'Tpmax(ms)']
        rows = []
        for i in range(len(p['publishRate'])):
            row = [int(pub[i]), int(con[i]), pub_latency50pct[i], pub_latency75pct[i],
                   pub_latency95pct[i], pub_latency99pct[i], pub_latency999pct[i], pub_latency9999pct[i], pub_latencyAvg[i], pub_latencyMax[i]]
            rows.append(row)
        return self.__markdown_table(table_name, header, rows)

    def __markdown_table(self, table_name, header, rows):
        table = table_name
        table += '|'+'|'.join(header)+'|\n'
        align = '|'
        for i in header:
            align += ':---:|'
        table += align+'\n'
        for row in rows:
            table += '|'+'|'.join(map(str, row))+'|\n'
        return table

    def __cleanup(self):
        self.logger.info('>>> start to clean up workspace.')
        self.__shutdown_mq_worker_cluster()
        # self.__cleanup_local_docker()
        self.__cleanup_workspace()
        self.__unlock_local_task_home()

    def __unlock_local_task_home(self):
        self.logger.info('>>> Unlock local workspace.')
        fcntl.lockf(self.lockfile.fileno(), fcntl.LOCK_UN)


class WorkflowError(Exception):

    def __init__(self, message, error_code=9999):
        self.message = message
        self.error_code = error_code


