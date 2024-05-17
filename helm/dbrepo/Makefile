.PHONY: all
all:

.PHONY: build
build: ## Generate Helm values schema JSON
	helm schema -input ./values.yaml
	readme-generator-for-helm --readme README.md --values values.yaml