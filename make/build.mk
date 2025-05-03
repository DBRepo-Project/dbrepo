##@ Build

.PHONY: build-images
build-images: build-java-lib ## Build Docker images.
	docker compose build

.PHONY: build-java-lib
build-java-lib: ## Build the Java Library.
	APP_VERSION=$(APP_VERSION) mvn -f ./lib/java/dbrepo-core/pom.xml -q clean package install -DskipTests
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-data-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-metadata-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true

.PHONY: build-auth-event-listener
build-auth-event-listener: ## Build the Auth Service Event Listener.
	mvn -f ./dbrepo-auth-service/listeners/pom.xml -q clean package -DskipTests
	cp ./dbrepo-auth-service/listeners/target/create-event-listener.jar ./helm/dbrepo/files/create-event-listener.jar

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-python-lib
build-python-lib: ## Build the Python Library.
	rm -rf ./dbrepo-analyse-service/venv/ ./dbrepo-analyse-service/Pipfile.lock ./dbrepo-analyse-service/lib/*
	rm -rf ./dbrepo-search-service/venv/ ./dbrepo-search-service/Pipfile.lock ./dbrepo-search-service/lib/*
	rm -rf ./dbrepo-dashboard-service/venv/ ./dbrepo-dashboard-service/Pipfile.lock ./dbrepo-dashboard-service/lib/*
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-analyse-service/lib
	(cd ./dbrepo-analyse-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
	(cd ./dbrepo-search-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/lib
	(cd ./dbrepo-dashboard-service && python3 -m venv venv && PIPENV_IGNORE_VIRTUALENVS=1 pipenv install --dev)

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm dependency update ./helm/seaweedfs
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
