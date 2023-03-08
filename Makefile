.PHONY: clean all

TAG ?= latest

all:

build-backend: build-metadata-db build-database-service build-query-service build-table-service build-identifier-service build-authentication-service build-container-service build-discovery-service build-gateway-service build-metadata-service build-analyse-service

build-metadata-db:
	mvn -f ./fda-metadata-db/pom.xml clean install

build-authentication-service: build-metadata-db
	mvn -f ./fda-authentication-service/pom.xml clean package -DskipTests

build-identifier-service: build-metadata-db
	mvn -f ./fda-identifier-service/pom.xml clean package -DskipTests

build-table-service: build-metadata-db
	mvn -f ./fda-table-service/pom.xml clean package -DskipTests

build-container-service: build-metadata-db
	mvn -f ./fda-container-service/pom.xml clean package -DskipTests

build-database-service: build-metadata-db
	mvn -f ./fda-database-service/pom.xml clean package -DskipTests

build-discovery-service: build-metadata-db
	mvn -f ./fda-discovery-service/pom.xml clean package -DskipTests

build-gateway-service: build-metadata-db
	mvn -f ./fda-gateway-service/pom.xml clean package -DskipTests

build-query-service: build-metadata-db
	mvn -f ./fda-query-service/pom.xml clean package -DskipTests

build-metadata-service: build-metadata-db
	mvn -f ./fda-metadata-service/pom.xml clean package -DskipTests

build-semantics-service:
	bash ./fda-semantics-service/build.sh

build-analyse-service:
	bash ./fda-analyse-service/build.sh

build-docker:
	docker compose build fda-metadata-db
	docker compose build --parallel

build-frontend:
	yarn --cwd ./fda-ui install --legacy-peer-deps
	yarn --cwd ./fda-ui run build

build-clients:
	bash ./.gitlab/swagger/generate.sh

tag: tag-identifier tag-search tag-container tag-database tag-discovery tag-gateway tag-query tag-table tag-analyse tag-authentication tag-metadata-db tag-ui tag-units tag-broker tag-metadata

tag-analyse:
	docker tag fda-analyse-service:latest "dbrepo/analyse-service:${TAG}"

tag-authentication:
	docker tag fda-authentication-service:latest "dbrepo/authentication-service:${TAG}"

tag-metadata-db:
	docker tag fda-metadata-db:latest "dbrepo/metadata-db:${TAG}"

tag-ui:
	docker tag fda-ui:latest "dbrepo/ui:${TAG}"

tag-identifier:
	docker tag fda-identifier-service:latest "dbrepo/identifier-service:${TAG}"

tag-metadata:
	docker tag fda-metadata-service:latest "dbrepo/metadata-service:${TAG}"

tag-container:
	docker tag fda-container-service:latest "dbrepo/container-service:${TAG}"

tag-database:
	docker tag fda-database-service:latest "dbrepo/database-service:${TAG}"

tag-discovery:
	docker tag fda-discovery-service:latest "dbrepo/discovery-service:${TAG}"

tag-gateway:
	docker tag fda-gateway-service:latest "dbrepo/gateway-service:${TAG}"

tag-query:
	docker tag fda-query-service:latest "dbrepo/query-service:${TAG}"

tag-table:
	docker tag fda-table-service:latest "dbrepo/table-service:${TAG}"

tag-units:
	docker tag fda-semantics-service:latest "dbrepo/semantics-service:${TAG}"

tag-broker:
	docker tag fda-broker-service:latest "dbrepo/broker-service:${TAG}"

tag-search:
	docker tag fda-search-service:latest "dbrepo/search-service:${TAG}"

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
	mvn -f ./fda-authentication-service/pom.xml clean test verify

test-identifier-service: clean build-metadata-db build-identifier-service
	mvn -f ./fda-identifier-service/pom.xml clean test verify

test-container-service: clean build-metadata-db build-container-service
	mvn -f ./fda-container-service/pom.xml clean test verify

test-database-service: clean build-metadata-db build-database-service
	docker pull rabbitmq:3-management-alpine
	mvn -f ./fda-database-service/pom.xml clean test verify

test-discovery-service: clean build-metadata-db build-discovery-service
	mvn -f ./fda-discovery-service/pom.xml clean test verify

test-gateway-service: clean build-metadata-db build-gateway-service
	mvn -f ./fda-gateway-service/pom.xml clean test verify

test-query-service: clean build-metadata-db build-query-service
	mvn -f ./fda-query-service/pom.xml clean test verify

test-table-service: clean build-metadata-db build-table-service
	mvn -f ./fda-table-service/pom.xml clean test verify

test-metadata-service: clean build-metadata-db build-metadata-service
	mvn -f ./fda-metadata-service/pom.xml clean test verify

test-semantics-service: build-semantics-service
	bash ./fda-semantics-service/test.sh

test-analyse-service: build-analyse-service
	bash ./fda-analyse-service/test.sh

coverage-frontend: build-frontend
	yarn --cwd ./fda-ui run coverage || true

test-frontend: clean build-frontend
	yarn --cwd ./fda-ui install
	yarn --cwd ./fda-ui run test:unit || true

clean:
	docker system prune -f --volumes

test-clients:
	bash ./.gitlab/test.sh

test: test-backend test-frontend

teardown:
	./.junit/teardown
