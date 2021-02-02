#JoyQueue CI/CD integration test

 cd to project root directory and execute following commands:
``` 

 mvn  -Dmaven.test.skip=true -P docker install 
 cd docker/bin
 python3 ./integration/bootstrap.py  -s ./   -b  $(pwd)/integration/benchmark
 
 
```