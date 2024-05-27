.PHONY: all
all:

.PHONY: build
build: ## Generate Helm values schema JSON
	#helm package --sign --key 'Martin Weise' . --keyring ~/.gnupg/mweise.gpg --destination ./build
	helm schema -input ./values.yaml
	readme-generator-for-helm --readme README.md --values values.yaml
