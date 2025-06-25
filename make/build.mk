##@ Build

.PHONY: build-images
build-images: build-java-lib ## Build Docker images.
	docker compose build
	docker build -t dbrepo-compute-service:latest ./dbrepo-compute-service

.PHONY: build-jupyter-image
build-jupyter-image:
	docker build -t starter-notebook ./.jupyter

.PHONY: build-jupyter-image
build-jupyter-image:
	docker build -t starter-notebook ./.jupyter

.PHONY: build-java-lib
build-java-lib: ## Build the Java Library.
	APP_VERSION=$(APP_VERSION) mvn -f ./lib/java/dbrepo-core/pom.xml -q clean package install -DskipTests
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-data-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-metadata-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true

.PHONY: build-auth-event-listener
build-auth-event-listener: ## Build the Auth Service Event Listener.
	mvn -f ./dbrepo-auth-service/listeners/pom.xml -q clean package -DskipTests
	cp ./dbrepo-auth-service/listeners/target/create-event-listener.jar ./dbrepo-auth-service/listeners/create-event-listener.jar
	cp ./dbrepo-auth-service/listeners/create-event-listener.jar ./helm/dbrepo/files/create-event-listener.jar

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-python-lib
build-python-lib: ## Build the Python Library.
	rm -rf ./dbrepo-analyse-service/lib/* ./dbrepo-search-service/lib/* ./dbrepo-search-service/Pipfile.lock ./dbrepo-dashboard-service/lib/* ./dbrepo-dashboard-service/Pipfile.lock ./dbrepo-auth-service/init/lib/* ./dbrepo-auth-service/init/Pipfile.lock
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
	PIPENV_PIPFILE=./dbrepo-search-service/Pipfile pipenv lock
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/lib
	PIPENV_PIPFILE=./dbrepo-dashboard-service/Pipfile pipenv lock
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-auth-service/init/lib
	PIPENV_PIPFILE=./dbrepo-auth-service/init/Pipfile pipenv lock

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
