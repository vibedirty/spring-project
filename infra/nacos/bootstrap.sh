#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ENV_FILE="$SCRIPT_DIR/.env"
NACOS_BASE_URL=${NACOS_BASE_URL:-http://127.0.0.1:8848}

if [ ! -f "$ENV_FILE" ]; then
    echo "Missing $ENV_FILE. Copy .env.example to .env and set NACOS_ADMIN_PASSWORD." >&2
    exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${NACOS_ADMIN_PASSWORD:?NACOS_ADMIN_PASSWORD must be set in infra/nacos/.env}"

curl --fail --silent --show-error \
    "$NACOS_BASE_URL/nacos/v3/client/ns/instance/list?serviceName=p0-health-check" \
    >/dev/null

# The first call initializes the Nacos 3 administrator. A repeated call can
# return a business error because the user already exists, so it is allowed to
# fail and the subsequent login remains the authoritative check.
curl --silent --show-error --request POST \
    "$NACOS_BASE_URL/nacos/v3/auth/user/admin" \
    --data-urlencode "password=$NACOS_ADMIN_PASSWORD" \
    >/dev/null || true

LOGIN_RESPONSE=$(curl --fail --silent --show-error --request POST \
    "$NACOS_BASE_URL/nacos/v3/auth/user/login" \
    --data-urlencode "username=nacos" \
    --data-urlencode "password=$NACOS_ADMIN_PASSWORD")

ACCESS_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | python3 -c \
    'import json, sys; print(json.load(sys.stdin)["accessToken"])')

create_namespace() {
    namespace_id=$1
    namespace_name=$2
    namespace_desc=$3

    curl --silent --show-error --request POST \
        "$NACOS_BASE_URL/nacos/v3/admin/core/namespace" \
        --header "accessToken:$ACCESS_TOKEN" \
        --data-urlencode "namespaceId=$namespace_id" \
        --data-urlencode "namespaceName=$namespace_name" \
        --data-urlencode "namespaceDesc=$namespace_desc"
    printf '\n'
}

publish_baseline_config() {
    namespace_id=$1
    baseline_content=$(printf 'p0:\n  baseline:\n    verified: true')

    curl --fail --silent --show-error --request POST \
        "$NACOS_BASE_URL/nacos/v3/admin/cs/config" \
        --header "accessToken:$ACCESS_TOKEN" \
        --data-urlencode "namespaceId=$namespace_id" \
        --data-urlencode "groupName=COMMON_GROUP" \
        --data-urlencode "dataId=common.yaml" \
        --data-urlencode "type=yaml" \
        --data-urlencode "desc=P0 baseline marker; applications do not import it until P1" \
        --data-urlencode "content=$baseline_content"
    printf '\n'
}

create_namespace hard-dev hard-dev "Local development namespace"
create_namespace hard-test hard-test "Local test namespace"
publish_baseline_config hard-dev
publish_baseline_config hard-test

echo "Nacos P0 namespaces and baseline configurations are ready."
