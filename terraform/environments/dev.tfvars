project_name = "insurance-portal"
environment  = "dev"
aws_region   = "us-east-1"

vpc_cidr              = "10.0.0.0/16"
availability_zones    = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs   = ["10.0.0.0/24", "10.0.1.0/24"]
private_subnet_cidrs  = ["10.0.10.0/24", "10.0.11.0/24"]
database_subnet_cidrs = ["10.0.20.0/24", "10.0.21.0/24"]
single_nat_gateway    = true

# AWS account: 522826260049 (Dhinesh-aws-account)
container_image   = "522826260049.dkr.ecr.us-east-1.amazonaws.com/insurance-portal-dev:latest"
container_port    = 8081
task_cpu          = 512
task_memory       = 1024
desired_count     = 1
health_check_path = "/actuator/health/liveness"

container_environment = {
  SERVER_PORT          = "8081"
  CORS_ALLOWED_ORIGINS = "http://localhost:4200"
}

# ARN of the Secrets Manager secret created by aws/secrets-setup.sh, e.g.
# arn:aws:secretsmanager:us-east-1:522826260049:secret:insurance-portal-dev/jwt-secret-AbCdEf
# Leave empty until the secret has been created, then set it here (or pass
# via -var) before applying so the ECS task picks up JWT_SECRET securely.
jwt_secret_arn = ""

db_engine_version        = "8.0.35"
db_instance_class        = "db.t3.micro"
db_allocated_storage     = 20
db_name                  = "insurance_portal"
db_username              = "insurance_user"
db_multi_az              = false
db_backup_retention_days = 7

enable_bucket_versioning = true
