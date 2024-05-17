##@ Test

.PHONY: test-data-service
test-data-service: ## Test the Data Service.
	mvn -f ./dbrepo-data-service/pom.xml clean test verify

.PHONY: test-metadata-service
test-metadata-service: ## Test the Metadata Service.
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

.PHONY: test-analyse-service
test-analyse-service: ## Test the Analyse Service.
	bash ./dbrepo-analyse-service/test.sh

.PHONY: test-lib
test-lib: ## Test the Python Library.
	bash ./lib/python/test.sh

.PHONY: scan-images
scan-images: ## Scan the docker images for vulnerabilities.
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-analyse-service-report.json dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-analyse-service:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-authentication-service-report.json dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-authentication-service:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-broker-service-report.json bitnami/rabbitmq:3.10
	trivy image --insecure --exit-code 1 --severity CRITICAL bitnami/rabbitmq:3.10
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-gateway-service-report.json "nginx:1.25.0-alpine-slim"
	trivy image --insecure --exit-code 1 --severity CRITICAL "nginx:1.25.0-alpine-slim"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-db-report.json dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-db:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-metadata-service-report.json dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-metadata-service:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-data-service-report.json dbrepo-data-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-data-service:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "dbrepo-search-db"
	trivy image --insecure --exit-code 1 --severity CRITICAL "dbrepo-search-db"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-db-report.json "opensearchproject/opensearch-dashboards:2.10.0"
	trivy image --insecure --exit-code 1 --severity CRITICAL "opensearchproject/opensearch-dashboards:2.10.0"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-data-db-report.json "bitnami/mariadb:11.2.2-debian-11-r0"
	trivy image --insecure --exit-code 1 --severity CRITICAL "bitnami/mariadb:11.2.2-debian-11-r0"
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-ui-report.json dbrepo-ui:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-ui:latest
	trivy image --insecure --exit-code 0 --format template --template "@.trivy/gitlab.tpl" -o ./.trivy/trivy-search-service-report.json dbrepo-search-service:latest
	trivy image --insecure --exit-code 1 --severity CRITICAL dbrepo-search-service:latest