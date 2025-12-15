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
| `just lint-commits` | conform | Validate commit messages |
| `just lint-secrets` | gitleaks | Scan for secrets |
| `just lint-yaml` | yamlfmt | Lint YAML files |
| `just lint-markdown` | rumdl | Lint markdown files |
| `just lint-shell` | shellcheck | Lint shell scripts |
| `just lint-shell-fmt` | shfmt | Check shell formatting |
| `just lint-actions` | actionlint | Lint GitHub Actions |
| `just lint-license` | reuse | Check license compliance |
| `just lint-xml` | xmllint | Validate XML files |
| `just lint-container` | hadolint | Lint Containerfile |
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
