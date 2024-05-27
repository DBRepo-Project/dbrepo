##@ Generate

.PHONY: gen-swagger-doc
gen-swagger-doc: ## Generate Swagger documentation.
	bash .docs/.swagger/swagger-site.sh

.PHONY: gen-swagger-doc-fe
gen-swagger-doc-fe: build-images ## Generate Swagger documentation and fetch.
	docker compose up -d
	bash .docs/.swagger/swagger-generate.sh
	bash .docs/.swagger/swagger-site.sh
	docker compose down

.PHONY: gen-dbrepo-doc
gen-docs-doc: ## Generate DBRepo documentation.
	mkdocs build

.PHONY: gen-lib-doc
gen-lib-doc: ## Generate Python Library documentation.
	bash ./lib/python/build-site.sh
