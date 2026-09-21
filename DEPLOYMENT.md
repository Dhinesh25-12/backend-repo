# Deploying to AWS with Terraform

This is a step-by-step runbook for deploying this repo's Spring Boot backend
(VPC/ECS Fargate/RDS/S3, provisioned via `terraform/`) to AWS, and for wiring
up the companion frontend in
[`Dhinesh25-12/ui-repo`](https://github.com/Dhinesh25-12/ui-repo).

## 1. Prerequisites

- Install [Terraform](https://developer.hashicorp.com/terraform/downloads)
  matching the version constraint in `terraform/versions.tf` (`>= 1.5.0`,
  AWS provider `~> 5.0`), and the [AWS CLI](https://aws.amazon.com/cli/).
- Configure AWS credentials for the target account (`aws configure` or
  environment variables) with permissions to create VPC, ECS, ALB, RDS, S3,
  IAM, Secrets Manager, and DynamoDB resources.
- This repo already contains everything needed to build and publish the
  backend image:
  - `Dockerfile` — multi-stage Maven/Java 17 build, listens on port `8081`.
  - `buildspec.yml` — AWS CodeBuild spec that builds, tests, and pushes the
    image to ECR.
  - `aws/ecs-task-definition.json` — sample ECS task definition template.

  The `container_image` Terraform variable defaults to a placeholder image
  and **must** be overridden with the real image URI for a working
  deployment (see step 5).

## 2. Bootstrap remote state storage

Terraform's S3 backend (`terraform/versions.tf`) intentionally does **not**
manage its own state bucket/lock table (avoids a chicken-and-egg problem), so
create them once, manually, before the first `terraform init`:

```bash
scripts/bootstrap-terraform-backend.sh \
  -b insurance-portal-tfstate \
  -t insurance-portal-tf-locks \
  -r us-east-1
```

This creates a versioned + encrypted S3 bucket and a DynamoDB table with a
`LockID` string primary key. Then:

```bash
cd terraform
cp backend.hcl.example backend.hcl   # edit bucket/table/region if you used different names
```

## 3. Choose/prepare environment variables

Review `terraform/variables.tf` defaults and the environment-specific files
`terraform/environments/dev.tfvars` / `prod.tfvars`. At minimum, set:

- `container_image` — the real backend image URI (see step 5).
- `container_environment` — e.g. `CORS_ALLOWED_ORIGINS` for the UI's public
  URL; DB connection details are sourced from Secrets Manager automatically
  by the `database`/`compute` modules.
- Any sizing/region overrides needed for the target environment.

## 4. Initialize and deploy backend infrastructure

```bash
cd terraform
terraform init -backend-config=backend.hcl
terraform plan  -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars
```

Use `environments/prod.tfvars` for production (with a distinct backend `key`
to keep state isolated).

Capture the root outputs after apply:

```bash
terraform output
```

Key outputs: `vpc_id`, `public_subnet_ids`, `web_security_group_id`,
`alb_dns_name`, `ecr_repository_url`, `ecs_cluster_name`, `ecs_service_name`,
`db_endpoint`, `db_credentials_secret_arn`, `storage_bucket_name`.

## 5. Push the backend image and roll out the service

```bash
ECR_URL=$(terraform output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$ECR_URL"

cd ..
docker build -t "$ECR_URL:latest" .
docker push "$ECR_URL:latest"
```

Then either re-apply with the pushed image tag:

```bash
cd terraform
terraform apply -var-file=environments/dev.tfvars -var="container_image=$ECR_URL:latest"
```

or force a fresh ECS deployment to pick up the same tag:

```bash
aws ecs update-service \
  --cluster "$(terraform output -raw ecs_cluster_name)" \
  --service "$(terraform output -raw ecs_service_name)" \
  --force-new-deployment
```

(`buildspec.yml` automates the build/tag/push steps above if you wire this
repo into an AWS CodeBuild/CodePipeline project.)

## 6. Deploy the UI (from `Dhinesh25-12/ui-repo`)

Per that repo's Terraform README, pick one:

- **Option A — shared infrastructure (recommended):** In `ui-repo`'s
  Terraform, replace its own `network`/`security` module calls with a
  `terraform_remote_state` data source pointing at this stack's S3 state
  (same bucket/key/region as `terraform/backend.hcl`), feeding `vpc_id`,
  `public_subnet_ids`, and `web_security_group_id` into `ui-repo`'s
  `compute` module. Keep `ui-repo`'s `database`/`storage`/`monitoring`
  modules only if genuinely needed beyond what this stack provides.
- **Option B — fully independent stacks:** Apply `ui-repo`'s Terraform as-is
  with a distinct `project_name`/`environment` and separate state
  bucket/table names to avoid collisions in the same AWS account.

Either way, use distinct state bucket/key/DynamoDB table names between the
two repos so `init`/`apply` in one never clobbers the other's state.

## 7. Wire the two together

- Set the UI's production `apiBaseUrl`
  (`ui-repo/src/environments/environment.ts`) to this stack's `alb_dns_name`
  output.
- Set this stack's `container_environment.CORS_ALLOWED_ORIGINS` (in
  `terraform/environments/dev.tfvars`/`prod.tfvars`) to the UI's public URL
  so the backend accepts cross-origin requests, then re-apply.

## 8. Verify

- Confirm the ECS service reaches steady state and the ALB health check
  (`health_check_path`, default `/actuator/health/liveness`) passes:

  ```bash
  curl "http://$(terraform output -raw alb_dns_name)/actuator/health/liveness"
  ```

- Load the UI and confirm it can reach the backend API without CORS errors.
- Confirm RDS connectivity and that the Secrets Manager secret
  (`db_credentials_secret_arn`) is correctly referenced by the running task.

## 9. Ongoing changes

For future changes, re-run `terraform plan`/`apply` with the appropriate
`-var-file` per environment. Keep `dev` and `prod` in separate state
(different backend `key`) to isolate them.

See `terraform/README.md` for further detail on the module layout and state
management.
