# CBOMkit - the essentials for CBOMs

[![License](https://img.shields.io/github/license/cbomkit/cbomkit.svg)](https://opensource.org/licenses/Apache-2.0) <!--- long-description-skip-begin -->
[![Current Release](https://img.shields.io/github/release/cbomkit/cbomkit.svg)](https://github.com/cbomkit/cbomkit/releases)

CBOMkit is a toolset for dealing with Cryptography Bill of Materials (CBOM). CBOMkit includes a
- **CBOM Generation** ([CBOMkit-hyperion](https://github.com/cbomkit/sonar-cryptography), [CBOMkit-theia](https://github.com/cbomkit/cbomkit-theia)): Generate CBOMs from source code by scanning private and public git repositories to find the used cryptography.
- **CBOM Viewer ([CBOMkit-coeus](https://github.com/cbomkit/cbomkit?tab=readme-ov-file#cbomkit-coeus))**: Visualize a generated or uploaded CBOM and access comprehensive statistics.
- **CBOM Compliance Check**: Evaluate CBOMs created or uploaded against specified compliance policies and receive detailed compliance status reports.
- **CBOM Database**: Collect and store CBOMs into the database and expose this data through a RESTful API.

![CBOMkit Demo](.github/img/cbomkit.gif)

> [!WARNING]
> The CBOMkit service does not build any repository prior to scanning. For Java repositories in particular, this means that we cannot rely on any build results (class files, jars) that could improve the scanning result. This potentially reduces completeness and accuracy of the findings since some Java symbols may not be resolved. For better results, use the [sonar-cryptography-plugin](https://github.com/cbomkit/sonar-cryptography) together with SonarQube or [CBOMkit-action](https://github.com/cbomkit/cbomkit-action) embedded in a pipeline definition that builds the code before scanning.

## Quickstart

First, clone the repository and navigate to the project directory:

```shell
git clone https://github.com/cbomkit/cbomkit
cd cbomkit
```

### Deployment Options

#### Option 1: Docker Compose

Starting the CBOMkit using `docker-compose`.
```shell
# run the make command to start the docker compose 
make production
```

To run the latest development build instead of the latest release, use the `edge` images:
```shell
make edge
```
#### Option 2: Podman

If you prefer Podman, ensure podman-compose is installed (`pip3 install podman-compose`), then run:
```shell
# run the make command to start the docker compose using podman
make production ENGINE=podman
```

#### Option 3: Kubernetes (Helm)

Deploy to a cluster by providing your domain and database credentials. This command automatically fetches the latest release tags:
```shell
# clone the repository 
git clone https://github.com/cbomkit/cbomkit
cd cbomkit
# deploy using helm
helm install cbomkit \
  --set common.clusterDomain={CLUSTER_DOMAIN} \
  --set postgresql.auth.username={POSTGRES_USER} \
  --set postgresql.auth.password={POSTGRES_PASSWORD} \
  --set backend.tag=$(curl -s https://api.github.com/repos/cbomkit/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') \
  --set frontend.tag=$(curl -s https://api.github.com/repos/cbomkit/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') \
  ./chart
```

### Using CBOMkit

- Access UI in the browser using http://localhost:8001/
- Enter a git url like [https://github.com/keycloak/keycloak](https://github.com/keycloak/keycloak) or a package url (PURL) like `pkg:maven/io.quarkus/quarkus-core@3.18.1` to generate a CBOM
- View your generated CBOM by selecting your previously scanned CBOM
- Drag and drop CBOM from the [examples](example) into the dropbox to view it

Default service endpoints:
* UI can be accessed at http://localhost:8001/
* API can be accessed at http://localhost:8081/api

## Architecture

The CBOMkit consists of three integral components: a web frontend, an API server, and a database.
In the `ext-compliance` deployment, an additional Open Policy Agent service is used for compliance evaluation (see [External Compliance Evaluation](#external-compliance-evaluation)).

### Frontend and CBOMkit-coeus

The web frontend serves as an intuitive user interface for interacting with the API server. It offers a range of functionalities, including:
 - Browsing the inventory of existing Cryptographic Bills of Materials (CBOMs)
 - Initiating new scans to generate CBOMs 
 - Uploading existing CBOMs for visualization and analysis

#### CBOMkit-coeus

For enhanced flexibility, the frontend component can be deployed as a standalone version, known as the CBOMkit-coeus. 
This option allows for streamlined visualization and compliance analysis independent of the full CBOMkit suite.

```shell
# use this command if you want to run only the CBOMkit-coeus
make coeus
```

### API Server

The API server functions as the central component of the CBOMkit, offering a comprehensive RESTful API 
(see [OpenAPI specification](openapi.yaml)) with the following key features:

#### Features
- Retrieve the most recent generated CBOMs
- Access stored CBOMs from the database
- Perform compliance checks for user-provided CBOMs against specified policies 
- Conduct compliance assessments for stored or generated CBOMs against defined policies

*Sample Query to Retrieve CBOM project identifier*
```shell
curl --request GET \
  --url 'http://localhost:8081/api/v1/cbom/pkg:github%2Fkeycloak%2Fkeycloak@<commit_hash>'
```

In addition to the RESTful API, the server incorporates WebSocket integration, enabling:
 - Initiation of CBOM generation through Git repository scanning 
 - Real-time progress updates during the scanning process, transmitted via WebSocket connection

### Compliance

A critical component of the CBOMkit is its compliance checking mechanism for Cryptography Bills of Materials (CBOMs). 
The CBOM structure represents a hierarchical tree of cryptographic assets detected and used by an application. 
This standardized format facilitates the development and implementation of generalized policies 
to identify and flag violations in cryptographic usage.

The CBOMkit currently features a foundational `quantum-safe` compliance check. 
This initial implementation serves as a proof of concept and demonstrates the system's capability to evaluate
cryptographic components against defined policies.

The compliance framework is designed with extensibility in mind, providing a solid platform for:
 - Implementing additional compliance checks 
 - Enhancing existing verification processes 
 - Integrating custom compliance checks (external)

#### External Compliance Evaluation

CBOMkit supports the use of [Open Policy Agent (OPA)](https://www.openpolicyagent.org) as an external compliance evaluation service. OPA evaluates compliance based on user-defined policies written in its declarative policy language, [Rego](https://www.openpolicyagent.org/docs/policy-language).

In CBOMkit, you can configure OPA as an external compliance service using either:
- the environment variable `CBOMKIT_OPA_API_BASE`, or
- the configuration key `cbomkit.ext-policies.opa-api-base` in [application.properties](src/main/resources/application.properties).

If either option is specified, it must contain the base URL of a running OPA instance. If the variable or property is unset, or if CBOMkit cannot connect to OPA, the system automatically falls back to its built-in internal compliance service.

> [!NOTE]
> The compliance service is selected once, on first use, and then cached for the lifetime of the process. Changing `CBOMKIT_OPA_API_BASE` (or starting OPA after CBOMkit) therefore requires a restart of the API server to take effect.

The internal compliance service implements a fixed “quantum-safe policy.” This built-in policy checks the quantum safety of asymmetric algorithms using whitelists of algorithm OIDs and names.

##### Policy Definition in OPA

CBOMkit provides a sample Rego policy, [quantum_safe.rego](opa/quantum_safe.rego), which replicates the behavior of the internal compliance service.
A Rego policy file begins with a package declaration and defines one or more rules. All CBOMkit policies must start with:

```rego
package policies
```

Each rule includes:
- A header,
- A conditional expression (the logic), and
- A JSON object to be returned when the condition is satisfied.

For compliance evaluation, OPA executes these rules on the set of CBOM components.
By convention, a rule header should follow this format:

```rego
<policy_name>.findings contains finding if ...
```

The `<policy_name>` identifies the policy being evaluated. It must match the policy name configured in the CBOMkit front end through the environment variable `VUE_APP_POLICY_NAME`.
By default, this is `quantum_safe`, the predefined policy included in [quantum_safe.rego](opa/quantum_safe.rego).

When running CBOMkit as a Docker application via `make ext-compliance` (see below), the OPA container is automatically configured with this default policy file.
If you run OPA yourself instead, you can push the sample policy to a running instance with [upload_quantum_safe.sh](opa/upload_quantum_safe.sh):

```shell
cd opa
# defaults to http://localhost:8181, pass a different base URL as first argument
./upload_quantum_safe.sh
```

###### Findings Format
Each policy must produce a JSON list named `findings`, which CBOMkit expects in OPA’s evaluation response.
Every finding object must contain at least the first three mandatory attributes:

```rego
{
  "bom-ref": "string",         # The UUID of the matching component (usually component["bom-ref"])
  "result": "string",          # One of ["quantum-safe", "quantum-vulnerable", "na", "unknown"]
  "rule": "string",            # The name of the rule
  "property": "string",        # Optional: the relevant CBOM property name
  "value": "string" or numeric # Optional: the property’s value
}
```

If any mandatory attribute of a finding is missing, the evaluation will fail, and CBOMkit will revert to its internal compliance service. The result value conveys the rule’s outcome. `NA` indicates that the rule does not apply to a certain component (for example, symmetric algorithms in the predefined "quantum_safe" policy). "property" and "value" are optional and used when rendering compliance details in the CBOMkit interface.

###### Evaluation Results
A compliance policy acts as a knowledge base defining what is compliant or non-compliant. If a component does not match any rule, no finding is produced for that component.

The API response includes an explicit `evaluationStatus` field:

| Status | Meaning |
|--------|---------|
| `evaluated` | Policy ran and every cryptographic asset has a finding |
| `partial` | Policy ran but some assets lack findings |
| `not_evaluated` | No findings were produced (missing policy, empty/malformed OPA result, or response without a `findings` key) |
| `not_applicable` | Empty CBOM, or only non-applicable (e.g. symmetric-only) assets |
| `error` | Evaluation failed (OPA outage, parse failure, unsupported policy) |

`globalComplianceStatus` is `true` **only** when `evaluationStatus` is `evaluated`, at least one applicable asset was checked, every applicable asset is conclusively quantum-safe, and no applicable asset is `quantum-vulnerable` or `unknown`. Missing policies, empty findings, incomplete coverage, and errors never produce a quantum-safe global verdict.

The response also includes `evaluationSummary` counts: `totalCryptographicAssets`, `applicableAssets`, `evaluatedAssets`, `quantumSafeAssets`, `vulnerableAssets`, `unknownAssets`, and `notApplicableAssets`.

> [!NOTE]
> Older versions assumed a CBOM was quantum-safe when OPA returned no findings or could not locate the policy. That fail-open behavior has been removed.

#### Configuration

Different deployment configurations utilize distinct sources for compliance verification.

| Deployment       | How is the compliance check performed?                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `coeus`          | A `quantum-safe` algorithm compliance check is natively implemented within the frontend. This integration allows for immediate, client-side assessment of basic quantum resistance criteria.                                                                                                                                                                                                                                         |
| `production`     | In the standard deployment, a core compliance service is integrated into the backend service. This implementation enables the execution of compliance checks via the RESTful API, providing a scalable and centralized approach to cryptographic policy verification.                                                                                                                                                                |
| `ext-compliance` | In advanced deployment scenarios, compliance evaluation is delegated to a dedicated external service. This service can be invoked by the API server as needed. This configuration maintains the standard user experience for both the frontend and API of the CBOMkit, mirroring the functionality of the `production` configuration while allowing for more sophisticated or specialized compliance checks to be performed externally. |

### Database Migrations

The schema is managed by Hibernate (`quarkus.hibernate-orm.schema-management.strategy=update`), which
only ever adds tables and columns. Changes it cannot apply — altering a column type or a constraint —
are shipped as SQL scripts in [`migrations/`](migrations/) and have to be run once against existing
databases, in file name order. A freshly created database never needs them.

| Migration | Required for databases created before |
|-----------|---------------------------------------|
| [`2026-08-17-scanresult-language-as-text.sql`](migrations/2026-08-17-scanresult-language-as-text.sql) | Go support ([#345](https://github.com/cbomkit/cbomkit/issues/345)) |

### Handling of Credentials

When a new scan of a GitHub repository is started, CBOMkit generates a temporary local clone
of the repository. The frontend enables users to provide GitHub credentials 
(either a username and password or a personal access token). These credentials are not
logged or stored; instead, they are directly forwarded 
to [JGit](https://github.com/eclipse-jgit/jgit) to facilitate the cloning process. 
After the scan completes - regardless of whether it succeeds or fails - the temporary 
local clone is deleted.

### Scanning and CBOM Generation

The CBOMkit leverages advanced scanning technology to identify cryptographic usage within source code and generate 
Cryptography Bills of Materials (CBOMs). This scanning capability is provided by the 
[CBOMkit-hyperion (Sonar Cryptography Plugin)](https://github.com/cbomkit/sonar-cryptography), an open-source tool developed by IBM.

## Supported languages and libraries

The current scanning capabilities of the CBOMkit are defined by the [Sonar Cryptography Plugin's](https://github.com/cbomkit/sonar-cryptography) supported languages
and cryptographic libraries:

| Language | Cryptographic Library                                                                         | Coverage |
|----------|-----------------------------------------------------------------------------------------------|----------|
| Java     | [JCA](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html) | 100%     |
|          | [BouncyCastle](https://github.com/bcgit/bc-java) (*light-weight API*)                         | 100%[^1] |
| Python   | [pyca/cryptography](https://cryptography.io/en/latest/)                                       | 100%     |
| Go       | [crypto](https://pkg.go.dev/crypto) (*standard library*)                                      | 100%[^2]         |
|          | [golang.org/x/crypto](https://pkg.go.dev/golang.org/x/crypto)                                 | Partial[^3]      |
| C#       | [System.Security.Cryptography](https://learn.microsoft.com/en-us/dotnet/api/system.security.cryptography) | In development[^4] |

[^1]: We only cover the BouncyCastle *light-weight API* according to [this specification](https://javadoc.io/static/org.bouncycastle/bctls-jdk14/1.80/specifications.html)
[^2]: All packages under [`crypto`](https://pkg.go.dev/crypto@go1.25.6#section-directories) are covered except `crypto/x509`
[^3]: Covers `golang.org/x/crypto/hkdf`, `golang.org/x/crypto/pbkdf2`, and `golang.org/x/crypto/sha3`
[^4]: C# support uses an [ANTLR v7 grammar](https://github.com/antlr/grammars-v4/tree/master/csharp) to parse source files directly. The current csharp support only covers the language support and does not contain detection rules other than the rules used for verifying the detection engine. **This is not yet meant for active usage!** **Known limitations of the detection engine:** no cross-method variable tracking (only single-method scope), only works for c# v7, string-based matching (no type resolution)

While the CBOMkit's scanning capabilities are currently bound to the Sonar Cryptography Plugin, the modular
design of this plugin allows for potential expansion to support additional languages and cryptographic libraries in
future updates.

## Contributing to CBOMkit

We welcome contributions—simply fork the CBOMkit repository, and then make a [pull
request](https://help.github.com/articles/about-pull-requests/) containing your contribution.

See our [contributions guidelines](CONTRIBUTING.md) for more details. Please also review our
[Code of Conduct](CODE_OF_CONDUCT.md) and ensure you adhere to its principles to help maintain
a respectful and welcoming environment for everyone.

## Support

- **Source Code:** https://github.com/cbomkit/cbomkit
- **Issue Tracker:** https://github.com/cbomkit/cbomkit/issues

If you are having issues, please let us know by posting the issue on our GitHub issue tracker.

## License

[Apache License 2.0](LICENSE.txt)
