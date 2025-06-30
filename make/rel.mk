##@ Release

.PHONY: tag-images
tag-images: build-images ## Tag the docker images.
	docker tag dbrepo-auth-service-init:latest "${REPOSITORY_URL}/auth-service-init:${APP_VERSION}"
	docker tag dbrepo-compute-service:latest "${REPOSITORY_URL}/compute-service:${APP_VERSION}"
	docker tag dbrepo-dashboard-service:latest "${REPOSITORY_URL}/dashboard-service:${APP_VERSION}"
	docker tag dbrepo-dashboard-service-init:latest "${REPOSITORY_URL}/dashboard-service-init:${APP_VERSION}"
	docker tag dbrepo-data-service:latest "${REPOSITORY_URL}/data-service:${APP_VERSION}"
	docker tag dbrepo-metadata-service:latest "${REPOSITORY_URL}/metadata-service:${APP_VERSION}"
	docker tag dbrepo-search-service:latest "${REPOSITORY_URL}/search-service:${APP_VERSION}"
	docker tag dbrepo-search-service-init:latest "${REPOSITORY_URL}/search-service-init:${APP_VERSION}"
	docker tag dbrepo-storage-service-init:latest "${REPOSITORY_URL}/storage-service-init:${APP_VERSION}"
	docker tag dbrepo-ui:latest "${REPOSITORY_URL}/ui:${APP_VERSION}"

.PHONY: tag-jupyter-image
tag-jupyter-image: build-jupyter-image
	docker tag starter-notebook:latest "${REPOSITORY_URL}/starter-notebook:${APP_VERSION}"

.PHONY: release-images
release-images: tag-images ## Release the docker images.
	docker push "${REPOSITORY_URL}/auth-service-init:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/compute-service:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/dashboard-service:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/dashboard-service-init:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/data-service:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/metadata-service:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/search-service:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/search-service-init:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/storage-service-init:${APP_VERSION}"
	docker push "${REPOSITORY_URL}/ui:${APP_VERSION}"

.PHONY: release-juypter-image
release-jupyter-image: tag-jupyter-image
	docker push "${REPOSITORY_URL}/starter-notebook:${APP_VERSION}"

.PHONY: release-helm
release-helm: gen-helm-doc ## Release the DBRepo and DBRepo MariaDB Galera Helm charts.
	helm push ./build/dbrepo-${CHART_VERSION}.tgz oci://registry.datalab.tuwien.ac.at/dbrepo/dbrepo/helm
	helm push ./build/dbrepo-mariadb-galera-${CHART_VERSION}.tgz oci://registry.datalab.tuwien.ac.at/dbrepo/dbrepo/helm