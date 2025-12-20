##@ Build

.PHONY: build-images
build-images: build-java-lib build-python-lib ## Build Docker images.
	docker compose build

.PHONY: build-jupyter-image
build-jupyter-image:
	docker build -t starter-notebook ./.jupyter

.PHONY: build-java-lib
build-java-lib: ## Build the Java Library.
	APP_VERSION=$(APP_VERSION) bash ./.scripts/build-java-lib.sh

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-python-lib
build-python-lib: ## Build the Python Library.
	APP_VERSION=$(APP_VERSION) bash ./.scripts/build-python-lib.sh

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
