##@ Generate

.PHONY: gen-openapi-doc
gen-openapi-doc: build-images ## Generate Swagger documentation and fetch.
	docker compose up -d
	bash ./docs/.openapi/openapi-generate.sh
	docker compose down
	openapi-merge-cli --config ./docs/.openapi/openapi-merge.json
	cp ./docs/api/rest.tpl ./docs/api/rest.md
	openapi-to-md ./docs/.openapi/api.yaml >> ./docs/api/rest.md

.PHONY: gen-helm-doc
gen-helm-doc: build-helm ## Generate Helm documentation and schema
	helm schema --values ./helm/dbrepo/values.yaml --output ./helm/dbrepo/values.schema.json
	readme-generator --readme ./helm/dbrepo/README.md --values ./helm/dbrepo/values.yaml

.PHONY: gen-dbrepo-doc
gen-docs-doc: ## Generate DBRepo documentation and deploy to GitHub Pages.
	mike deploy --push $DOC_VERSION latest
	mike set-default latest

.PHONY: gen-python-doc
gen-python-doc: ## Generate Python Library documentation.
	bash ./lib/python/build-site.sh

gen-secrets:
	IS_DEV=1 bash ./.scripts/gen-secrets.sh
