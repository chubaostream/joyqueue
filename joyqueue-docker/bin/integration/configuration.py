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

import configparser;
import os

DEFAULT_SECTION = 'Default'


class Configuration:
    def __init__(self,config_file, argv):
        self.config = configparser.ConfigParser()
        self.config.read(config_file);
        self.argv = argv

    def get_value(self, key, section=DEFAULT_SECTION):
        return self.config.get(section, key);

    def set_value(self, key, value, section=DEFAULT_SECTION):
        self.config.set(self,section, key, value)

    def get_argv(self):
        return self.argv

