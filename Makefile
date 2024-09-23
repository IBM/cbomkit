# extract cbomkit version tag from pom.xml
VERSION := $(shell curl -s https://api.github.com/repos/IBM/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
# build the backend
build-backend: dev
	./mvnw clean package
# build the docker image for the backend
build-backend-image: build-backend
	docker build \
		-t cbomkit:${VERSION} \
		-f src/main/docker/Dockerfile.jvm \
		. \
		--load
# build the docker image for the frontend
build-frontend-image:
	docker build \
		-t cbomkit-frontend:${VERSION} \
		-f frontend/docker/Dockerfile \
		./frontend \
		--load
# run the dev setup using docker compose
dev:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit docker-compose --profile dev up -d
dev-backend:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit docker-compose --profile dev-backend up
dev-frontend:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit docker-compose --profile dev-frontend up
# run the prod setup using docker compose
production:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit docker-compose --profile prod up
coeus:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=true docker-compose --profile viewer up
ext-compliance:
	env CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=false POSTGRESQL_AUTH_USERNAME=cbomkit POSTGRESQL_AUTH_PASSWORD=cbomkit docker-compose --profile ext-compliance up