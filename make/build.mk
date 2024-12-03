##@ Build

.PHONY: build-images
build-images: ## Build Docker images.
	docker build --network=host -t dbrepo-metadata-service:build --target build dbrepo-metadata-service
	docker build --network=host -t dbrepo-data-service:build --target build dbrepo-data-service
	docker compose build --parallel

.PHONY: build-data-service
build-data-service: ## Build the Data Service.
	mvn -f ./dbrepo-data-service/pom.xml clean package -DskipTests

.PHONY: build-metadata-service
build-metadata-service: ## Build the Metadata Service.
	mvn -f ./dbrepo-metadata-service/pom.xml clean package -DskipTests

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-lib
build-lib: ## Build the Python Library.
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp ./lib/python/dist/dbrepo-${APP_VERSION}.tar.gz ./dbrepo-analyse-service/lib/dbrepo-${APP_VERSION}.tar.gz
	(cd ./dbrepo-analyse-service && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)
	cp ./lib/python/dist/dbrepo-${APP_VERSION}.tar.gz ./dbrepo-search-service/lib/dbrepo-${APP_VERSION}.tar.gz
	(cd ./dbrepo-search-service && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)
	cp ./lib/python/dist/dbrepo-${APP_VERSION}.tar.gz ./dbrepo-search-service/init/lib/dbrepo-${APP_VERSION}.tar.gz
	(cd ./dbrepo-search-service/init && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	./.scripts/check-helm.sh
	helm dependency update ./helm/seaweedfs
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
	helm schema -input ./helm/dbrepo/values.yaml -output ./helm/dbrepo/values.schema.json
