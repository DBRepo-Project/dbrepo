##@ Build

.PHONY: build-images
build-images: build-java-lib build-python-lib ## Build Docker images.
	docker compose build

.PHONY: build-jupyter-image
build-jupyter-image:
	docker build -t starter-notebook ./.jupyter

.PHONY: build-java-lib
build-java-lib: ## Build the Java Library.
	APP_VERSION=$(APP_VERSION) mvn -f ./lib/java/dbrepo-core/pom.xml -q clean package install -DskipTests
	rm -rf ./dbrepo-consumer-service/lib/at/ ./dbrepo-data-service/lib/at/ ./dbrepo-metadata-service/lib/at/
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-consumer-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-data-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
	mvn deploy:deploy-file -q -Dfile=./lib/java/dbrepo-core/target/dbrepo-core-$(APP_VERSION).jar -DgroupId=at.ac.tuwien.ifs.dbrepo -DartifactId=dbrepo-core -Dversion=$(APP_VERSION) -Dpackaging=jar -Durl=file:./dbrepo-metadata-service/lib/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true

.PHONY: build-ui
build-ui: ## Build the UI.
	bun --cwd ./dbrepo-ui build

.PHONY: build-python-lib
build-python-lib: ## Build the Python Library.
	rm -rf ./dbrepo-search-service/lib/* ./dbrepo-search-service/Pipfile.lock ./dbrepo-dashboard-service/lib/* ./dbrepo-dashboard-service/Pipfile.lock
	PIPENV_PIPFILE=./lib/python/Pipfile pipenv lock
	python3 -m build --sdist ./lib/python
	python3 -m build --wheel ./lib/python
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
	PIPENV_PIPFILE=./dbrepo-search-service/Pipfile pipenv lock
	cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/lib
	PIPENV_PIPFILE=./dbrepo-dashboard-service/Pipfile pipenv lock

.PHONY: build-helm
build-helm: ## Build the DBRepo and DBRepo MariaDB Galera Helm Charts.
	helm package ./helm/seaweedfs --destination ./build
	helm dependency update ./helm/dbrepo
	helm package ./helm/dbrepo --destination ./build
