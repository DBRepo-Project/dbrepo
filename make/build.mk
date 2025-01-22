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
	rm -f ./dbrepo-analyse-service/Pipfile.lock ./dbrepo-analyse-service/lib/dbrepo-${APP_VERSION}*
	rm -f ./dbrepo-search-service/Pipfile.lock ./dbrepo-search-service/lib/dbrepo-${APP_VERSION}*
	rm -f ./dbrepo-search-service/init/Pipfile.lock ./dbrepo-search-service/init/lib/dbrepo-${APP_VERSION}*
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-analyse-service/lib
	(cd ./dbrepo-analyse-service && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
	(cd ./dbrepo-search-service && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/init/lib
	(cd ./dbrepo-search-service/init && PIPENV_IGNORE_VIRTUALENVS=1 pipenv lock)

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm dependency update ./helm/seaweedfs
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
