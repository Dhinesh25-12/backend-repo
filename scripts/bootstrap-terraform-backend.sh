#!/usr/bin/env bash
# Bootstraps the S3 bucket + DynamoDB table used for Terraform remote state
# and locking (see terraform/README.md, "State Management"). This must be run
# once, manually, before `terraform init`, since the backend config in
# terraform/versions.tf intentionally does not manage these resources itself
# (avoids a chicken-and-egg problem).
#
# Usage:
#   scripts/bootstrap-terraform-backend.sh [-b BUCKET] [-t TABLE] [-r REGION]
#
# Defaults match terraform/backend.hcl.example.
set -euo pipefail

BUCKET="insurance-portal-tfstate"
TABLE="insurance-portal-tf-locks"
REGION="us-east-1"

usage() {
  echo "Usage: $0 [-b BUCKET] [-t TABLE] [-r REGION]" >&2
  exit 1
}

while getopts "b:t:r:h" opt; do
  case "$opt" in
    b) BUCKET="$OPTARG" ;;
    t) TABLE="$OPTARG" ;;
    r) REGION="$OPTARG" ;;
    h|*) usage ;;
  esac
done

command -v aws >/dev/null 2>&1 || { echo "aws CLI is required" >&2; exit 1; }

echo "Creating S3 bucket '$BUCKET' in $REGION (if it doesn't already exist)..."
if aws s3api head-bucket --bucket "$BUCKET" --region "$REGION" 2>/dev/null; then
  echo "  Bucket already exists, skipping creation."
else
  if [ "$REGION" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION"
  else
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" \
      --create-bucket-configuration LocationConstraint="$REGION"
  fi
fi

echo "Enabling versioning on '$BUCKET'..."
aws s3api put-bucket-versioning --bucket "$BUCKET" --region "$REGION" \
  --versioning-configuration Status=Enabled

echo "Enabling default encryption (AES256) on '$BUCKET'..."
aws s3api put-bucket-encryption --bucket "$BUCKET" --region "$REGION" \
  --server-side-encryption-configuration '{
    "Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]
  }'

echo "Blocking public access on '$BUCKET'..."
aws s3api put-public-access-block --bucket "$BUCKET" --region "$REGION" \
  --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

echo "Creating DynamoDB lock table '$TABLE' in $REGION (if it doesn't already exist)..."
if aws dynamodb describe-table --table-name "$TABLE" --region "$REGION" >/dev/null 2>&1; then
  echo "  Table already exists, skipping creation."
else
  aws dynamodb create-table \
    --table-name "$TABLE" \
    --region "$REGION" \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
  aws dynamodb wait table-exists --table-name "$TABLE" --region "$REGION"
fi

cat <<EOF

Done. Now copy terraform/backend.hcl.example to terraform/backend.hcl and set:
  bucket         = "$BUCKET"
  region         = "$REGION"
  dynamodb_table = "$TABLE"

Then run:
  cd terraform
  terraform init -backend-config=backend.hcl
EOF
