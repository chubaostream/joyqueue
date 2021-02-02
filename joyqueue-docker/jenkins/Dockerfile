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

FROM jenkins/jenkins:lts

ENV DEBIAN_FRONTEND noninteractive
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

USER root
RUN apt-get update -y && apt-get install maven -y
RUN apt-get install -y  --no-install-recommends apt-utils net-tools
RUN apt-get install vim -y
RUN java -version && ls -l /usr/bin/java

# Python Version
ARG ver=3.5

RUN apt-get update && apt-get install -y \
    software-properties-common  sshpass

RUN apt-get update \
    && apt-get dist-upgrade -y \
    && apt-get -y install python"${ver}" \
       libffi-dev \
       libpq-dev \
       libssl-dev \
       python3-dev \
       python3-pip \
       python3-setuptools \
       python3-venv \
       python3-wheel \
       build-essential \
    && rm -rf /var/lib/apt/lists/* \
    && ln -nsf /usr/bin/python"${ver}" /usr/bin/python

RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

ENV DEBIAN_FRONTEND teletype
USER jenkins
