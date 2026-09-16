project_name = "insurance-portal"
environment  = "prod"
aws_region   = "us-east-1"

vpc_cidr              = "10.0.0.0/16"
availability_zones    = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs   = ["10.0.0.0/24", "10.0.1.0/24"]
private_subnet_cidrs  = ["10.0.10.0/24", "10.0.11.0/24"]
database_subnet_cidrs = ["10.0.20.0/24", "10.0.21.0/24"]
single_nat_gateway    = true

container_image   = "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/insurance-portal:latest"
container_port    = 8081
task_cpu          = 512
task_memory       = 1024
desired_count     = 3
health_check_path = "/actuator/health/liveness"

container_environment = {
  SERVER_PORT          = "8081"
  CORS_ALLOWED_ORIGINS = "https://example.com"
}

db_engine_version        = "16.4"
db_instance_class        = "db.t3.micro"
db_allocated_storage     = 20
db_name                  = "insurance_portal"
db_username              = "insurance_user"
db_multi_az              = true
db_backup_retention_days = 7

enable_bucket_versioning = true
