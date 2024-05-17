##@ Development

.PHONY: start-dev
start-dev: build-images ## Start the development deployment.
	docker compose up -d


.PHONY: stop-dev
stop-dev: ## Stop the development deployment and remove all data.
	docker compose down
