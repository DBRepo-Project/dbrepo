##@ Deployment

.PHONY: start
start: ## Run stable deployment.
	docker compose -f docker-compose.prod.yml up -d

.PHONY: stop
stop: ## Run stable deployment.
	docker compose -f docker-compose.prod.yml down
