# extract cbomkit version tag from pom.xml
VERSION := $(shell curl -s https://api.github.com/repos/IBM/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
# set engine to use for build and compose, default to docker
ENGINE ?= docker
# build the backend
build-backend: dev
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
dev:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit $(ENGINE)-compose --profile dev up -d
dev-backend:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit $(ENGINE)-compose --profile dev-backend up
dev-frontend:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit $(ENGINE)-compose --profile dev-frontend up
# run the prod setup using $(ENGINE) compose
production:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit $(ENGINE)-compose --profile prod up
coeus:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=true $(ENGINE)-compose --profile viewer up
ext-compliance:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit $(ENGINE)-compose --profile ext-compliance up