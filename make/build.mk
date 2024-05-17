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

.PHONY: build-helm
build-helm: ## Build the Helm Chart.
	helm package ./helm/dbrepo --destination ./build
