##@ Build

.PHONY: build-images
build-images: ## Build Docker images.
	docker build --network=host -t dbrepo-core:build --target build ./lib/java/dbrepo-core
	docker build --network=host -t dbrepo-data-service:build --target build dbrepo-data-service
	docker build --network=host -t dbrepo-metadata-service:build --target build dbrepo-metadata-service
	docker compose build --parallel

.PHONY: build-data-service
build-data-service: ## Build the Data Service.
	mvn -f ./dbrepo-data-service/pom.xml clean package -DskipTests

.PHONY: build-metadata-service
build-metadata-service: ## Build the Metadata Service.
	mvn -f ./dbrepo-metadata-service/pom.xml clean package -DskipTests

.PHONY: build-auth-event-listener
build-auth-event-listener: ## Build the Auth Service Event Listener.
	mvn -f ./dbrepo-auth-service/listeners/pom.xml clean package -DskipTests
	cp ./dbrepo-auth-service/listeners/target/create-event-listener.jar ./helm/dbrepo/files/create-event-listener.jar

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-lib
build-lib: ## Build the Python Library.
	rm -rf ./dbrepo-analyse-service/venv/ ./dbrepo-analyse-service/Pipfile.lock ./dbrepo-analyse-service/lib/*
	rm -rf ./dbrepo-search-service/venv/ ./dbrepo-search-service/Pipfile.lock ./dbrepo-search-service/lib/*
	rm -rf ./dbrepo-dashboard-service/venv/ ./dbrepo-dashboard-service/Pipfile.lock ./dbrepo-dashboard-service/lib/*
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-analyse-service/lib
	(cd ./dbrepo-analyse-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
	(cd ./dbrepo-search-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/init/lib
	(cd ./dbrepo-search-service/init && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/lib
	(cd ./dbrepo-dashboard-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/init/lib
	(cd ./dbrepo-dashboard-service/init && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm dependency update ./helm/seaweedfs
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
