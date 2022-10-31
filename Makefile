.PHONY: clean all

TAG ?= latest

all:

clean:

config-backend:
	./.fda-deployment/fda-authentication-service/install_smtp

config-frontend:
	./.fda-deployment/fda-ui/install_cert
	docker compose -f docker compose.prod.yml config

config-ssl:
	openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./fda-ui-proxy/default/privkey.pem -out ./fda-ui-proxy/default/fullchain.pem -subj '/C=AT/ST=Vienna/L=Vienna/O=Technische Universität Wien/OU=Data Science Group/CN=dbrepo.ossdip.at'

config-docker:
	docker image pull -q mariadb:10.5 || true > /dev/null

config: config-docker config-frontend config-backend

build-backend: build-backend-metadata-db build-backend-database build-backend-query build-backend-table build-backend-identifier build-backend-authentication build-backend-container build-backend-discovery build-backend-gateway build-backend-metadata

build-backend-metadata-db:
	mvn -f ./fda-metadata-db/pom.xml clean install

build-backend-authentication: build-backend-metadata-db
	mvn -f ./fda-authentication-service/pom.xml clean package -DskipTests

build-backend-identifier: build-backend-metadata-db
	mvn -f ./fda-identifier-service/pom.xml clean package -DskipTests

build-backend-table: build-backend-metadata-db
	mvn -f ./fda-table-service/pom.xml clean package -DskipTests

build-backend-container: build-backend-metadata-db
	mvn -f ./fda-container-service/pom.xml clean package -DskipTests

build-backend-database: build-backend-metadata-db
	mvn -f ./fda-database-service/pom.xml clean package -DskipTests

build-backend-discovery: build-backend-metadata-db
	mvn -f ./fda-discovery-service/pom.xml clean package -DskipTests

build-backend-gateway: build-backend-metadata-db
	mvn -f ./fda-gateway-service/pom.xml clean package -DskipTests

build-backend-query: build-backend-metadata-db
	mvn -f ./fda-query-service/pom.xml clean package -DskipTests

build-backend-metadata: build-backend-metadata-db
	mvn -f ./fda-metadata-service/pom.xml clean package -DskipTests

build-docker:
	docker compose build fda-metadata-db
	docker compose build --parallel

build-frontend:
	yarn --cwd ./fda-ui install --legacy-peer-deps
	yarn --cwd ./fda-ui run build

build-clients:
	bash ./.gitlab/swagger/generate.sh

tag: tag-identifier tag-container tag-database tag-discovery tag-gateway tag-query tag-table tag-analyse tag-authentication tag-metadata-db tag-ui tag-units tag-broker tag-ui-proxy tag-metadata

tag-analyse:
	docker tag fda-analyse-service:latest "dbrepo/analyse-service:${TAG}"

tag-authentication:
	docker tag fda-authentication-service:latest "dbrepo/authentication-service:${TAG}"

tag-metadata-db:
	docker tag fda-metadata-db:latest "dbrepo/metadata-db:${TAG}"

tag-ui:
	docker tag fda-ui:latest "dbrepo/ui:${TAG}"

tag-ui-proxy:
	docker tag fda-ui:latest "dbrepo/ui-proxy:${TAG}"

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
	docker tag fda-units-service:latest "dbrepo/units-service:${TAG}"

tag-broker:
	docker tag fda-broker-service:latest "dbrepo/broker-service:${TAG}"

release: build-docker tag release-identifier release-container release-database release-discovery release-gateway release-query release-table release-analyse release-authentication release-metadata-db release-ui release-units release-broker release-ui-proxy release-metadata

release-analyse:
	docker push "dbrepo/analyse-service:${TAG}"

release-authentication:
	docker push "dbrepo/authentication-service:${TAG}"

release-metadata-db: build-docker tag-metadata-db
	docker push "dbrepo/metadata-db:${TAG}"

release-ui:
	docker push "dbrepo/ui:${TAG}"

release-ui-proxy:
	docker push "dbrepo/ui-proxy:${TAG}"

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
	docker push "dbrepo/units-service:${TAG}"

release-broker:
	docker push "dbrepo/broker-service:${TAG}"

release-metadata:
	docker push "dbrepo/metadata-service:${TAG}"

pull: pull-identifier pull-container pull-database pull-discovery pull-gateway pull-query pull-table pull-analyse pull-authentication pull-metadata-db pull-ui pull-units pull-broker pull-ui-proxy pull-metadata

pull-analyse:
	docker pull "dbrepo/analyse-service:${TAG}"

pull-authentication:
	docker pull "dbrepo/authentication-service:${TAG}"

pull-metadata-db:
	docker pull "dbrepo/metadata-db:${TAG}"

pull-ui:
	docker pull "dbrepo/ui:${TAG}"

pull-ui-proxy:
	docker pull "dbrepo/ui-proxy:${TAG}"

pull-identifier:
	docker pull "dbrepo/identifier-service:${TAG}"

pull-container:
	docker pull "dbrepo/container-service:${TAG}"

pull-database:
	docker pull "dbrepo/database-service:${TAG}"

pull-discovery:
	docker pull "dbrepo/discovery-service:${TAG}"

pull-gateway:
	docker pull "dbrepo/gateway-service:${TAG}"

pull-query:
	docker pull "dbrepo/query-service:${TAG}"

pull-table:
	docker pull "dbrepo/table-service:${TAG}"

pull-units:
	docker pull "dbrepo/units-service:${TAG}"

pull-broker:
	docker pull "dbrepo/broker-service:${TAG}"

pull-metadata:
	docker pull "dbrepo/metadata-service:${TAG}"

test-backend: test-backend-auth test-backend-container test-backend-database test-backend-discovery test-backend-gateway test-backend-query test-backend-table test-backend-metadata

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

test-backend-metadata:
	mvn -f ./fda-metadata-service/pom.xml clean test verify

coverage-frontend: clean build-frontend
	yarn --cwd ./fda-ui run coverage || true

test-frontend: clean build-frontend
	yarn --cwd ./fda-ui install
	docker compose up -d
	yarn --cwd ./fda-ui run test

test-clients:
	bash ./.gitlab/test.sh

test: test-backend test-frontend

teardown:
	./.fda-deployment/teardown
