#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
#
# SPDX-License-Identifier: EUPL-1.2

mkdir -p target/raplp
chmod 777 target/raplp

cp src/main/resources/static/wallet-provider-openapi-v0.yaml target/raplp/

podman run --rm \
  -v "${PWD}/target/raplp:/data" \
  ghcr.io/diggsweden/rest-api-profil-lint-processor:v1.1.1 \
  -f /data/wallet-provider-openapi-v0.yaml \
  -l /data/raplp-details.log |
  tee target/raplp/raplp-summary.txt
