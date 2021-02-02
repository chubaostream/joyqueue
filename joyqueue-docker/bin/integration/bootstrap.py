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

# -*- coding:utf-8 -*-
# !/usr/bin/env python3

import os
import argparse
from shell import  Shell
from workflow import Workflow
from configuration import Configuration
from pathlib import Path


def bootstrap():
    config_file = os.path.dirname(__file__)+'/bootstrap.conf'
    argv = init_config()
    configuration = Configuration(config_file, argv)
    # configuration.set_value('ResultDir', result_path[1])
    workflow = Workflow(configuration)
    workflow.run()


def init_config():
    parser = argparse.ArgumentParser(
        description='Fetch JoyQueue benchmark, then deploy and run.')
    parser.add_argument(
        '-s',
        '--score_file',
        help='score file result path',
        required=True)
    parser.add_argument(
        '-b',
        '--benchmark_config',
        help='benchmark config file root path')
    return parser.parse_args()


def collect_data():
    results_dir = '{}'.format('/export/docker/workspace')
    path = Path(results_dir)
    if not path.exists():
        return 'benchmark result not exist'
    files = os.listdir(results_dir)
    for name in files:
        if name.endswith('.json'):
            fh = open('{}/{}'.format(results_dir, name), 'r')
            score = fh.read()
            print(score)


def pyc_clean():
    shell = Shell()
    clean = """
            find ./ -name *.pyc -delete
          """
    shell.run_local_script(clean)


if __name__ == '__main__':
    bootstrap()
    collect_data()
    pyc_clean()