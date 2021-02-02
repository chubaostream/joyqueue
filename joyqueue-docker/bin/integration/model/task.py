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


class Task:

    def __init__(self, config):
        self.mq_start_shell_entry = config.get_value('MQEntryPoint', 'Task')
        self.pressure_shell_entry = config.get_value('PressureEntryPoint', 'Task')
        self.pressure_repo = config.get_value('PressureRepo', 'Task')
        self.pressure_repo_name = config.get_value('PressureRepoName', 'Task')
        self.pressure_docker_namespace = config.get_value('PressureDockerNamespace', 'Task')
        self.mq_repo = config.get_value('MQRepo', 'Task')
        self.mq_docker_namespace = config.get_value('MQDockerNamespace', 'Task')
        self.mq_docker_tag = config.get_value('MQDockerTag', 'Task')
        self.mq_home = config.get_value('MQHome', 'Task')
        self.mq_repo_name = config.get_value('MQRepoName', 'Task')
        self.expose_port = config.get_value('expose', 'Task')
        self.mq_start_flag = config.get_value('startedFlag', 'Task')
        self.mq_log_file = config.get_value('MQLogFile', 'Task')


