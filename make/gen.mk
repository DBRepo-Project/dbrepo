##@ Generate

.PHONY: gen-openapi-doc
gen-openapi-doc: build-images ## Generate Swagger documentation and fetch.
	docker compose up -d
	bash .docs/.openapi/openapi-generate.sh
	docker compose down
	openapi-merge-cli --config .docs/.openapi/openapi-merge.json

.PHONY: gen-helm-doc
gen-helm-doc: build-helm ## Generate Helm documentation and schema
	./.scripts/check-helm.sh
	helm schema -input ./helm/dbrepo/values.yaml -output ./helm/dbrepo/values.schema.json
	readme-generator --readme ./helm/dbrepo/README.md --values ./helm/dbrepo/values.yaml

.PHONY: gen-dbrepo-doc
gen-docs-doc: ## Generate DBRepo documentation.
	mkdocs build

.PHONY: gen-lib-doc
gen-lib-doc: ## Generate Python Library documentation.
	bash ./lib/python/build-site.sh
