.PHONY: all

TAG ?= latest
TRIVY_VERSION ?= v0.41.0
AZURE_REPO ?= dbrepo.azurecr.io

all: build

build: build-backend build-docker

build-backend: build-metadata-service build-analyse-service build-search-sync-agent

build-search-sync-agent: build-metadata-service
	mvn -f ./dbrepo-search-sync-agent/pom.xml clean package -DskipTests

build-metadata-service:
	mvn -f ./dbrepo-metadata-service/pom.xml clean install -DskipTests

build-analyse-service:
	bash ./dbrepo-analyse-service/build.sh

build-docker:
	docker build -t dbrepo-metadata-service:build --target build dbrepo-metadata-service
	docker build -t dbrepo-search-sync-agent:build --target build dbrepo-search-sync-agent
	docker compose build --parallel

build-frontend:
	yarn --cwd ./dbrepo-ui install --legacy-peer-deps
	yarn --cwd ./dbrepo-ui run build

build-clients:
	bash ./.gitlab/swagger/generate.sh

tag: tag-analyse-service tag-authentication-service tag-metadata-db tag-ui tag-broker-service tag-metadata-service tag-search-sync-agent

tag-analyse-service:
	docker tag dbrepo-analyse-service:latest "dbrepo/analyse-service:${TAG}"
	docker tag dbrepo-analyse-service:latest "${AZURE_REPO}/dbrepo/analyse-service:${TAG}"

tag-authentication-service:
	docker tag dbrepo-authentication-service:latest "dbrepo/authentication-service:${TAG}"
	docker tag dbrepo-authentication-service:latest "${AZURE_REPO}/dbrepo/authentication-service:${TAG}"

tag-metadata-db:
	docker tag dbrepo-metadata-db:latest "dbrepo/metadata-db:${TAG}"
	docker tag dbrepo-metadata-db:latest "${AZURE_REPO}/dbrepo/metadata-db:${TAG}"

tag-ui:
	docker tag dbrepo-ui:latest "dbrepo/ui:${TAG}"
	docker tag dbrepo-ui:latest "${AZURE_REPO}/dbrepo/ui:${TAG}"

tag-search-sync-agent:
	docker tag dbrepo-search-sync-agent:latest "dbrepo/search-sync-agent:${TAG}"
	docker tag dbrepo-search-sync-agent:latest "${AZURE_REPO}/dbrepo/search-sync-agent:${TAG}"

tag-metadata-service:
	docker tag dbrepo-metadata-service:latest "dbrepo/metadata-service:${TAG}"
	docker tag dbrepo-metadata-service:latest "${AZURE_REPO}/dbrepo/metadata-service:${TAG}"

tag-broker-service:
	docker tag dbrepo-broker-service:latest "dbrepo/broker-service:${TAG}"
	docker tag dbrepo-broker-service:latest "${AZURE_REPO}/dbrepo/broker-service:${TAG}"

tag-search-db:
	docker tag dbrepo-search-db:latest "dbrepo/search-db:${TAG}"
	docker tag dbrepo-search-db:latest "${AZURE_REPO}/dbrepo/search-db:${TAG}"

release: build-docker tag release-analyse-service release-authentication-service release-metadata-db release-ui release-broker-service release-metadata-service release-search-sync-agent

release-analyse-service: tag-analyse-service
	docker push "dbrepo/analyse-service:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/analyse-service:${TAG}"

release-authentication-service: tag-authentication-service
	docker push "dbrepo/authentication-service:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/authentication-service:${TAG}"

release-metadata-db: tag-metadata-db
	docker push "dbrepo/metadata-db:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/metadata-db:${TAG}"

release-ui: tag-ui
	docker push "dbrepo/ui:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/ui:${TAG}"

release-search-sync-agent: tag-search-sync-agent
	docker push "dbrepo/search-sync-agent:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/search-sync-agent:${TAG}"

release-broker-service: tag-broker-service
	docker push "dbrepo/broker-service:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/broker-service:${TAG}"

release-search-db: tag-search-db
	docker push "dbrepo/search-db:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/search-db:${TAG}"

release-metadata-service: tag-metadata-service
	docker push "dbrepo/metadata-service:${TAG}"
	docker push "${AZURE_REPO}/dbrepo/metadata-service:${TAG}"

test-backend: test-metadata-service test-analyse-service test-search-sync-agent

test-search-sync-agent: build-search-sync-agent
	mvn -f ./dbrepo-search-sync-agent/pom.xml clean test verify

test-metadata-service: build-metadata-service teardown
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

test-analyse-service: build-analyse-service
	bash ./dbrepo-analyse-service/test.sh

scan: scan-analyse-service scan-authentication-service scan-broker-service scan-gateway-service scan-metadata-db scan-metadata-service scan-search-db scan-ui scan-search-sync-agent scan-data-db

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

scan-gateway-service:
	docker pull "nginx:1.25.0-alpine-slim"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-gateway-service-report.json "nginx:1.25.0-alpine-slim"
	trivy image --insecure --exit-code 0 "nginx:1.25.0-alpine-slim"
	trivy image --insecure --exit-code 1 --severity CRITICAL "nginx:1.25.0-alpine-slim"

scan-metadata-db:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-db-report.json dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 0 dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-db:latest

scan-metadata-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-service-report.json dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 0 dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-service:latest

scan-search-sync-agent:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-sync-agent-report.json dbrepo-search-sync-agent:latest
	trivy image --insecure --exit-code 0 dbrepo-search-sync-agent:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-search-sync-agent:latest

scan-search-db:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "dbrepo-search-db"
	trivy image --insecure --exit-code 0 "dbrepo-search-db"
	trivy image --insecure --exit-code 1 --severity CRITICAL "dbrepo-search-db"

scan-data-db:
	docker pull "bitnami/mariadb:10.5"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "bitnami/mariadb:10.5"
	trivy image --insecure --exit-code 0 "bitnami/mariadb:10.5"
	trivy image --insecure --exit-code 1 --severity CRITICAL "bitnami/mariadb:10.5"

scan-ui:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-ui-report.json dbrepo-ui:latest
	trivy image --insecure --exit-code 0 dbrepo-ui:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-ui:latest

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
