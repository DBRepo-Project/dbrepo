##@ Development

.PHONY: start-dev
start-dev: build-auth-event-listener build-images ## Start the development deployment.
	docker container stop dbrepo-gateway-service || true
	docker container rm dbrepo-gateway-service || true
	docker compose up -d

.PHONY: stop-dev
stop-dev: ## Stop the development deployment and remove all data.
	docker compose down

.PHONY: package-config
package-config: ## Package the config files
	mkdir -p ./.docker/config
	cp ./dbrepo-auth-service/dbrepo-realm.json ./.docker/config
	cp ./dbrepo-auth-service/import-realms.sh ./.docker/config
	cp ./dbrepo-auth-service/master-realm.json ./.docker/config
	cp ./dbrepo-data-db/1_grant-user.sql ./.docker/config
	cp ./dbrepo-metadata-db/1_setup-schema.sql ./.docker/config
	cp ./dbrepo-metadata-db/2_setup-data.sql ./.docker/config
	cp ./dbrepo-metadata-db/metrics.cnf ./.docker/config
	cp ./dbrepo-broker-service/rabbitmq.conf ./.docker/config
	cp ./dbrepo-broker-service/enabled_plugins ./.docker/config
	cp ./dbrepo-broker-service/definitions.json ./.docker/config
	cp ./dbrepo-broker-service/advanced.config ./.docker/config
	cp ./dbrepo-gateway-service/dbrepo.conf ./.docker/config
	cp -r ./dbrepo-dashboard-ui/dashboards ./.docker/config
	cp -r ./dbrepo-dashboard-ui/provisioning ./.docker/config
	cp ./dbrepo-dashboard-ui/grafana.ini ./.docker/config/grafana.ini
	cp ./dbrepo-dashboard-ui/ldap.toml ./.docker/config/ldap.toml
	cp ./dbrepo-metric-db/prometheus.yml ./.docker/config
	cp ./dbrepo-storage-service/s3_config.json ./.docker/config
	cp ./dbrepo-auth-service/listeners/create-event-listener.jar ./.docker/config
	cd ./.docker && tar czf ./dist.tar.gz ./docker-compose.yml ./.env ./config

.PHONY: install-staging
install-staging: build-helm ## Install on staging server
	helm -n dbrepo uninstall dbrepo --ignore-not-found --wait
	kubectl -n dbrepo delete pvc --all
	helm -n dbrepo install dbrepo ./build/dbrepo-${CHART_VERSION}.tgz --create-namespace -f ./.gitlab/agents/dev/values.yaml
