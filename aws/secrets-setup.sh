#!/usr/bin/env bash
# Initializes AWS Secrets Manager with the JWT signing secret used by the
# Insurance Policy Management Portal backend.
#
# NOTE: RDS master credentials are already generated and stored in Secrets
# Manager automatically by `terraform/modules/database` (see
# `aws_secretsmanager_secret.db_credentials` in
# `terraform/modules/database/main.tf`), so there is nothing to do for the
# database here. This script only handles JWT_SECRET, which the Terraform
# `compute` module can optionally read from Secrets Manager via the
# `jwt_secret_arn` variable (see `terraform/environments/dev.tfvars`).
#
# Usage:
#   aws/secrets-setup.sh [-p PROJECT_NAME] [-e ENVIRONMENT] [-r REGION] [-s SECRET]
#
# Defaults match terraform/environments/dev.tfvars (account 522826260049).
set -euo pipefail

PROJECT_NAME="insurance-portal"
ENVIRONMENT="dev"
REGION="us-east-1"
JWT_SECRET_VALUE=""

usage() {
  echo "Usage: $0 [-p PROJECT_NAME] [-e ENVIRONMENT] [-r REGION] [-s SECRET]" >&2
  echo "  -s SECRET  Use this exact JWT secret value instead of generating one" >&2
  exit 1
}

while getopts "p:e:r:s:h" opt; do
  case "$opt" in
    p) PROJECT_NAME="$OPTARG" ;;
    e) ENVIRONMENT="$OPTARG" ;;
    r) REGION="$OPTARG" ;;
    s) JWT_SECRET_VALUE="$OPTARG" ;;
    h|*) usage ;;
  esac
done

command -v aws >/dev/null 2>&1 || { echo "aws CLI is required" >&2; exit 1; }

SECRET_NAME="${PROJECT_NAME}-${ENVIRONMENT}/jwt-secret"

if [ -z "$JWT_SECRET_VALUE" ]; then
  if command -v openssl >/dev/null 2>&1; then
    JWT_SECRET_VALUE="$(openssl rand -base64 48)"
  else
    JWT_SECRET_VALUE="$(head -c 48 /dev/urandom | base64)"
  fi
fi

echo "Creating/updating Secrets Manager secret '$SECRET_NAME' in $REGION..."
if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$REGION" >/dev/null 2>&1; then
  aws secretsmanager put-secret-value \
    --secret-id "$SECRET_NAME" \
    --region "$REGION" \
    --secret-string "$JWT_SECRET_VALUE" >/dev/null
  echo "  Existing secret updated with a new value."
else
  aws secretsmanager create-secret \
    --name "$SECRET_NAME" \
    --region "$REGION" \
    --description "JWT signing secret for ${PROJECT_NAME} (${ENVIRONMENT})" \
    --secret-string "$JWT_SECRET_VALUE" >/dev/null
  echo "  New secret created."
fi

SECRET_ARN="$(aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$REGION" --query 'ARN' --output text)"

cat <<EOF

Done. JWT secret stored at: $SECRET_NAME
ARN: $SECRET_ARN

Set this in terraform/environments/dev.tfvars (or pass via -var) before the
next apply so the ECS task execution role is granted access and the
container receives JWT_SECRET securely:

  jwt_secret_arn = "$SECRET_ARN"

Then re-run:
  cd terraform
  terraform apply -var-file=environments/dev.tfvars
EOF
