#!/bin/bash
mvn -f ./lib/java/dbrepo-core/pom.xml -q clean package install -DskipTests
rm -rf ./dbrepo-consumer-service/lib/at/ ./dbrepo-data-service/lib/at/ ./dbrepo-metadata-service/lib/at/ ./dbrepo-replication-service/lib/at/
mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-1.14.1.jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=1.14.1 -Dpackaging=jar -Durl=file:./dbrepo-consumer-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-1.14.1.jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=1.14.1 -Dpackaging=jar -Durl=file:./dbrepo-data-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-1.14.1.jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=1.14.1 -Dpackaging=jar -Durl=file:./dbrepo-metadata-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-1.14.1.jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=1.14.1 -Dpackaging=jar -Durl=file:./dbrepo-replication-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
