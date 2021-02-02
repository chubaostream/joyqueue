#!/bin/sh
#
# Copyright 2019 The JoyQueue Authors.
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


# start to test

python3 ./integration/bootstrap.py $*
while getopts "s:b::" opt; do
  case $opt in
    s) score=$OPTARG;;
  esac
done
if [ -e $score/score.json ];then
    exit 0
else
    exit 1
fi
echo 'rm log'
if [ -f 'integration/log.txt' ]; then
    rm integration/log.txt
fi
