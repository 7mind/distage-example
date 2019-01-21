# distage-sample

preriquisists to install:
- java 11
- sbt 1.2.7
- docker

folder *devops* containts postman collection for manual testing 

installation guide
- run docker-compose up (it will ddl table for users's storage)
- make git checkout .idea inside folder in case of running via intellij idea . it will fetch runConfiguration. 
  first is productin (with postgres and akka http client)
  seconds is dummy (mocked in-mem storages)

- to run tests please do follow in root:
  sbt clean update test
