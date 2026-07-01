##@ Release

GHCR_IMAGE_PREFIX ?= ghcr.io/dbrepo-project/dbrepo

.PHONY: tag-images
tag-images: build-images ## Tag the docker images.
	docker tag ${GHCR_IMAGE_PREFIX}/consumer-service:latest "${REPOSITORY_URL}/consumer-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/dashboard-service:latest "${REPOSITORY_URL}/dashboard-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/dashboard-service-init:latest "${REPOSITORY_URL}/dashboard-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/data-service:latest "${REPOSITORY_URL}/data-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/metadata-service:latest "${REPOSITORY_URL}/metadata-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/search-service:latest "${REPOSITORY_URL}/search-service:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/search-service-init:latest "${REPOSITORY_URL}/search-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/storage-service-init:latest "${REPOSITORY_URL}/storage-service-init:${APP_VERSION}${BUILD_VERSION}"
	docker tag ${GHCR_IMAGE_PREFIX}/ui:latest "${REPOSITORY_URL}/ui:${APP_VERSION}${BUILD_VERSION}"

.PHONY: tag-jupyter-image
tag-jupyter-image: build-jupyter-image
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
