# Deployment Checklist

Pre-deployment validation, post-deployment verification, and rollback
procedures for deploying the Insurance Policy Management Portal backend.
See [`DEPLOYMENT.md`](DEPLOYMENT.md) and
[`aws/dev-deployment-guide.md`](aws/dev-deployment-guide.md) for full
step-by-step instructions.

## Pre-deployment

- [ ] AWS credentials configured for the target account (dev: `522826260049`)
      with permissions to manage VPC, ECS, ALB, RDS, S3, IAM, Secrets
      Manager, and DynamoDB
- [ ] Terraform state S3 bucket + DynamoDB lock table exist
      (`scripts/bootstrap-terraform-backend.sh`)
- [ ] `terraform/backend.hcl` present and correct for the target environment
- [ ] `terraform/environments/<env>.tfvars` reviewed for the target
      environment (region, sizing, `container_environment`, subnet CIDRs)
- [ ] JWT secret created in Secrets Manager (`aws/secrets-setup.sh`) and its
      ARN set as `jwt_secret_arn` in the `.tfvars` file
- [ ] `terraform init -backend-config=backend.hcl` completes successfully
- [ ] `terraform validate` passes
- [ ] `terraform plan -var-file=environments/<env>.tfvars` reviewed and
      approved (no unexpected destroys/replacements)
- [ ] Backend Docker image builds locally (`docker build .`)
- [ ] Unit/integration tests pass (`mvn test`)

## Deployment

- [ ] `terraform apply -var-file=environments/<env>.tfvars`
- [ ] Backend image built, tagged, and pushed to the ECR repository
      (`terraform output -raw ecr_repository_url`)
- [ ] ECS service updated to the new image (re-apply with the pushed tag, or
      `aws ecs update-service --force-new-deployment`)

## Post-deployment verification

- [ ] ECS service reaches `steady state` with all tasks `RUNNING`
- [ ] ALB target group shows all targets `healthy`
- [ ] Health check responds successfully:
      `curl http://<alb_dns_name>/actuator/health/liveness`
- [ ] RDS instance status is `available` and accepting connections from the
      ECS task (check application logs for successful Flyway migrations)
- [ ] CloudWatch log group (`/ecs/<project>-<env>`) is receiving log events
- [ ] S3 documents bucket exists with versioning and encryption enabled
- [ ] Frontend (UI) can reach the backend without CORS errors
      (`CORS_ALLOWED_ORIGINS` matches the UI's public URL)

## Rollback procedure

If the new deployment is unhealthy:

1. **Roll back the application image** (fastest, most common case):
   ```bash
   aws ecs update-service \
     --cluster "$(terraform -chdir=terraform output -raw ecs_cluster_name)" \
     --service "$(terraform -chdir=terraform output -raw ecs_service_name)" \
     --task-definition <previous-task-definition-arn>
   ```
   Find the previous task definition revision with:
   ```bash
   aws ecs list-task-definitions --family-prefix insurance-portal-<env> --sort DESC
   ```
2. **Roll back infrastructure changes**: re-apply the previous Terraform
   configuration (e.g. `git checkout <previous-commit> -- terraform/` or
   revert the `.tfvars` change) and run `terraform apply` again.
3. **Database issues**: RDS automated backups are retained per
   `db_backup_retention_days` (`7` days in dev); restore via
   `aws rds restore-db-instance-to-point-in-time` if a migration caused data
   corruption. `deletion_protection` is disabled and `skip_final_snapshot`
   is enabled in dev, so take a manual snapshot before any destructive
   change if you need a restore point.
4. Confirm rollback success by repeating the **Post-deployment
   verification** checklist above.
