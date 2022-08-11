all:

config-backend:
	./.fda-deployment/fda-authentication-service/install_smtp

config-frontend:
	./.fda-deployment/fda-ui/install_cert
	docker-compose -f docker-compose.prod.yml config

config-docker:
	docker image pull -q mariadb:10.5 || true > /dev/null

config: config-docker config-frontend config-backend

build-backend-metadata:
	mvn -f ./fda-metadata-db/pom.xml clean install

build-backend-authentication: build-backend-metadata
	mvn -f ./fda-authentication-service/pom.xml clean package -DskipTests

build-backend-identifier: build-backend-metadata
	mvn -f ./fda-identifier-service/pom.xml clean package -DskipTests

build-backend-container: build-backend-metadata
	mvn -f ./fda-container-service/pom.xml clean package -DskipTests

build-backend-database: build-backend-metadata
	mvn -f ./fda-database-service/pom.xml clean package -DskipTests

build-backend-discovery: build-backend-metadata
	mvn -f ./fda-discovery-service/pom.xml clean package -DskipTests

build-backend-gateway: build-backend-metadata
	mvn -f ./fda-gateway-service/pom.xml clean package -DskipTests

build-backend-query: build-backend-metadata
	mvn -f ./fda-query-service/pom.xml clean package -DskipTests

build-backend-table: build-backend-metadata
	mvn -f ./fda-table-service/pom.xml clean package -DskipTests

build-backend: build-backend-metadata build-backend-database build-backend-query build-backend-table build-backend-identifier build-backend-authentication build-backend-container build-backend-discovery build-backend-gateway

build-docker:
	docker-compose build fda-metadata-db
	docker-compose build --parallel

build-docker-sandbox:
	docker-compose -f docker-compose.prod.yml build fda-metadata-db
	docker-compose -f docker-compose.prod.yml build

build-frontend:
	yarn --cwd ./fda-ui install --legacy-peer-deps
	yarn --cwd ./fda-ui run build

tag: tag-identifier tag-container tag-database tag-discovery tag-gateway tag-query tag-table tag-document tag-units tag-broker

tag-identifier:
	docker tag fda-identifier-service:latest fairdataaustria/fda-identifier-service:latest

tag-container:
	docker tag fda-container-service:latest fairdataaustria/fda-container-service:latest

tag-database:
	docker tag fda-database-service:latest fairdataaustria/fda-database-service:latest

tag-discovery:
	docker tag fda-discovery-service:latest fairdataaustria/fda-discovery-service:latest

tag-gateway:
	docker tag fda-gateway-service:latest fairdataaustria/fda-gateway-service:latest

tag-query:
	docker tag fda-query-service:latest fairdataaustria/fda-query-service:latest

tag-table:
	docker tag fda-table-service:latest fairdataaustria/fda-table-service:latest

tag-document:
	docker tag fda-document-service:latest fairdataaustria/fda-document-service:latest

tag-units:
	docker tag fda-units-service:latest fairdataaustria/fda-units-service:latest

tag-broker:
	docker tag fda-units-service:latest fairdataaustria/fda-broker-service:latest

release: tag release-identifier release-container release-database release-discovery release-gateway release-query release-table release-document release-units release-broker

release-identifier:
	docker push fairdataaustria/fda-identifier-service:latest

release-container:
	docker push fairdataaustria/fda-container-service:latest

release-database:
	docker push fairdataaustria/fda-database-service:latest

release-discovery:
	docker push fairdataaustria/fda-discovery-service:latest

release-gateway:
	docker push fairdataaustria/fda-gateway-service:latest

release-query:
	docker push fairdataaustria/fda-query-service:latest

release-table:
	docker push fairdataaustria/fda-table-service:latest

release-document:
	docker push fairdataaustria/fda-document-service:latest

release-units:
	docker push fairdataaustria/fda-units-service:latest

release-broker:
	docker push fairdataaustria/fda-broker-service:latest

test-backend: test-backend-auth test-backend-container test-backend-database test-backend-discovery test-backend-gateway test-backend-query test-backend-table

test-backend-auth:
	mvn -f ./fda-authentication-service/pom.xml clean test verify

test-backend-identifier:
	mvn -f ./fda-identifier-service/pom.xml clean test verify

test-backend-container:
	mvn -f ./fda-container-service/pom.xml clean test verify

test-backend-database:
	mvn -f ./fda-database-service/pom.xml clean test verify

test-backend-discovery:
	mvn -f ./fda-discovery-service/pom.xml clean test verify

test-backend-gateway:
	mvn -f ./fda-gateway-service/pom.xml clean test verify

test-backend-query:
	mvn -f ./fda-query-service/pom.xml clean test verify

test-backend-table:
	mvn -f ./fda-table-service/pom.xml clean test verify

coverage-frontend: clean build-frontend
	yarn --cwd ./fda-ui run coverage || true

test-frontend: clean build-frontend
	yarn --cwd ./fda-ui install
	docker-compose up -d
	yarn --cwd ./fda-ui run test

test: test-backend test-frontend


teardown:
	./.fda-deployment/teardown
