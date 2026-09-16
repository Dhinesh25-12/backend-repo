terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state management (S3 backend + DynamoDB state locking).
  # Backend settings are partial here and must be supplied at `terraform init`
  # time (or via a backend config file), since backend blocks cannot use
  # input variables. Example:
  #
  #   terraform init \
  #     -backend-config="bucket=insurance-portal-tfstate" \
  #     -backend-config="key=insurance-portal/terraform.tfstate" \
  #     -backend-config="region=us-east-1" \
  #     -backend-config="dynamodb_table=insurance-portal-tf-locks" \
  #     -backend-config="encrypt=true"
  #
  # See backend.hcl.example for a ready-to-use config file.
  backend "s3" {}
}
