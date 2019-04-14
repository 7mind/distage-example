# distage-sample

Install requirements:
- java 11
- sbt 1.2.7
- docker

*devops* directory contains `postman` requests for manual testing 

installation guide
- run `docker-compose up` (it will ddl table for users's storage)
- If using Intellij IDEA run `git checkout .idea` to fetch `runConfigurations`:
  - First configuration will launch the app in production mode (with postgres and akka http client)
  - Second configuration is dummy (with mock in-memory database)

- to run tests execute the following:
  `sbt clean update test`
