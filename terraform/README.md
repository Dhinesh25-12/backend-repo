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

## Deploying alongside the companion UI (`ui-repo`)

The companion frontend (`Dhinesh25-12/ui-repo`) ships its own Terraform stack
that, by default, provisions a *second* VPC, security groups, RDS instance,
S3 bucket, and monitoring stack under the same `project_name`. Applying both
stacks independently works, but it duplicates networking/database resources
and cost. To deploy both the UI and backend into a single AWS account (for
example account `522826260049`) without duplicating infrastructure, reuse
this stack's network/security-group resources for the UI's EC2 compute tier
instead of letting `ui-repo` create its own:

1. Apply this repo's Terraform first (see **Usage** above), then note the
   root outputs `vpc_id`, `public_subnet_ids`, and `web_security_group_id`
   (a security group pre-opened for inbound HTTP/HTTPS, intended for a
   frontend/UI compute tier sharing this VPC).
2. In `ui-repo`'s Terraform, replace its `modules/network` and
   `modules/security` module calls with a `terraform_remote_state` data
   source pointing at this stack's state, e.g.:

   ```hcl
   data "terraform_remote_state" "backend" {
     backend = "s3"
     config = {
       bucket = "insurance-portal-tfstate"
       key    = "insurance-portal/terraform.tfstate"
       region = "us-east-1"
     }
   }

   module "compute" {
     source = "../../modules/compute"
     # ...
     subnet_ids         = data.terraform_remote_state.backend.outputs.public_subnet_ids
     security_group_ids = [data.terraform_remote_state.backend.outputs.web_security_group_id]
   }
   ```

3. Drop (or skip applying) `ui-repo`'s own `network`/`security` modules, and
   keep its `database`/`storage`/`monitoring` modules only if you actually
   need infrastructure distinct from what this stack already provisions.
4. Use distinct state bucket/key/DynamoDB table names per repo (this stack
   defaults to `insurance-portal-tfstate` / `insurance-portal-tf-locks`; keep
   `ui-repo`'s bootstrap-created bucket/table separately named, e.g. suffix
   both with the target account ID to guarantee global S3 bucket uniqueness)
   so `terraform init`/`apply` in one repo never clobbers the other's state.
5. Point the UI's production `apiBaseUrl` (in `src/environments/environment.ts`)
   at this stack's `alb_dns_name` output, and set this stack's
   `container_environment.CORS_ALLOWED_ORIGINS` (see
   `environments/dev.tfvars`) to the UI's public URL so the backend accepts
   cross-origin requests from it.

If instead you prefer two fully independent stacks (simpler, but with
duplicated VPC/RDS/S3/monitoring resources), give each repo's Terraform a
distinct `project_name` and/or `environment` value to avoid resource-name
collisions within the same AWS account, and skip steps 1–3 above.
