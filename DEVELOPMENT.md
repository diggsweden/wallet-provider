# Development Guide

This guide outlines core essentials for developing in this project.

## Table of Contents

- [Setup and Configuration](#setup-and-configuration)
  - [Prerequisites - Linux](#prerequisites---linux)
  - [Prerequisites - macOS](#prerequisites---macos)
  - [Quick Start](#quick-start)
  - [IDE Setup](#ide-setup)
  - [Consuming SNAPSHOTS](#consuming-snapshots-from-maven-central)
- [Development Workflow](#development-workflow)
  - [Available Commands](#available-commands)
  - [Testing and Verification](#testing-format-and-lint)
  - [OpenAPI Compatibility Checks](#openapi-compatibility-checks)
  - [Documentation](#documentation)
  - [Pull Request Process](#pull-request-workflow)

## Setup and Configuration

### Prerequisites - Linux

1. Install [mise](https://mise.jdx.dev/) (manages linting tools):

   ```bash
   curl https://mise.run | sh
   ```

2. Activate mise in your shell:

   ```bash
   # For bash - add to ~/.bashrc
   eval "$(mise activate bash)"

   # For zsh - add to ~/.zshrc
   eval "$(mise activate zsh)"

   # For fish - add to ~/.config/fish/config.fish
   mise activate fish | source
   ```

   Then restart your terminal.

3. Install pipx (needed for reuse license linting):

   ```bash
   # Debian/Ubuntu
   sudo apt install pipx
   ```

4. Install project tools:

   ```bash
   mise install
   ```

### Prerequisites - macOS

1. Install [mise](https://mise.jdx.dev/) (manages linting tools):

   ```bash
   brew install mise
   ```

2. Activate mise in your shell:

   ```bash
   # For zsh - add to ~/.zshrc
   eval "$(mise activate zsh)"

   # For bash - add to ~/.bashrc
   eval "$(mise activate bash)"

   # For fish - add to ~/.config/fish/config.fish
   mise activate fish | source
   ```

   Then restart your terminal.

3. Install newer bash than macOS default:

   ```bash
   brew install bash
   ```

4. Install pipx (needed for reuse license linting):

   ```bash
   brew install pipx
   ```

5. Install project tools:

   ```bash
   mise install
   ```

### Quick Start

```shell
# Install all development tools
mise install

# Show all just tasks
just

# Setup shared linting tools
just setup-devtools

# Run all quality checks
just verify
```

### IDE Setup

#### VSCode

1. Install plugins:

   - [Checkstyle for Java](https://marketplace.visualstudio.com/items?itemName=shengchen.vscode-checkstyle)
   - [markdownlint](https://marketplace.visualstudio.com/items?itemName=DavidAnson.vscode-markdownlint)
   - [PMD for Java](https://marketplace.visualstudio.com/items?itemName=cracrayol.pmd-java)
   - [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
   - [ShellCheck](https://marketplace.visualstudio.com/items?itemName=timonwong.shellcheck)
   - [shell-format](https://marketplace.visualstudio.com/items?itemName=foxundermoon.shell-format) version 7.2.5

        **Note 1:** There is
        [a known issue](https://github.com/foxundermoon/vs-shell-format/issues/396)
        with version 7.2.8 of shell-format
        preventing it from being detected as a formatter for shell scripts.
        Please use version 7.2.5 until the issue is fixed.

        **Note 2:** You need to have the `shfmt` binary installed in order to use the plugin.
        On Ubuntu you can install it with `sudo apt-get install shfmt`.

2. Open workspace settings - settings.json (for example with Ctrl+Shift+P → Preferences: Workspace Settings (JSON)) and add:

    ```json
    "editor.formatOnSave": true,
    "java.checkstyle.configuration": "development/lint/google_checks.xml",
    "java.checkstyle.version": "1x.xx.x",
    "java.format.settings.profile": "GoogleStyle",
    "java.format.settings.url": "development/format/eclipse-java-google-style.xml",
    "javaPMD.rulesets": [
        "development/sast/pmd_default_java.xml"
    ],
    "shellformat.path": "<path to shfmt>",
    "[markdown]": {
        "editor.defaultFormatter": "DavidAnson.vscode-markdownlint"
    },
    "[java]": {
        "editor.defaultFormatter": "redhat.java",
    }
    ```

#### IntelliJ

1. **Code Style**
   - Settings → `Editor → Code Style → Java`
   - Click gear → `Import Scheme → Eclipse XML Profile`
   - Select `development/format/eclipse-java-google-style.xml`

2. **Checkstyle**
   - Install "CheckStyle-IDEA" plugin
   - Settings → `Tools → Checkstyle`
   - Click the built-in Google Style Check

## Consuming SNAPSHOTS from Maven Central

Configure your pom.xml file with:

```xml
<repositories>
  <repository>
    <name>Central Portal Snapshots</name>
    <id>central-portal-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

## Development Workflow

### Available Commands

Run `just` to see all available commands. Key commands:

| Command | Description |
|---------|-------------|
| `just verify` | Run all checks (lint + test) |
| `just lint-all` | Run all linters |
| `just lint-fix` | Auto-fix linting issues |
| `just test` | Run tests (mvn verify) |
| `just build` | Build project |
| `just clean` | Clean build artifacts |

#### Linting Commands

| Command | Tool | Description |
|---------|------|-------------|
| `just lint-commits` | gommitlint | Validate commit messages |
| `just lint-secrets` | gitleaks | Scan for secrets |
| `just lint-yaml` | yamlfmt | Lint YAML files |
| `just lint-markdown` | rumdl | Lint markdown files |
| `just lint-shell` | shellcheck | Lint shell scripts |
| `just lint-shell-fmt` | shfmt | Check shell formatting |
| `just lint-actions` | actionlint | Lint GitHub Actions |
| `just lint-license` | reuse | Check license compliance |
| `just lint-xml` | xmllint | Validate XML files |
| `just lint-container` | hadolint | Lint Containerfile |
| `just lint-openapi` | podman/raplp | Check REST API-profile compliance |
| `just lint-openapi-diff` | openapi-diff | Check OpenAPI backward compatibility |
| `just lint-java` | Maven | Run all Java linters |
| `just lint-java-checkstyle` | checkstyle | Java style checks |
| `just lint-java-pmd` | pmd | Java static analysis |
| `just lint-java-spotbugs` | spotbugs | Java bug detection |
| `just lint-java-fmt` | formatter | Check Java formatting |

#### Fix Commands

| Command | Description |
|---------|-------------|
| `just lint-yaml-fix` | Fix YAML formatting |
| `just lint-markdown-fix` | Fix markdown formatting |
| `just lint-shell-fmt-fix` | Fix shell formatting |
| `just lint-java-fmt-fix` | Fix Java formatting |

### Testing, Format and Lint

Run all verification:

```shell
just verify
```

Or run Maven directly:

```shell
mvn clean verify
```

### OpenAPI Compatibility Checks

API backward compatibility is verified during linting and verification (`just lint-all`, `just verify`, and `just lint-openapi-diff`) using `openapi-diff`.
The tool extracts the baseline specification from local git (`origin/main` or `main`) and compares it against the local specification without requiring internet access.
Standard unit tests (`mvn test`) skip the compatibility diff by default to remain fast and fully offline-capable.

Run the compatibility check directly:

```shell
just lint-openapi-diff
```

#### Handling Breaking Changes and New API Versions

1. **Workflow for Intentional Breaking Changes**:
   When introducing a breaking change to the OpenAPI specification, `openapi-diff` will fail the build to protect API consumers.
   If the breaking change is intentional and approved, suppress the specific failure rule in `development/openapi-diff.yaml`.
   Commit the updated `development/openapi-diff.yaml` file together with the API specification changes in the same Pull Request.
   This allows continuous integration checks to pass and provides reviewers with a clear audit record of the exception.
   Once the Pull Request is merged into `main`, the new specification becomes the baseline for future comparisons.
   You can reset `development/openapi-diff.yaml` back to default in a follow-up commit to re-enable full strict checking.

2. **Introducing New Specification Files (Major Version Bumps)**:
   When introducing a brand new specification file (for example, `wallet-provider-openapi-v2.yaml`) that does not yet exist on `main`, `just lint-openapi-diff` automatically detects that no prior version exists on `main` and skips the diff check.
   Once the Pull Request is merged to `main`, subsequent changes to the new specification file are automatically tracked and verified.

#### Compatibility Rules and Suppression Keys

Rules use dot notation that maps to nested YAML keys in `development/openapi-diff.yaml`.
Setting a rule key to `false` permits the breaking change without failing the build.

Common rule examples:

| Rule Key | YAML Path | Description |
|---|---|---|
| `incompatible.request.required.increased` | `request.required.increased` | New required property added to request body |
| `incompatible.request.params.decreased` | `request.params.decreased` | Existing parameter removed from request |
| `incompatible.response.responses.decreased` | `response.responses.decreased` | HTTP response status code removed |
| `incompatible.openapi.endpoints.decreased` | `openapi.endpoints.decreased` | Entire API endpoint path or method removed |

For the complete and up-to-date list of all available incompatibility rules, refer to [BackwardIncompatibleProp.java in openapi-diff](https://github.com/OpenAPITools/openapi-diff/blob/master/core/src/main/java/org/openapitools/openapidiff/core/model/BackwardIncompatibleProp.java).

### Wallet Instance Attestation prototype

`POST /v0/wallet-instance-attestations` accepts a public elliptic curve JWK encoded as a JSON string:

```json
{
  "jwk": "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"18wHLeIgW9wVN6VD1Txgpqy2LszYkMf6J8njVAibvhM\",\"y\":\"-V4dS4UaLMgP_4fY4j8ir7cl1TXlFdAgcx55o7TkcSA\"}"
}
```

The response is `200 OK` with `{"wallet_instance_attestation":"<compact JWT>"}`.
The JWT uses `oauth-client-attestation+jwt`, ES256, and the certificate chain from the existing `wua.keystore` configuration.
Its `cnf.jwk` contains the supplied public key.
Missing, blank, malformed, non-EC, and private keys produce `400` problem details.
This prototype does not verify key ownership, app integrity, hardware attestation, or revocation status, and does not persist issued attestations.

Wallet metadata comes from the `wia` configuration rather than client input.
All environment variables below are required and have no defaults:

| Environment variable | Purpose |
| --- | --- |
| `WALLET_PROVIDER_WIA_CLIENT_ID` | OAuth client identifier in `sub`, shared across instances |
| `WALLET_PROVIDER_WIA_WALLET_NAME` | Wallet Solution identifier in `wallet_name` |
| `WALLET_PROVIDER_WIA_WALLET_VERSION` | Wallet Solution version |
| `WALLET_PROVIDER_WIA_WALLET_LINK` | Wallet Solution information URL |
| `WALLET_PROVIDER_WIA_CERTIFICATION_INFORMATION` | Wallet Solution certification information |
| `WALLET_PROVIDER_WIA_VALIDITY_MINUTES` | Token lifetime, between 1 and 1439 minutes |
| `WALLET_PROVIDER_WIA_STATUS` | Placeholder status reference in `client_status.status` |
| `WALLET_PROVIDER_WIA_STATUS_MAINTENANCE_DAYS` | Period represented by `client_status.exp`, at least 31 days |

The shared `wua.keystore` configuration accepts `WALLET_PROVIDER_ISSUER` and `WALLET_PROVIDER_WUA_STATUS` overrides.
Their defaults are `Digg` and a placeholder status-list reference with index `412` and URI `https://revocation_url/statuslists/1`, respectively.

#### Wallet metadata settings and production values

The following settings describe the wallet software and are copied into every signed WIA.
The example values are development fixtures, not defaults or official production identifiers.

| `wia` setting | JWT claim | Meaning | Expected production value |
| --- | --- | --- | --- |
| `client-id` | `sub` | OAuth client identifier, shared across wallet instances; it does not identify a user or installation. | The agreed OAuth client identifier used by the wallet and authorization servers. `digg-wallet` can remain if adopted as the official identifier. Wallet-ecosystem currently uses `wallet-dev` to match its Keycloak client. |
| `wallet-name` | `wallet_name` | Wallet Solution identifier in the Wallet Provider Trusted List, rather than just a display name. | The exact identifier from the official trusted-list registration. `Digg Wallet` is currently an example value. |
| `wallet-version` | `wallet_version` | Version of the wallet software being attested, rather than the wallet-provider backend version. | The actual wallet software release, for example `1.2.3`. `0.0.1` is a prototype value. Supporting several releases will require selecting the version from validated information about the requesting app. |
| `wallet-link` | `wallet_link` | Public URL providing information about the wallet solution. | A stable HTTPS page dedicated to Digg Wallet and its relevant product and certification information. `https://www.digg.se` is currently a generic placeholder. |
| `wallet-solution-certification-information` | `wallet_solution_certification_information` | Information about the certification body, certificate number, and other relevant certification details. | Actual certification information from trusted records, using the agreed format when established. `UNCERTIFIED` is a local placeholder, not a standardized certification status or certification evidence. |

The metadata requirements are defined in [EUDI TS3, section 2.3.1](https://github.com/eu-digital-identity-wallet/eudi-doc-standards-and-technical-specifications/blob/main/docs/technical-specifications/ts3-wallet-unit-attestation.md#231-wallet-solution-identification-and-certification-information).
TS3 currently leaves the exact certification-information format undefined.
For client attestation authentication, the request's OAuth `client_id` normally must match the WIA's `sub`, as described in the [OAuth client attestation specification](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth).

These metadata values must not be inferred from the supplied public JWK.
The JWK supplies only the public key for `cnf.jwk`; it does not establish app identity, version, or certification.
This prototype uses static provider configuration and does not validate the running app's version or certification.
Most metadata can remain provider configuration, while a future implementation supporting multiple wallet releases should obtain and verify app-version information before signing it.

The token's `exp` and `client_status.exp` are independent.
The status reference and maintenance period are placeholders; this prototype publishes no live status list and provides no revocation-maintenance guarantee.
The wallet must create its own proof of possession when presenting the WIA to an authorization server.

### Documentation

Generate Javadocs:

```shell
mvn javadoc:javadoc
```

View documentation in your browser:

```shell
<browser> target/reports/apidocs/index.html
```

### Pull Request Workflow

When submitting a PR, CI will automatically run several checks. To avoid surprises, run these checks locally first.

#### Running Code Quality Checks Locally

```shell
# Run all checks
just verify

# Or run linting only
just lint-all

# Auto-fix where possible
just lint-fix
```

#### Quality Check Details

- **Java Linting**: Checkstyle, PMD, SpotBugs
- **API Compatibility**: openapi-diff verifies backward compatibility against main
- **General Linting**: Shell, YAML, Markdown, GitHub Actions, XML
- **Container Linting**: Hadolint for Containerfile
- **Security**: Secret scanning with gitleaks
- **License Compliance**: REUSE tool ensures proper copyright information
- **Commit Structure**: Conform checks commit messages for changelog generation

#### Handling Failed Checks

If any checks fail in the CI pipeline:

1. Review the CI error logs
2. Run checks locally to reproduce the issues:

   ```shell
   just lint-all
   ```

3. Auto-fix where possible:

   ```shell
   just lint-fix
   ```

4. Make necessary manual fixes
5. Update your Pull Request
6. Verify all checks pass in the updated PR
