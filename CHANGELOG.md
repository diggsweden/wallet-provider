# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.14] - 2026-05-05

### Changed

- Bump reusable-ci to v2.8.2

## [0.0.13] - 2026-04-27

### Changed

- Use reusableci with sbom naming


## [0.0.12] - 2026-04-27

### Changed

- Use reuseable ci latest
- Bump gommitlint to 0.9.10, switch rumdl from ubi to aqua
- Bump reusable-ci to v2.7.9
- Merge pull request #71 from diggsweden/renovate/java-non-major
- Bump org.springdoc:springdoc-openapi-starter-webmvc-ui to v3.0.3
- Merge pull request #75 from diggsweden/chore/openssf-update
- Bump openssf-scorecard to 2.7.9
- Merge pull request #76 from diggsweden/fix/use-main-version-of-reusable-integration
- Update java to temurin-25 (#74)

### Fixed

- Use main version of reusable integration script


## [0.0.11] - 2026-04-21

### Changed

- Update copyright holder to official agency name (#72)
- Upgrade to spring boot 4.0.5 and jackson 3 (#73)
- Merge pull request #69 from diggsweden/chore/bump-reusable-integration-workflow
- Bump reusable integration workflow
- Update dependency prettier to v3.8.3 (#70)
- Merge pull request #68 from diggsweden/chore/bump-reusable-integration-workflow-0
- Update dependency com.nimbusds:nimbus-jose-jwt to v10.9 (#63)
- Update diggsweden/reusable-ci action to v2.7.6 (#62)
- Migrate to java 25 (#60)

### Fixed

- Bump reusable integration workflow


## [0.0.10] - 2026-04-08

### Added

- Add attack resistance claims to key attestation
- Add ecosystem integration test workflow (#57)

### Changed

- Merge pull request #59 from diggsweden/feat/attack-resistance-claims
- Update java non-major (#56)
- Use gommitlint instead of conform
- Update reusable-ci to v2.7.3


## [0.0.9] - 2026-03-13

### Changed

- Merge pull request #53 from diggsweden/fix/move-to-versionless

### Fixed

- Move to versionless wallet-unit-attestation


## [0.0.8] - 2026-03-10

### Added

- Add no-op version control linter

### Changed

- Merge pull request #48 from diggsweden/feat/fail-on-dirty-working-tree
- Enable version control linter
- Merge pull request #49 from diggsweden/build/add-commitlint
- Use gommitlint
- Update dependency org.springdoc:springdoc-openapi-starter-webmvc-ui to v2.8.16 (#51)
- Merge pull request #45 from diggsweden/chore/bump-reusable-ci-to-v2.6.1
- Bump workflows to use reusable-ci v2.6.1
- Update java non-major (#44)


## [0.0.7] - 2026-02-27

### Removed

- Remove walletId from WalletUnitAttestationDto (#43)


## [0.0.6] - 2026-02-19

### Added

- Add versionless endpoint for create WUA endpoint
- Add missing newline at end of file

### Changed

- Merge pull request #42 from diggsweden/feat/versionless-wua-endpoint
- Merge pull request #41 from diggsweden/fix/skip-format-validation-in-image-build
- Merge pull request #40 from diggsweden/test/fail-on-formatting-errors
- Apply automated formatting
- Merge pull request #33 from diggsweden/fix/bump-hadolint-to-v2.14.0
- Update java non-major (#38)
- Update dependency prettier to v3.8.1 (#37)

### Fixed

- Skip format validation in container image build
- Verify correct formatting instead of fixing it
- Bump hadolint to v2.14.0

### Removed

- Remove 'V2' from symbol names
- Remove unused WUA endpoint
- Remove unnecessary parameter value
- Remove duplicate declaration of formatter version


## [0.0.5] - 2026-02-10

### Changed

- Merge pull request #24 from diggsweden/renovate/actions-checkout-6.x
- Update actions/checkout action to v6
- Merge pull request #21 from diggsweden/renovate/actions-setup-java-5.x
- Update actions/setup-java action to v5
- Merge pull request #17 from diggsweden/renovate/cgr.dev-chainguard-jre-latest
- Update cgr.dev/chainguard/jre:latest docker digest to 867928b
- Merge pull request #14 from diggsweden/renovate/pin-dependencies
- Pin docker.io/library/eclipse-temurin docker tag to c98f0d2


## [0.0.4] - 2026-02-05

### Added

- Add tests for empty and null nonce

### Changed

- Merge pull request #35 from diggsweden/fix/handle-empty-or-missing-nonce


## [0.0.3] - 2026-01-29

### Added

- Add more tests to cover v2 changes
- Add more tests
- Add v2 endpoint that adheres to latest openid4vic specification

### Changed

- Merge pull request #32 from diggsweden/feat/wua-v2-oid4vci-1.0-second-try
- Update dependency org.assertj:assertj-core to v3.27.7 [security] (#34)

### Fixed

- Move comment to avoid warning
- Handle exceptions to avoid spotbugs errors


## [0.0.2] - 2026-01-18

### Changed

- Merge pull request #31 from diggsweden/fix/spotbugs-errors
- Update dependency prettier to v3.7.4 (#29)
- Update actions/setup-java action to v4.8.0 (#28)
- Update java non-major (#27)
- Update dependency prettier to v3.7.4 (#29)
- Update actions/setup-java action to v4.8.0 (#28)
- Update java non-major (#27)
- Use reuseable ci 2.6.0
- Change devbase-justkit name, improve dev doc
- Update justfile and reuseableci
- Update java non-major (#23)
- Update github actions (#22)
- Update actions/checkout action to v4.3.0 (#19)
- Update java non-major (#18)
- Pin dependencies (#15)
- Pin sha and version
- Use base renovate config
- Adjust schedule
- Use reuseable-ci v2

### Fixed

- Throw without extra error logging
- Throw without extra error logging
- Clean up exception handling
- Clean up exception handling
- Clean up exception handling
- Do not throw checked Exceptions all the way to the controller
- Use WalletRuntimeException instead of RuntimeException
- Copy of ObjectMapper to avoid EI_EXPOSE_REP2 error
- Correct lintwarnings for docs,container

### Removed

- Remove deprecated PMD rule
- Remove unnecessary null check
- Remove duplicated PMD rule


## [0.0.1] - 2025-10-14

### Added

- Add missing newline
- Add ability to skip megalinter output sanitization

### Changed

- Refactor with new certificate (#12)
- Use reusable-ci v1
- Merge pull request #10 from diggsweden/feat/move-hardcoded-values
- Move hardcoded values to properties file
- Merge pull request #9 from jahwag/feat/signed-jwt
- Response of /wallet-unit-attestion should be a signed jwt
- Merge pull request #7 from diggsweden/feat/wua-endpoint-returns-public-key
- Wua endpoint returns public key
- Merge pull request #5 from diggsweden/feat/apply-checkstyle-on-test-sources
- Include all sources in Checkstyle check
- Merge pull request #4 from diggsweden/feat/endpoint-wua
- Return hardcoded sd-jwt in response from /wallet-unit-attestation
- Test
- New endpoint for wua
- Merge pull request #3 from diggsweden/test/healthcheck
- Merge pull request #2 from diggsweden/feat/add-ability-to-skip-megalinter-output-sanitization
- Wrap long lines for readability
- Inital commit

### Fixed

- Verify claims in WalletUnitAttestationServiceTest
- Reorder imports to comply with our rules
- Test method name
- Health check

### Removed

- Remove default value for config wua.keystore.location


[0.0.14]: https://github.com/diggsweden/wallet-provider/compare/v0.0.13..v0.0.14
[0.0.13]: https://github.com/diggsweden/wallet-provider/compare/v0.0.12..v0.0.13
[0.0.12]: https://github.com/diggsweden/wallet-provider/compare/v0.0.11..v0.0.12
[0.0.11]: https://github.com/diggsweden/wallet-provider/compare/v0.0.10..v0.0.11
[0.0.10]: https://github.com/diggsweden/wallet-provider/compare/v0.0.9..v0.0.10
[0.0.9]: https://github.com/diggsweden/wallet-provider/compare/v0.0.8..v0.0.9
[0.0.8]: https://github.com/diggsweden/wallet-provider/compare/v0.0.7..v0.0.8
[0.0.7]: https://github.com/diggsweden/wallet-provider/compare/v0.0.6..v0.0.7
[0.0.6]: https://github.com/diggsweden/wallet-provider/compare/v0.0.5..v0.0.6
[0.0.5]: https://github.com/diggsweden/wallet-provider/compare/v0.0.4..v0.0.5
[0.0.4]: https://github.com/diggsweden/wallet-provider/compare/v0.0.3..v0.0.4
[0.0.3]: https://github.com/diggsweden/wallet-provider/compare/v0.0.2..v0.0.3
[0.0.2]: https://github.com/diggsweden/wallet-provider/compare/v0.0.1..v0.0.2

<!-- generated by git-cliff -->
