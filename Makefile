# Local overrides (credentials, cluster domain, etc.) — not committed to git.
# Copy config.mk.example to config.mk and fill in your values.
-include config.mk

# extract cbomkit version tag from pom.xml
VERSION := $(shell curl -s https://api.github.com/repos/cbomkit/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
# set engine to use for build and compose, default to docker
ENGINE ?= docker
# common env vars passed to every compose invocation
COMPOSE_ENV = CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=${POSTGRESQL_USERNAME} POSTGRESQL_AUTH_PASSWORD=${POSTGRESQL_PASSWORD}
# guard macro — usage: make target guard-VARNAME
guard-%:
	@[ -n "${$*}" ] || (echo "ERROR: $* is not set. Copy config.mk.example to config.mk and fill in your values." && exit 1)
# build the backend
build-backend:
	./mvnw clean package
# build the container image for the backend
build-backend-image: build-backend
	$(ENGINE) build \
		-t cbomkit:${VERSION} \
		-f src/main/docker/Dockerfile.jvm \
		. \
		--load
# build the container image for the frontend
build-frontend-image:
	$(ENGINE) build \
		-t cbomkit-frontend:${VERSION} \
		-f frontend/docker/Dockerfile \
		./frontend \
		--load
# run the dev setup using docker/podman compose
dev: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	env ${COMPOSE_ENV} $(ENGINE)-compose --profile dev up -d
dev-backend: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	env ${COMPOSE_ENV} $(ENGINE)-compose --profile dev-backend up -d
dev-frontend: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	env ${COMPOSE_ENV} $(ENGINE)-compose --profile dev-frontend up
# run the prod setup using $(ENGINE) compose
production: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	env ${COMPOSE_ENV} $(ENGINE)-compose --profile prod up
edge: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	$(ENGINE) pull ghcr.io/cbomkit/cbomkit:edge
	$(ENGINE) pull ghcr.io/cbomkit/cbomkit-frontend:edge
	env CBOMKIT_VERSION=edge CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=${POSTGRESQL_USERNAME} POSTGRESQL_AUTH_PASSWORD=${POSTGRESQL_PASSWORD} $(ENGINE)-compose --profile prod up
coeus:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=true $(ENGINE)-compose --profile viewer up
ext-compliance: guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	env ${COMPOSE_ENV} $(ENGINE)-compose --profile ext-compliance up

deploy: guard-CLUSTER_DOMAIN guard-POSTGRESQL_USERNAME guard-POSTGRESQL_PASSWORD
	helm install cbomkit \
		--set common.clusterDomain=${CLUSTER_DOMAIN} \
		--set cbomkit.tag=${VERSION} \
		--set frontend.tag=${VERSION} \
		--set postgresql.auth.username=${POSTGRESQL_USERNAME} \
		--set postgresql.auth.password=${POSTGRESQL_PASSWORD} \
		./chart

undeploy:
	helm uninstall cbomkit

