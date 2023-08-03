.PHONY: all

TAG ?= latest
TRIVY_VERSION ?= v0.41.0
ELASTIC_VERSION ?= 8.7.1
NGINX_VERSION ?= 1.25.0-alpine-slim
HARBOR_REPO ?= s210.dl.hpc.tuwien.ac.at

all: build

build: build-backend build-docker

build-backend: build-metadata-db build-database-service build-query-service build-table-service build-identifier-service build-container-service build-metadata-service build-analyse-service build-user-service build-semantics-service build-search-sync-agent build-data-service

build-metadata-db:
	mvn -f ./dbrepo-metadata-db/pom.xml clean install

build-identifier-service: build-metadata-db
	mvn -f ./dbrepo-identifier-service/pom.xml clean package -DskipTests

build-table-service: build-metadata-db
	mvn -f ./dbrepo-table-service/pom.xml clean package -DskipTests

build-container-service: build-metadata-db
	mvn -f ./dbrepo-container-service/pom.xml clean package -DskipTests

build-search-sync-agent: build-metadata-db
	mvn -f ./dbrepo-search-sync-agent/pom.xml clean package -DskipTests

build-database-service: build-metadata-db
	mvn -f ./dbrepo-database-service/pom.xml clean package -DskipTests

build-query-service: build-metadata-db
	mvn -f ./dbrepo-query-service/pom.xml clean package -DskipTests

build-metadata-service: build-metadata-db
	mvn -f ./dbrepo-metadata-service/pom.xml clean package -DskipTests

build-user-service: build-metadata-db
	mvn -f ./dbrepo-user-service/pom.xml clean package -DskipTests

build-data-service: build-metadata-db
	mvn -f ./dbrepo-data-service/pom.xml clean package -DskipTests

build-semantics-service: build-metadata-db
	mvn -f ./dbrepo-semantics-service/pom.xml clean package -DskipTests

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

tag: tag-identifier-service tag-container-service tag-database-service tag-query-service tag-table-service tag-analyse-service tag-authentication-service tag-metadata-db tag-ui tag-semantics-service tag-broker-service tag-metadata-service tag-user-service tag-search-sync-agent tag-data-service

tag-analyse-service:
	docker tag dbrepo-analyse-service:latest "dbrepo/analyse-service:${TAG}"
	docker tag dbrepo-analyse-service:latest "${HARBOR_REPO}/dbrepo/analyse-service:${TAG}"

tag-authentication-service:
	docker tag dbrepo-authentication-service:latest "dbrepo/authentication-service:${TAG}"
	docker tag dbrepo-authentication-service:latest "${HARBOR_REPO}/dbrepo/authentication-service:${TAG}"

tag-metadata-db:
	docker tag dbrepo-metadata-db:latest "dbrepo/metadata-db:${TAG}"
	docker tag dbrepo-metadata-db:latest "${HARBOR_REPO}/dbrepo/metadata-db:${TAG}"

tag-ui:
	docker tag dbrepo-ui:latest "dbrepo/ui:${TAG}"
	docker tag dbrepo-ui:latest "${HARBOR_REPO}/dbrepo/ui:${TAG}"

tag-identifier-service:
	docker tag dbrepo-identifier-service:latest "dbrepo/identifier-service:${TAG}"
	docker tag dbrepo-identifier-service:latest "${HARBOR_REPO}/dbrepo/identifier-service:${TAG}"

tag-search-sync-agent:
	docker tag dbrepo-search-sync-agent:latest "dbrepo/search-sync-agent:${TAG}"
	docker tag dbrepo-search-sync-agent:latest "${HARBOR_REPO}/dbrepo/search-sync-agent:${TAG}"

tag-metadata-service:
	docker tag dbrepo-metadata-service:latest "dbrepo/metadata-service:${TAG}"
	docker tag dbrepo-metadata-service:latest "${HARBOR_REPO}/dbrepo/metadata-service:${TAG}"

tag-data-service:
	docker tag dbrepo-data-service:latest "dbrepo/data-service:${TAG}"
	docker tag dbrepo-data-service:latest "${HARBOR_REPO}/dbrepo/data-service:${TAG}"

tag-container-service:
	docker tag dbrepo-container-service:latest "dbrepo/container-service:${TAG}"
	docker tag dbrepo-container-service:latest "${HARBOR_REPO}/dbrepo/container-service:${TAG}"

tag-database-service:
	docker tag dbrepo-database-service:latest "dbrepo/database-service:${TAG}"
	docker tag dbrepo-database-service:latest "${HARBOR_REPO}/dbrepo/database-service:${TAG}"

tag-query-service:
	docker tag dbrepo-query-service:latest "dbrepo/query-service:${TAG}"
	docker tag dbrepo-query-service:latest "${HARBOR_REPO}/dbrepo/query-service:${TAG}"

tag-user-service:
	docker tag dbrepo-user-service:latest "dbrepo/user-service:${TAG}"
	docker tag dbrepo-user-service:latest "${HARBOR_REPO}/dbrepo/user-service:${TAG}"

tag-table-service:
	docker tag dbrepo-table-service:latest "dbrepo/table-service:${TAG}"
	docker tag dbrepo-table-service:latest "${HARBOR_REPO}/dbrepo/table-service:${TAG}"

tag-semantics-service:
	docker tag dbrepo-semantics-service:latest "dbrepo/semantics-service:${TAG}"
	docker tag dbrepo-semantics-service:latest "${HARBOR_REPO}/dbrepo/semantics-service:${TAG}"

tag-broker-service:
	docker tag dbrepo-broker-service:latest "dbrepo/broker-service:${TAG}"
	docker tag dbrepo-broker-service:latest "${HARBOR_REPO}/dbrepo/broker-service:${TAG}"

tag-search-db:
	docker tag dbrepo-search-db:latest "dbrepo/search-db:${TAG}"
	docker tag dbrepo-search-db:latest "${HARBOR_REPO}/dbrepo/search-db:${TAG}"

release: build-docker tag release-identifier-service release-container-service release-database-service release-query-service release-table-service release-analyse-service release-authentication-service release-metadata-db release-ui release-semantics-service release-broker-service release-metadata-service release-user-service release-search-sync-agent release-data-service

release-analyse-service: tag-analyse-service
	docker push "dbrepo/analyse-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/analyse-service:${TAG}"

release-authentication-service: tag-authentication-service
	docker push "dbrepo/authentication-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/authentication-service:${TAG}"

release-metadata-db: tag-metadata-db
	docker push "dbrepo/metadata-db:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/metadata-db:${TAG}"

release-ui: tag-ui
	docker push "dbrepo/ui:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/ui:${TAG}"

release-identifier-service: tag-identifier-service
	docker push "dbrepo/identifier-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/identifier-service:${TAG}"

release-search-sync-agent: tag-search-sync-agent
	docker push "dbrepo/search-sync-agent:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/search-sync-agent:${TAG}"

release-container-service: tag-container-service
	docker push "dbrepo/container-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/container-service:${TAG}"

release-database-service: tag-database-service
	docker push "dbrepo/database-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/database-service:${TAG}"

release-query-service: tag-query-service
	docker push "dbrepo/query-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/query-service:${TAG}"

release-user-service: tag-user-service
	docker push "dbrepo/user-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/user-service:${TAG}"

release-table-service: tag-table-service
	docker push "dbrepo/table-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/table-service:${TAG}"

release-semantics-service: tag-semantics-service
	docker push "dbrepo/semantics-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/semantics-service:${TAG}"

release-broker-service: tag-broker-service
	docker push "dbrepo/broker-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/broker-service:${TAG}"

release-metadata-service: tag-metadata-service
	docker push "dbrepo/metadata-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/metadata-service:${TAG}"

release-data-service: tag-data-service
	docker push "dbrepo/data-service:${TAG}"
	docker push "${HARBOR_REPO}/dbrepo/data-service:${TAG}"

test-backend: test-container-service test-database-service test-query-service test-table-service test-identifier-service test-metadata-service test-semantics-service test-analyse-service test-user-service test-search-sync-agent test-data-service

test-identifier-service: build-metadata-db build-identifier-service
	mvn -f ./dbrepo-identifier-service/pom.xml clean test verify

test-container-service: build-metadata-db build-container-service
	mvn -f ./dbrepo-container-service/pom.xml clean test verify

test-database-service: build-metadata-db build-database-service
	docker pull rabbitmq:3-management-alpine
	docker pull elasticsearch:8.7.1
	mvn -f ./dbrepo-database-service/pom.xml clean test verify

test-query-service: build-metadata-db build-query-service
	mvn -f ./dbrepo-query-service/pom.xml clean test verify

test-table-service: build-metadata-db build-table-service
	mvn -f ./dbrepo-table-service/pom.xml clean test verify

test-search-sync-agent: build-metadata-db build-search-sync-agent
	mvn -f ./dbrepo-search-sync-agent/pom.xml clean test verify

test-metadata-service: build-metadata-db build-metadata-service
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

test-data-service: build-metadata-db build-data-service
	mvn -f ./dbrepo-data-service/pom.xml clean test verify

test-user-service: build-metadata-db build-user-service
	mvn -f ./dbrepo-user-service/pom.xml clean test verify

test-semantics-service: build-metadata-db build-semantics-service
	mvn -f ./dbrepo-semantics-service/pom.xml clean test verify

test-analyse-service: build-analyse-service
	bash ./dbrepo-analyse-service/test.sh

scan: scan-analyse-service scan-authentication-service scan-broker-service scan-container-service scan-database-service scan-gateway-service scan-identifier-service scan-metadata-db scan-metadata-service scan-query-service scan-search-db scan-semantics-service scan-table-service scan-ui scan-user-service scan-search-sync-agent scan-data-service

scan-analyse-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-analyse-service-report.json dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 0 dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-analyse-service:latest

scan-authentication-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-authentication-service-report.json dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 0 dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-authentication-service:latest

scan-broker-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-broker-service-report.json dbrepo-broker-service:latest
	trivy image --insecure --exit-code 0 dbrepo-broker-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-broker-service:latest

scan-container-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-container-service-report.json dbrepo-container-service:latest
	trivy image --insecure --exit-code 0 dbrepo-container-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-container-service:latest

scan-database-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-database-service-report.json dbrepo-database-service:latest
	trivy image --insecure --exit-code 0 dbrepo-database-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-database-service:latest

scan-gateway-service:
	docker pull "nginx:${NGINX_VERSION}"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-gateway-service-report.json "nginx:${NGINX_VERSION}"
	trivy image --insecure --exit-code 0 "nginx:${NGINX_VERSION}"
	trivy image --insecure --exit-code 1 --severity CRITICAL "nginx:${NGINX_VERSION}"

scan-identifier-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-identifier-service-report.json dbrepo-identifier-service:latest
	trivy image --insecure --exit-code 0 dbrepo-identifier-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-identifier-service:latest

scan-metadata-db:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-db-report.json dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 0 dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-db:latest

scan-metadata-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-service-report.json dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 0 dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-service:latest

scan-data-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-data-service-report.json dbrepo-data-service:latest
	trivy image --insecure --exit-code 0 dbrepo-data-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-data-service:latest

scan-query-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-query-service-report.json dbrepo-query-service:latest
	trivy image --insecure --exit-code 0 dbrepo-query-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-query-service:latest

scan-search-sync-agent:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-sync-agent-report.json dbrepo-search-sync-agent:latest
	trivy image --insecure --exit-code 0 dbrepo-search-sync-agent:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-search-sync-agent:latest

scan-search-db:
	docker pull "elasticsearch:${ELASTIC_VERSION}"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "elasticsearch:${ELASTIC_VERSION}"
	trivy image --insecure --exit-code 0 "elasticsearch:${ELASTIC_VERSION}"
	trivy image --insecure --exit-code 1 --severity CRITICAL "elasticsearch:${ELASTIC_VERSION}"

scan-semantics-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-semantics-service-report.json dbrepo-semantics-service:latest
	trivy image --insecure --exit-code 0 dbrepo-semantics-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-semantics-service:latest

scan-table-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-table-service-report.json dbrepo-table-service:latest
	trivy image --insecure --exit-code 0 dbrepo-table-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-table-service:latest

scan-ui:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-ui-report.json dbrepo-ui:latest
	trivy image --insecure --exit-code 0 dbrepo-ui:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-ui:latest

scan-user-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-user-service-report.json dbrepo-user-service:latest
	trivy image --insecure --exit-code 0 dbrepo-user-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-user-service:latest

coverage-frontend: build-frontend
	yarn --cwd ./dbrepo-ui run coverage || true

test-frontend: build-frontend
	yarn --cwd ./dbrepo-ui install
	yarn --cwd ./dbrepo-ui run test:unit || true
	yarn --cwd ./dbrepo-ui run coverage || true

test-clients:
	bash ./.gitlab/test.sh

test: test-backend test-frontend

teardown:
	./.scripts/teardown.sh
