
# Jenkins docker build and start


## Docker build 

docker build -t jenkins/jenkins:lts_python35 .


## Docker run 


### bridge network mode

docker run  -p 80:8080  -v /export/docker/jenkins/jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock -v $(which docker):/usr/bin/docker -u0 -d jenkins/jenkins:lts_python35


### host network mode

docker run  --network host -e JENKINS_OPTS="--httpPort=80" -v /export/docker/jenkins/jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock -v $(which docker):/usr/bin/docker -u0 -d jenkins/jenkins:lts_python35

