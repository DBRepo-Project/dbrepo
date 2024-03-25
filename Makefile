.PHONY: all

TAG ?= latest
REPOSITORY_1_URL ?= docker.io/dbrepo
REPOSITORY_2_URL ?= s210.dl.hpc.tuwien.ac.at/dbrepo

all: build

clean:
	rm -rf ./dist || true
	rm -f .env || true
	docker container stop $(docker container ls -aq) || true
	docker container rm $(docker container ls -aq) || true
	docker volume rm $(docker volume ls -q) || true

build: build-backend build-docker

build-backend: build-metadata-service build-analyse-service build-data-service

build-data-service: build-metadata-service
	mvn -f ./dbrepo-data-service/pom.xml clean package -DskipTests

build-metadata-service:
	mvn -f ./dbrepo-metadata-service/pom.xml clean install -DskipTests

build-analyse-service:
	bash ./dbrepo-analyse-service/build.sh

build-lib-python:
	bash ./lib/python/build.sh

build-docker:
	bash ./bin/build-docker.sh

build-frontend:
	yarn --cwd ./dbrepo-ui install --legacy-peer-deps
	yarn --cwd ./dbrepo-ui run build

build-swagger:
	bash ./.docs/generate.sh

build-helm:
	helm package ./helm-charts/dbrepo --destination ./build

tag: tag-analyse-service tag-authentication-service tag-metadata-db tag-ui tag-metadata-service tag-data-service tag-search-db tag-search-db-init tag-search-service tag-data-db-sidecar

tag-analyse-service:
	docker tag dbrepo-analyse-service:latest "${REPOSITORY_1_URL}/analyse-service:${TAG}"
	docker tag dbrepo-analyse-service:latest "${REPOSITORY_2_URL}/analyse-service:${TAG}"

tag-authentication-service:
	docker tag dbrepo-authentication-service:latest "${REPOSITORY_1_URL}/authentication-service:${TAG}"
	docker tag dbrepo-authentication-service:latest "${REPOSITORY_2_URL}/authentication-service:${TAG}"

tag-metadata-db:
	docker tag dbrepo-metadata-db:latest "${REPOSITORY_1_URL}/metadata-db:${TAG}"
	docker tag dbrepo-metadata-db:latest "${REPOSITORY_2_URL}/metadata-db:${TAG}"

tag-ui:
	docker tag dbrepo-ui:latest "${REPOSITORY_1_URL}/ui:${TAG}"
	docker tag dbrepo-ui:latest "${REPOSITORY_2_URL}/ui:${TAG}"

tag-data-service:
	docker tag dbrepo-data-service:latest "${REPOSITORY_1_URL}/data-service:${TAG}"
	docker tag dbrepo-data-service:latest "${REPOSITORY_2_URL}/data-service:${TAG}"

tag-metadata-service:
	docker tag dbrepo-metadata-service:latest "${REPOSITORY_1_URL}/metadata-service:${TAG}"
	docker tag dbrepo-metadata-service:latest "${REPOSITORY_2_URL}/metadata-service:${TAG}"

tag-search-db:
	docker tag dbrepo-search-db:latest "${REPOSITORY_1_URL}/search-db:${TAG}"
	docker tag dbrepo-search-db:latest "${REPOSITORY_2_URL}/search-db:${TAG}"

tag-data-db-sidecar:
	docker tag dbrepo-data-db-sidecar:latest "${REPOSITORY_1_URL}/data-db-sidecar:${TAG}"
	docker tag dbrepo-data-db-sidecar:latest "${REPOSITORY_2_URL}/data-db-sidecar:${TAG}"

tag-search-db-init:
	docker tag dbrepo-search-db-init:latest "${REPOSITORY_1_URL}/search-db-init:${TAG}"
	docker tag dbrepo-search-db-init:latest "${REPOSITORY_2_URL}/search-db-init:${TAG}"

tag-search-service:
	docker tag dbrepo-search-service:latest "${REPOSITORY_1_URL}/search-service:${TAG}"
	docker tag dbrepo-search-service:latest "${REPOSITORY_2_URL}/search-service:${TAG}"

tag-storage-service-init:
	docker tag dbrepo-storage-service-init:latest "${REPOSITORY_1_URL}/storage-service-init:${TAG}"
	docker tag dbrepo-storage-service-init:latest "${REPOSITORY_2_URL}/storage-service-init:${TAG}"

release: build-docker tag release-analyse-service release-authentication-service release-metadata-db release-ui release-metadata-service release-data-service release-search-db release-search-db-init release-search-service release-data-db-sidecar release-storage-service-init

release-analyse-service: tag-analyse-service
	docker push "${REPOSITORY_1_URL}/analyse-service:${TAG}"
	docker push "${REPOSITORY_2_URL}/analyse-service:${TAG}"

release-authentication-service: tag-authentication-service
	docker push "${REPOSITORY_1_URL}/authentication-service:${TAG}"
	docker push "${REPOSITORY_2_URL}/authentication-service:${TAG}"

release-metadata-db: tag-metadata-db
	docker push "${REPOSITORY_1_URL}/metadata-db:${TAG}"
	docker push "${REPOSITORY_2_URL}/metadata-db:${TAG}"

release-ui: tag-ui
	docker push "${REPOSITORY_1_URL}/ui:${TAG}"
	docker push "${REPOSITORY_2_URL}/ui:${TAG}"

release-data-service: tag-data-service
	docker push "${REPOSITORY_1_URL}/data-service:${TAG}"
	docker push "${REPOSITORY_2_URL}/data-service:${TAG}"

release-search-db: tag-search-db
	docker push "${REPOSITORY_1_URL}/search-db:${TAG}"
	docker push "${REPOSITORY_2_URL}/search-db:${TAG}"

release-search-db-init: tag-search-db-init
	docker push "${REPOSITORY_1_URL}/search-db-init:${TAG}"
	docker push "${REPOSITORY_2_URL}/search-db-init:${TAG}"

release-data-db-sidecar: tag-data-db-sidecar
	docker push "${REPOSITORY_1_URL}/data-db-sidecar:${TAG}"
	docker push "${REPOSITORY_2_URL}/data-db-sidecar:${TAG}"

release-metadata-service: tag-metadata-service
	docker push "${REPOSITORY_1_URL}/metadata-service:${TAG}"
	docker push "${REPOSITORY_2_URL}/metadata-service:${TAG}"

release-search-service: tag-search-service
	docker push "${REPOSITORY_1_URL}/search-service:${TAG}"
	docker push "${REPOSITORY_2_URL}/search-service:${TAG}"

release-storage-service-init: tag-storage-service-init
	docker push "${REPOSITORY_1_URL}/storage-service-init:${TAG}"
	docker push "${REPOSITORY_2_URL}/storage-service-init:${TAG}"

test-backend: test-metadata-service test-analyse-service test-data-service test-lib-python

test-data-service: build-data-service
	mvn -f ./dbrepo-data-service/pom.xml clean test verify

test-metadata-service: build-metadata-service
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

test-analyse-service: build-analyse-service
	bash ./dbrepo-analyse-service/test.sh

test-lib-python: build-lib-python
	bash ./lib/python/test.sh

scan: scan-analyse-service scan-authentication-service scan-broker-service scan-gateway-service scan-metadata-db scan-metadata-service scan-search-db scan-ui scan-data-service scan-data-db scan-search-dashboard scan-search-service

scan-analyse-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-analyse-service-report.json dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 0 dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-analyse-service:latest

scan-authentication-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-authentication-service-report.json dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 0 dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-authentication-service:latest

scan-broker-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-broker-service-report.json bitnami/rabbitmq:3.10
	trivy image --insecure --exit-code 0 bitnami/rabbitmq:3.10
	trivy image --insecure --exit-code 1 --severity CRITICAL bitnami/rabbitmq:3.10

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

scan-data-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-data-service-report.json dbrepo-data-service:latest
	trivy image --insecure --exit-code 0 dbrepo-data-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-data-service:latest

scan-search-db:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "dbrepo-search-db"
	trivy image --insecure --exit-code 0 "dbrepo-search-db"
	trivy image --insecure --exit-code 1 --severity CRITICAL "dbrepo-search-db"

scan-search-dashboard:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "opensearchproject/opensearch-dashboards:2.10.0"
	trivy image --insecure --exit-code 0 "opensearchproject/opensearch-dashboards:2.10.0"
	trivy image --insecure --exit-code 1 --severity CRITICAL "opensearchproject/opensearch-dashboards:2.10.0"

scan-data-db:
	docker pull "bitnami/mariadb:11.2.2-debian-11-r0"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-data-db-report.json "bitnami/mariadb:11.2.2-debian-11-r0"
	trivy image --insecure --exit-code 0 "bitnami/mariadb:11.2.2-debian-11-r0"
	trivy image --insecure --exit-code 1 --severity CRITICAL "bitnami/mariadb:11.2.2-debian-11-r0"

scan-ui:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-ui-report.json dbrepo-ui:latest
	trivy image --insecure --exit-code 0 dbrepo-ui:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-ui:latest

scan-search-service:
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-service-report.json dbrepo-search-service:latest
	trivy image --insecure --exit-code 0 dbrepo-search-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-search-service:latest

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
	./bin/teardown.sh

build-api:
	bash .docs/.swagger/swagger-generate.sh

build-mkdocs:
	bash .docs/build-website.sh

build-sphinx:
	bash ./lib/python/build-website.sh

docs: build-mkdocs build-sphinx
