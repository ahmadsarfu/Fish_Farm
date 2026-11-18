#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p .local
if [ -f .local/platform.env ]; then source .local/platform.env; fi
# This code can claim existing farm 1 only while it has no owner.
# Read .local/farm-setup-code locally and enter it in the setup form; never commit it.
if [ -z "${FARM_SETUP_TOKEN:-}" ]; then
  if [ ! -s .local/farm-setup-code ]; then openssl rand -hex 32 > .local/farm-setup-code; fi
  export FARM_SETUP_TOKEN
  FARM_SETUP_TOKEN=$(cat .local/farm-setup-code)
fi
printf '%s\n' 'Starting local API. Existing-farm setup code is in .local/farm-setup-code.'
exec bash mvnw spring-boot:run "$@"
