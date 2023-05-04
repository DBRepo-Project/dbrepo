.PHONY: clean all

TAG ?= latest

all:

build-backend: build-metadata-db build-database-service build-query-service build-table-service build-identifier-service build-container-service build-discovery-service build-gateway-service build-metadata-service build-analyse-service build-user-service

build-metadata-db:
	mvn -f ./dbrepo-metadata-db/pom.xml clean install

build-identifier-service: build-metadata-db
	mvn -f ./dbrepo-identifier-service/pom.xml clean package -DskipTests

build-table-service: build-metadata-db
	mvn -f ./dbrepo-table-service/pom.xml clean package -DskipTests

build-container-service: build-metadata-db
	mvn -f ./dbrepo-container-service/pom.xml clean package -DskipTests

build-database-service: build-metadata-db
	mvn -f ./dbrepo-database-service/pom.xml clean package -DskipTests

build-discovery-service: build-metadata-db
	mvn -f ./dbrepo-discovery-service/pom.xml clean package -DskipTests

build-gateway-service: build-metadata-db
	mvn -f ./dbrepo-gateway-service/pom.xml clean package -DskipTests

build-query-service: build-metadata-db
	mvn -f ./dbrepo-query-service/pom.xml clean package -DskipTests

build-metadata-service: build-metadata-db
	mvn -f ./dbrepo-metadata-service/pom.xml clean package -DskipTests

build-user-service: build-metadata-db
	mvn -f ./dbrepo-user-service/pom.xml clean package -DskipTests

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

tag: tag-identifier tag-search tag-container tag-database tag-discovery tag-gateway tag-query tag-table tag-analyse tag-authentication tag-metadata-db tag-ui tag-units tag-broker tag-metadata tag-user

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

tag-user:
	docker tag dbrepo-user-service:latest "dbrepo/user-service:${TAG}"

tag-table:
	docker tag dbrepo-table-service:latest "dbrepo/table-service:${TAG}"

tag-units:
	docker tag dbrepo-semantics-service:latest "dbrepo/semantics-service:${TAG}"

tag-broker:
	docker tag dbrepo-broker-service:latest "dbrepo/broker-service:${TAG}"

tag-search:
	docker tag dbrepo-search-service:latest "dbrepo/search-service:${TAG}"

tag-user:
	docker tag dbrepo-user-service:latest "dbrepo/user-service:${TAG}"

release: build-docker tag release-identifier release-search release-container release-database release-discovery release-gateway release-query release-table release-analyse release-authentication release-metadata-db release-ui release-units release-broker release-metadata release-user

release-analyse: tag-analyse
	docker push "dbrepo/analyse-service:${TAG}"

release-authentication: tag-authentication
	docker push "dbrepo/authentication-service:${TAG}"

release-metadata-db: tag-metadata-db
	docker push "dbrepo/metadata-db:${TAG}"

release-ui: tag-ui
	docker push "dbrepo/ui:${TAG}"

release-identifier: tag-identifier
	docker push "dbrepo/identifier-service:${TAG}"

release-container: tag-container
	docker push "dbrepo/container-service:${TAG}"

release-database: tag-database
	docker push "dbrepo/database-service:${TAG}"

release-discovery: tag-discovery
	docker push "dbrepo/discovery-service:${TAG}"

release-gateway: tag-gateway
	docker push "dbrepo/gateway-service:${TAG}"

release-query: tag-query
	docker push "dbrepo/query-service:${TAG}"

release-user: tag-user
	docker push "dbrepo/user-service:${TAG}"

release-table: tag-table
	docker push "dbrepo/table-service:${TAG}"

release-units: tag-units
	docker push "dbrepo/semantics-service:${TAG}"

release-broker: tag-broker
	docker push "dbrepo/broker-service:${TAG}"

release-search: tag-search
	docker push "dbrepo/search-service:${TAG}"

release-metadata: tag-metadata
	docker push "dbrepo/metadata-service:${TAG}"

test-backend: test-container-service test-database-service test-discovery-service test-gateway-service test-query-service test-table-service test-identifier-service test-metadata-service test-semantics-service test-analyse-service test-user-service

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
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

test-user-service: clean build-metadata-db build-user-service
	mvn -f ./dbrepo-user-service/pom.xml clean test verify

test-semantics-service: build-semantics-service
	bash ./dbrepo-semantics-service/test.sh

test-analyse-service: build-analyse-service
	bash ./dbrepo-analyse-service/test.sh

coverage-frontend: build-frontend
	yarn --cwd ./dbrepo-ui run coverage || true

test-frontend: clean build-frontend
	yarn --cwd ./dbrepo-ui install
	yarn --cwd ./dbrepo-ui run test:unit || true
	yarn --cwd ./dbrepo-ui run coverage || true

clean:
	docker system prune -f --volumes

test-clients:
	bash ./.gitlab/test.sh

test: test-backend test-frontend

teardown:
	./.junit/teardown
