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


import logging
import subprocess
from shlex import split


class Shell:
    def __init__(self):
        self.__logger = logging.getLogger(__name__)
        self.__logger.setLevel(logging.DEBUG)
        handler = logging.FileHandler("log.txt")
        handler.setLevel(logging.DEBUG)
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)
        self.__logger.addHandler(handler)

    def __run_script(self, bash, script):
        self.__logger.debug('Script to execute:\n%s\n', script)
        with subprocess.Popen(
                bash,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT
        ) as proc:
            outs, errs = proc.communicate(script.encode('utf-8'))
            code = proc.returncode
            self.__logger.debug('Return code = %s', code)
            self.__logger.debug('The output is as following:\n%s', str(outs, encoding= 'utf-8'))
            return code, outs, errs

    def run_local_script(self, script):
        return self.__run_script(['/bin/bash'], script)

    def run_remote_script(self, remote, script, port=22, user='root'):
        ssh = 'ssh {}@{} -p {}'.format(user, remote, port)
        bash = split(ssh) + ['/bin/bash']
        script ="""
                source /etc/profile
               """+script
        return self.__run_script(bash, script)

    def local_scp(self, file, remote, target_dir, port=22, user='root'):
        scp = """ scp -P {} {} {}@{}:{} """.format(port, file, user, remote, target_dir)
        return self.run_local_script(scp)

    def remote_scp_dir(self, remote, remote_dir, file, local_dir, port=22, user='root'):
        scp = """
                scp -P {port} {user}@{remote}:{remote_dir}/{file} {local_dir} 
                """.format(port=port,
                           remote_dir=remote_dir,
                           file=file,
                           local_dir=local_dir,
                           remote=remote, user=user,)
        return self.run_local_script(scp)

