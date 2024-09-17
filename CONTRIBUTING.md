# Contributing

CBOMkit is a toolset for handling Cryptographic Bill of Materials (CBOM).
This page describes how you can join the community in this goal.

## Before you start

If you are new to the community? We recommend you do the following before diving into the code:

* Read the [Code of Conduct](https://github.com/IBM/cbomkit/blob/main/CODE_OF_CONDUCT.md)
* Familiarize yourself with the community (via [GitHub](https://github.com/IBM/cbomkit/discussions) etc.)

## Choose an issue to work on
The Sonar Cryptography Plugin uses the following labels to help non-maintainers find issues best suited to their interest and experience level:

* [good first issue](https://github.com/IBM/cbomkit/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) - these issues are typically the simplest available to work on, ideal for newcomers. They should already be fully scoped, with a clear approach outlined in the descriptions.
* [help wanted](https://github.com/IBM/cbomkit/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22) - these issues are generally more complex than good first issues. They typically cover work that core maintainers don't currently have capacity to implement and may require more investigation/discussion. These are a great option for experienced contributors looking for something a bit more challenging.

## Code Style

Check if all java files are well formated and license headers are in place.
```shell
mvn spotless:check
```
Applies format and license headers to files.
```shell
mvn spotless:apply
```
Spotless Maven Documentation: https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md

Check for coding style
```shell
mvn checkstyle::check
```

## Build

> To build or run CBOMkit, you need access to the `sonar-cryptography-plugin` dependency, 
> hosted on [GitHub](https://github.com/IBM/sonar-cryptography) as a GitHub Package.
> Using GitHub Packages requires you to authenticate with a GitHub account using a personal 
> access token. You will find explanations [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages).

```shell
# builds a JAR file for the Api server
make build-backend 
# creates a docker image for the Api server
make build-backend-image
# creates a docker image for the frontend/viewer
make build-frontend-image
```

## Development

### Setup the development environment

Depending on where you want to change things, you can spin up different development environments.
```shell
# creates a dev environment, including
#  - postgres database
make dev 
# creates a dev environment, including
#  - postgres database
#  - frontend
make dev-backend 
# creates a dev environment, including
#  - postgres database
#  - api server (backend)
make dev-frontend 
```

#### Start the Api server

```shell
# using the cli 
quarkus dev
# using maven
./mvnw quarkus:dev
```

> Download and install the quarkus-cli from [here](https://quarkus.io/guides/cli-tooling).

#### Start the frontend

```shell
# change to the frontend directory 
cd frontend/
# use vue cli to start the frontend in dev mode
vue-cli-service serve --port 8001
```

### PURLs

When the service is deployed, it first attempts to parse the `purls.json`
file from the resource directory as part of the initialization procedure.
This process extracts repository related purls for a particular software
package referenced in the file and stores it as an identifier in a database
table along with other related purls.

The folks at ScanOSS maintain a repository called [scanoss/purl2cpe](https://github.com/scanoss/purl2cpe).
This repository is the basis for the `purls.json` file,
as it already contains a large number of purls and their associated software packages.

When the `purls-generation/main.py` Python script is executed,
it traverses the resource folder containing a clone of the ScanOSS repository
from a given point in time and generates the `purls.json` file.

When a new file should be generated the variable `version` should be increased in
the `purls-generation/main.py` script. Only when the version changed, the CBOM Generator
will reread the purl.json file from the resource directory.