# Dev Environment Deployment Guide

Step-by-step guide for deploying the Insurance Policy Management Portal
backend to the **dev** environment in AWS account `522826260049`
(`Dhinesh-aws-account`), region `us-east-1`.

This complements the general runbook in [`DEPLOYMENT.md`](../DEPLOYMENT.md)
with the concrete values already wired up for this account/environment. See
`terraform/README.md` for details on the module layout.

## Configuration values used for `dev`

| Setting | Value |
|---|---|
| AWS Account ID | `522826260049` |
| AWS Account Name | `Dhinesh-aws-account` |
| AWS Region | `us-east-1` |
| VPC CIDR | `10.0.0.0/16` |
| Public subnets | `10.0.0.0/24`, `10.0.1.0/24` |
| Private (app) subnets | `10.0.10.0/24`, `10.0.11.0/24` |
| Database subnets | `10.0.20.0/24`, `10.0.21.0/24` |
| RDS engine | PostgreSQL `16.4` |
| RDS instance class | `db.t3.micro` |
| RDS Multi-AZ | `false` |
| RDS backup retention | `7` days |
| ECS cluster / service name | `insurance-portal-dev-cluster` / `insurance-portal-dev-service` |
| ECS task CPU / memory | `512` / `1024` |
| Container / server port | `8081` |
| Desired task count | `1` |
| ECR repository | `insurance-portal-dev` |
| S3 documents bucket | `insurance-portal-dev-storage-<random>` (versioned, AES256-encrypted, public access blocked) |
| CloudWatch log group | `/ecs/insurance-portal-dev` |
| Terraform state bucket | `insurance-portal-tfstate-dev-522826260049` |
| Terraform lock table | `insurance-portal-tf-locks-dev` |
| Terraform state key | `insurance-portal/dev/terraform.tfstate` |
| JWT expiration | `3600000` ms (1 hour) |
| CORS allowed origins | `http://localhost:4200` (update to the deployed UI URL) |
| DB connection pool | max `10`, min idle `2` |

These match the defaults already baked into `terraform/variables.tf`,
`terraform/environments/dev.tfvars`, and `terraform/backend.hcl`.

## Prerequisites

- [ ] Terraform `>= 1.5.0` and AWS provider `~> 5.0` installed (see
      `terraform/versions.tf`)
- [ ] AWS CLI installed and configured for account `522826260049` with
      permissions to create VPC, ECS, ALB, RDS, S3, IAM, Secrets Manager,
      and DynamoDB resources
- [ ] Docker installed (for building/pushing the backend image)
- [ ] `openssl` available (used by `aws/secrets-setup.sh` to generate the
      JWT secret)

## Steps

### 1. Bootstrap Terraform remote state (one-time)

```bash
scripts/bootstrap-terraform-backend.sh \
  -b insurance-portal-tfstate-dev-522826260049 \
  -t insurance-portal-tf-locks-dev \
  -r us-east-1
```

`terraform/backend.hcl` is already configured with these values.

### 2. Initialize Terraform

```bash
cd terraform
terraform init -backend-config=backend.hcl
```

### 3. Initialize secrets

RDS master credentials are generated automatically by Terraform and stored
in Secrets Manager (`terraform/modules/database`) — no manual step needed.

Create the JWT signing secret:

```bash
aws/secrets-setup.sh -p insurance-portal -e dev -r us-east-1
```

Copy the printed `jwt_secret_arn` value into
`terraform/environments/dev.tfvars`.

### 4. Preview and apply infrastructure

```bash
cd terraform
terraform plan  -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars
```

### 5. Build and push the backend image

```bash
ECR_URL=$(terraform output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$ECR_URL"

cd ..
docker build -t "$ECR_URL:latest" .
docker push "$ECR_URL:latest"
```

Then roll out the new image:

```bash
aws ecs update-service \
  --cluster "$(terraform -chdir=terraform output -raw ecs_cluster_name)" \
  --service "$(terraform -chdir=terraform output -raw ecs_service_name)" \
  --force-new-deployment
```

### 6. Validate the deployment

```bash
curl "http://$(terraform -chdir=terraform output -raw alb_dns_name)/actuator/health/liveness"
```

Confirm:
- [ ] ECS service reaches steady state with healthy targets
- [ ] RDS instance is available and reachable from the ECS task
- [ ] CloudWatch log group `/ecs/insurance-portal-dev` is receiving logs
- [ ] S3 documents bucket exists with versioning/encryption enabled

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `terraform init` fails with backend errors | Confirm the S3 bucket/DynamoDB table from step 1 exist in `us-east-1` and match `terraform/backend.hcl` |
| ECS tasks fail to start / flip to `STOPPED` | Check CloudWatch logs (`/ecs/insurance-portal-dev`); often a bad `container_image` tag or missing Secrets Manager permissions |
| ALB target group shows unhealthy targets | Verify `health_check_path` (`/actuator/health/liveness`) responds `200` on `container_port` (`8081`); check ECS security group allows ALB ingress |
| ECS task can't reach RDS | Confirm the task is in the private subnets and the RDS security group allows ingress from the ECS security group |
| `terraform apply` fails creating S3 bucket | S3 bucket names must be globally unique; the `storage` module already appends a random suffix, but a naming collision on the *state* bucket means it needs a different name |
| App fails to start due to `JWT_SECRET` | Run `aws/secrets-setup.sh`, then set `jwt_secret_arn` in `dev.tfvars` and re-apply |
