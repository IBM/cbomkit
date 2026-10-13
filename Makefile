# extract cbomkit version tag from latest GitHub release
VERSION := $(shell curl -s https://api.github.com/repos/cbomkit/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
# set engine to use for build and compose, default to docker
ENGINE ?= docker

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
dev:
	@[ -f .env.development ] || (echo "ERROR: .env.development not found. Copy .env.development.example to .env.development and fill in your values." && exit 1)
	CBOMKIT_VERSION=${VERSION} $(ENGINE)-compose --env-file .env.development --profile dev up -d

dev-backend:
	@[ -f .env.development ] || (echo "ERROR: .env.development not found. Copy .env.development.example to .env.development and fill in your values." && exit 1)
	CBOMKIT_VERSION=${VERSION} $(ENGINE)-compose --env-file .env.development --profile dev-backend up -d

dev-frontend:
	@[ -f .env.development ] || (echo "ERROR: .env.development not found. Copy .env.development.example to .env.development and fill in your values." && exit 1)
	CBOMKIT_VERSION=${VERSION} $(ENGINE)-compose --env-file .env.development --profile dev-frontend up

# run the prod setup using $(ENGINE) compose
production:
	CBOMKIT_VERSION=${VERSION} $(ENGINE)-compose --env-file .env.production --profile prod up

edge:
	$(ENGINE) pull ghcr.io/cbomkit/cbomkit:edge
	$(ENGINE) pull ghcr.io/cbomkit/cbomkit-frontend:edge
	CBOMKIT_VERSION=edge $(ENGINE)-compose --env-file .env.production --profile prod up

coeus:
	@[ -f .env.development ] || (echo "ERROR: .env.development not found. Copy .env.development.example to .env.development and fill in your values." && exit 1)
	CBOMKIT_VERSION=${VERSION} CBOMKIT_VIEWER=true $(ENGINE)-compose --env-file .env.development --profile viewer up

ext-compliance:
	@[ -f .env.development ] || (echo "ERROR: .env.development not found. Copy .env.development.example to .env.development and fill in your values." && exit 1)
	CBOMKIT_VERSION=${VERSION} $(ENGINE)-compose --env-file .env.development --profile ext-compliance up

deploy:
	@[ -f .env.production ] || (echo "ERROR: .env.production not found. Copy .env.production.example to .env.production and fill in your values." && exit 1)
	@set -a; . ./.env.production; set +a; \
	[ -n "$$CLUSTER_DOMAIN" ] || (echo "ERROR: CLUSTER_DOMAIN is not set in .env.production" && exit 1); \
	[ -n "$$DB_USERNAME" ] || (echo "ERROR: DB_USERNAME is not set in .env.production" && exit 1); \
	[ -n "$$DB_PASSWORD" ] || (echo "ERROR: DB_PASSWORD is not set in .env.production" && exit 1); \
	printf "common:\n  clusterDomain: \"%s\"\npostgresql:\n  auth:\n    username: \"%s\"\n    password: \"%s\"\n" \
		"$$CLUSTER_DOMAIN" "$$DB_USERNAME" "$$DB_PASSWORD" | \
	helm install cbomkit \
		-f - \
		--set backend.tag=${VERSION} \
		--set frontend.tag=${VERSION} \
		./chart

undeploy:
	helm uninstall cbomkit

