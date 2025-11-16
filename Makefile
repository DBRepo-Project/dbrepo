.PHONY: all

DOC_VERSION ?= 1.12
APP_VERSION ?= 1.12.1
CHART_VERSION ?= 1.12.1
REPOSITORY_URL ?= registry.datalab.tuwien.ac.at/dbrepo

.PHONY: all
all: help

.PHONY: help
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

.PHONY: version
version: ## Get current version.
	@echo $(APP_VERSION)

include make/build.mk
include make/dev.mk
include make/gen.mk
include make/rel.mk
include make/test.mk
