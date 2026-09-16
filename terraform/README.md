# Terraform Infrastructure

This directory provisions the AWS infrastructure for the Insurance Policy
Management Portal backend using modular Terraform.

## Modules

| Module                          | Resources                                                                 |
|----------------------------------|----------------------------------------------------------------------------|
| `modules/network`                | VPC, public/private/database subnets, internet gateway, NAT gateway(s), route tables |
| `modules/security-groups`         | Security groups for the ALB, ECS service, and RDS database (least-privilege ingress) |
| `modules/compute`                 | ECR repository, ECS Fargate cluster/service/task definition, Application Load Balancer, IAM roles, CloudWatch log group |
| `modules/database`                | RDS PostgreSQL instance, DB subnet group, master credentials stored in Secrets Manager |
| `modules/storage`                 | S3 bucket (encrypted, versioned, blocked public access) for policy/claim documents |

The root module (`main.tf`) wires these modules together, passing outputs
from one module as inputs to another (e.g. the VPC ID and subnet IDs from
`network` feed into `security-groups`, `compute`, and `database`).

## Variables

All configurable inputs are declared in `variables.tf` at the root, with
sensible defaults. Environment-specific values are provided via `.tfvars`
files in `environments/` (`dev.tfvars`, `prod.tfvars`).

## Outputs

Root-level outputs (`outputs.tf`) expose the key resource identifiers needed
after `apply`, including the VPC/subnet IDs, ALB DNS name, ECR repository
URL, ECS cluster/service names, RDS endpoint, DB credentials secret ARN, and
storage bucket name.

## State Management

Remote state is configured via an S3 backend with DynamoDB state locking
(`versions.tf`). Because backend configuration blocks cannot reference
variables, backend settings are supplied at `terraform init` time using a
backend config file. Copy `backend.hcl.example` and adjust as needed:

```bash
cp backend.hcl.example backend.hcl   # edit bucket/table names/region as needed
terraform init -backend-config=backend.hcl
```

The referenced S3 bucket (versioned + encrypted) and DynamoDB table (with a
`LockID` string primary key) must exist before running `init`; they are not
created by this configuration to avoid a chicken-and-egg bootstrap problem.

## Usage

```bash
terraform init -backend-config=backend.hcl
terraform plan  -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars
```

To target a different environment, use `environments/prod.tfvars` (and a
distinct backend `key`/workspace to keep state isolated).
