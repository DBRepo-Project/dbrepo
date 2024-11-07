##@ Development

.PHONY: start-dev
start-dev: build-images ## Start the development deployment.
	docker container stop dbrepo-gateway-service || true
	docker container rm dbrepo-gateway-service || true
	docker compose up -d


.PHONY: stop-dev
stop-dev: ## Stop the development deployment and remove all data.
	docker compose down

.PHONY: package-config
package-config: ## Package the config files
	mkdir -p ./.docker/config/{dashboards,provisioning}
	cp ./dbrepo-auth-service/dbrepo-realm.json ./.docker/config
	cp ./dbrepo-auth-service/import-realms.sh ./.docker/config
	cp ./dbrepo-auth-service/master-realm.json ./.docker/config
	cp ./dbrepo-metadata-db/1_setup-schema.sql ./.docker/config
	cp ./dbrepo-metadata-db/2_setup-data.sql ./.docker/config
	cp ./dbrepo-broker-service/rabbitmq.conf ./.docker/config
	cp ./dbrepo-broker-service/enabled_plugins ./.docker/config
	cp ./dbrepo-broker-service/definitions.json ./.docker/config
	cp ./dbrepo-broker-service/advanced.config ./.docker/config
	cp ./dbrepo-dashboard-service/grafana.ini ./.docker/config
	cp ./dbrepo-dashboard-service/ldap.toml ./.docker/config
	cp -r ./dbrepo-dashboard-service/dashboards ./.docker/config
	cp -r ./dbrepo-dashboard-service/provisioning ./.docker/config
	cp ./dbrepo-gateway-service/dbrepo.conf ./.docker/config
	cp ./dbrepo-metric-db/prometheus.yml ./.docker/config
	cp ./dbrepo-storage-service/s3_config.json ./.docker/config
	cd ./.docker && tar czf ./dist.tar.gz ./docker-compose.yml ./.env ./config
