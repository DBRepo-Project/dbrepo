##@ Test

.PHONY: test-data-service
test-data-service: ## Test the Data Service.
	mvn -f ./dbrepo-data-service/pom.xml clean test verify

.PHONY: test-metadata-service
test-metadata-service: ## Test the Metadata Service.
	mvn -f ./dbrepo-metadata-service/pom.xml clean test verify

.PHONY: test-analyse-service
test-analyse-service: ## Test the Analyse Service.
	bash ./dbrepo-analyse-service/test.sh

.PHONY: test-search-service
test-search-service: ## Test the Search Service
	bash ./dbrepo-search-service/test.sh

.PHONY: test-lib
test-lib: ## Test the Python Library.
	bash ./lib/python/test.sh

.PHONY: test-ui
test-ui: ## Test the UI.
	bash ./dbrepo-ui/test/test_heap.sh