##@ Release

.PHONY: bump
bump: ## Bump all versions except Python lib. Usage: make bump VERSION=X.Y.Z [CHART=X.Y.Z] [OLD=X.Y.Z]
	@if [ -z "$(VERSION)" ]; then echo "Usage: make bump VERSION=X.Y.Z [CHART=X.Y.Z] [OLD=X.Y.Z]"; exit 1; fi
	bash ./.scripts/bump-version.sh $(if $(OLD),$(OLD)) "$(VERSION)" "$(CHART)"

.PHONY: bump-python
bump-python: ## Bump Python library version. Usage: make bump-python VERSION=X.Y.Z
	@if [ -z "$(VERSION)" ]; then echo "Usage: make bump-python VERSION=X.Y.Z"; exit 1; fi
	bash ./.scripts/bump-python-version.sh "$(VERSION)"

.PHONY: tag-images
tag-images: ## Tag the docker images.
	docker tag dbrepo-consumer-service:latest "${REPOSITORY_URL}/consumer-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-dashboard-service:latest "${REPOSITORY_URL}/dashboard-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-dashboard-service-init:latest "${REPOSITORY_URL}/dashboard-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-data-service:latest "${REPOSITORY_URL}/data-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-metadata-service:latest "${REPOSITORY_URL}/metadata-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-search-service:latest "${REPOSITORY_URL}/search-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-search-service-init:latest "${REPOSITORY_URL}/search-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-storage-service-init:latest "${REPOSITORY_URL}/storage-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag dbrepo-ui:latest "${REPOSITORY_URL}/ui:${APP_VERSION}${BUILD_VERSION}"

.PHONY: tag-jupyter-image
tag-jupyter-image:
	docker tag starter-notebook:latest "${REPOSITORY_URL}/starter-notebook:${APP_VERSION}${BUILD_VERSION}"

.PHONY: release-images
release-images: tag-images ## Release the docker images.
	docker push "${REPOSITORY_URL}/consumer-service:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/dashboard-service:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/dashboard-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/data-service:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/metadata-service:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/search-service:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/search-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/storage-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker push "${REPOSITORY_URL}/ui:${APP_VERSION}${BUILD_VERSION}"

.PHONY: release-juypter-image
release-jupyter-image: tag-jupyter-image
	docker push "${REPOSITORY_URL}/starter-notebook:${APP_VERSION}${BUILD_VERSION}"

.PHONY: release-helm
release-helm: gen-helm-doc ## Release the DBRepo and DBRepo MariaDB Galera Helm charts.
	helm push ./build/dbrepo-${CHART_VERSION}.tgz oci://ghcr.io/dbrepo-project/helm
	helm push ./build/dbrepo-mariadb-galera-${CHART_VERSION}.tgz oci://ghcr.io/dbrepo-project/helm
