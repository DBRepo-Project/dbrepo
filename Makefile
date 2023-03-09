.PHONY: clean all

TAG ?= latest

all:

clean:
	bash ./.dbrepo2/clean.sh

build-backend: build-backend-metadata-db build-backend-database build-backend-query build-backend-table build-backend-identifier build-backend-authentication build-backend-container build-backend-discovery build-backend-gateway build-backend-metadata build-analyse-service

build-backend-metadata-db:
	mvn -f ./dbrepo-metadata-db/pom.xml clean install

build-backend-authentication: build-backend-metadata-db
	mvn -f ./dbrepo-authentication-service/pom.xml clean package -DskipTests

build-backend-identifier: build-backend-metadata-db
	mvn -f ./dbrepo-identifier-service/pom.xml clean package -DskipTests

build-backend-table: build-backend-metadata-db
	mvn -f ./dbrepo-table-service/pom.xml clean package -DskipTests

build-backend-container: build-backend-metadata-db
	mvn -f ./dbrepo-container-service/pom.xml clean package -DskipTests

build-backend-database: build-backend-metadata-db
	mvn -f ./dbrepo-database-service/pom.xml clean package -DskipTests

build-backend-discovery: build-backend-metadata-db
	mvn -f ./dbrepo-discovery-service/pom.xml clean package -DskipTests

build-backend-gateway: build-backend-metadata-db
	mvn -f ./dbrepo-gateway-service/pom.xml clean package -DskipTests

build-backend-query: build-backend-metadata-db
	mvn -f ./dbrepo-query-service/pom.xml clean package -DskipTests

build-backend-metadata: build-backend-metadata-db
	mvn -f ./dbrepo-metadata-service/pom.xml clean package -DskipTests

build-semantics-service:
	bash ./dbrepo-semantics-service/build.sh

build-analyse-service:
	bash ./dbrepo-analyse-service/build.sh

build-docker:
	docker compose build dbrepo-metadata-db
	docker compose build --parallel

build-frontend:
	yarn --cwd ./dbrepo-ui install --legacy-peer-deps
	yarn --cwd ./dbrepo-ui run build

build-clients:
	bash ./.gitlab/swagger/generate.sh

tag: tag-identifier tag-search tag-container tag-database tag-discovery tag-gateway tag-query tag-table tag-analyse tag-authentication tag-metadata-db tag-ui tag-units tag-broker tag-metadata

tag-analyse:
	docker tag dbrepo-analyse-service:latest "dbrepo/analyse-service:${TAG}"

tag-authentication:
	docker tag dbrepo-authentication-service:latest "dbrepo/authentication-service:${TAG}"

tag-metadata-db:
	docker tag dbrepo-metadata-db:latest "dbrepo/metadata-db:${TAG}"

tag-ui:
	docker tag dbrepo-ui:latest "dbrepo/ui:${TAG}"

tag-identifier:
	docker tag dbrepo-identifier-service:latest "dbrepo/identifier-service:${TAG}"

tag-metadata:
	docker tag dbrepo-metadata-service:latest "dbrepo/metadata-service:${TAG}"

tag-container:
	docker tag dbrepo-container-service:latest "dbrepo/container-service:${TAG}"

tag-database:
	docker tag dbrepo-database-service:latest "dbrepo/database-service:${TAG}"

tag-discovery:
	docker tag dbrepo-discovery-service:latest "dbrepo/discovery-service:${TAG}"

tag-gateway:
	docker tag dbrepo-gateway-service:latest "dbrepo/gateway-service:${TAG}"

tag-query:
	docker tag dbrepo-query-service:latest "dbrepo/query-service:${TAG}"

tag-table:
	docker tag dbrepo-table-service:latest "dbrepo/table-service:${TAG}"

tag-units:
	docker tag dbrepo-semantics-service:latest "dbrepo/semantics-service:${TAG}"

tag-broker:
	docker tag dbrepo-broker-service:latest "dbrepo/broker-service:${TAG}"

tag-search:
	docker tag dbrepo-search-service:latest "dbrepo/search-service:${TAG}"

release: build-docker tag release-identifier release-search release-container release-database release-discovery release-gateway release-query release-table release-analyse release-authentication release-metadata-db release-ui release-units release-broker release-metadata

release-analyse:
	docker push "dbrepo/analyse-service:${TAG}"

release-authentication:
	docker push "dbrepo/authentication-service:${TAG}"

release-metadata-db: build-docker tag-metadata-db
	docker push "dbrepo/metadata-db:${TAG}"

release-ui:
	docker push "dbrepo/ui:${TAG}"

release-identifier:
	docker push "dbrepo/identifier-service:${TAG}"

release-container:
	docker push "dbrepo/container-service:${TAG}"

release-database:
	docker push "dbrepo/database-service:${TAG}"

release-discovery:
	docker push "dbrepo/discovery-service:${TAG}"

release-gateway:
	docker push "dbrepo/gateway-service:${TAG}"

release-query:
	docker push "dbrepo/query-service:${TAG}"

release-table:
	docker push "dbrepo/table-service:${TAG}"

release-units:
	docker push "dbrepo/semantics-service:${TAG}"

release-broker:
	docker push "dbrepo/broker-service:${TAG}"

release-search:
	docker push "dbrepo/search-service:${TAG}"

release-metadata:
	docker push "dbrepo/metadata-service:${TAG}"

test-backend: test-authentication-service test-container-service test-database-service test-discovery-service test-gateway-service test-query-service test-table-service test-identifier-service test-metadata-service test-semantics-service test-analyse-service

test-authentication-service: clean build-metadata-db build-authentication-service
	docker pull rabbitmq:3-management-alpine
	mvn -f ./dbrepo-authentication-service/pom.xml clean test verify

test-identifier-service: clean build-metadata-db build-identifier-service
	mvn -f ./dbrepo-identifier-service/pom.xml clean test verify

test-container-service: clean build-metadata-db build-container-service
	mvn -f ./dbrepo-container-service/pom.xml clean test verify

test-database-service: clean build-metadata-db build-database-service
	docker pull rabbitmq:3-management-alpine
	mvn -f ./dbrepo-database-service/pom.xml clean test verify

test-discovery-service: clean build-metadata-db build-discovery-service
	mvn -f ./dbrepo-discovery-service/pom.xml clean test verify

test-gateway-service: clean build-metadata-db build-gateway-service
	mvn -f ./dbrepo-gateway-service/pom.xml clean test verify

test-query-service: clean build-metadata-db build-query-service
	mvn -f ./dbrepo-query-service/pom.xml clean test verify

test-table-service: clean build-metadata-db build-table-service
	mvn -f ./dbrepo-table-service/pom.xml clean test verify

test-metadata-service: clean build-metadata-db build-metadata-service
	mvn -f ./fda-metadata-service/pom.xml clean test verify

test-semantics-service: build-semantics-service
	bash ./dbrepo-semantics-service/test.sh

test-analyse-service: build-analyse-service
	bash ./dbrepo-analyse-service/test.sh

coverage-frontend: clean build-frontend
	yarn --cwd ./dbrepo-ui run coverage || true

test-frontend: clean build-frontend
	yarn --cwd ./dbrepo-ui install
	docker compose up -d
	yarn --cwd ./dbrepo-ui run test

test-clients:
	bash ./.gitlab/test.sh

test: test-backend test-frontend

teardown:
	./.junit/teardown
