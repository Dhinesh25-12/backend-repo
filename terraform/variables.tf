variable "project_name" {
  description = "Short name used to prefix/tag all provisioned resources."
  type        = string
  default     = "insurance-portal"
}

variable "environment" {
  description = "Deployment environment name (e.g. dev, staging, prod)."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS region to deploy resources into."
  type        = string
  default     = "us-east-1"
}

# ---------------------------------------------------------------------------
# Network
# ---------------------------------------------------------------------------

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones to spread subnets across."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ)."
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private application subnets (one per AZ)."
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "database_subnet_cidrs" {
  description = "CIDR blocks for private database subnets (one per AZ)."
  type        = list(string)
  default     = ["10.0.20.0/24", "10.0.21.0/24"]
}

variable "single_nat_gateway" {
  description = "Use a single shared NAT gateway instead of one per AZ (cheaper, less resilient)."
  type        = bool
  default     = true
}

# ---------------------------------------------------------------------------
# Compute (ECS Fargate)
# ---------------------------------------------------------------------------

variable "container_image" {
  description = "Container image URI (e.g. ECR repo URL) to run in the ECS service."
  type        = string
  default     = "public.ecr.aws/docker/library/httpd:latest"
}

variable "container_port" {
  description = "Port the application container listens on."
  type        = number
  default     = 8081
}

variable "task_cpu" {
  description = "Fargate task CPU units."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate task memory (MiB)."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Desired number of running ECS tasks."
  type        = number
  default     = 2
}

variable "health_check_path" {
  description = "HTTP path used by the ALB target group health check."
  type        = string
  default     = "/actuator/health/liveness"
}

variable "container_environment" {
  description = "Plain (non-secret) environment variables passed to the container."
  type        = map(string)
  default     = {}
}

variable "jwt_secret_arn" {
  description = "ARN of a Secrets Manager secret (plaintext string) holding the JWT signing secret, e.g. as created by aws/secrets-setup.sh. Leave empty to fall back to the application's default (insecure) JWT_SECRET."
  type        = string
  default     = ""
}

# ---------------------------------------------------------------------------
# Database (RDS PostgreSQL)
# ---------------------------------------------------------------------------

variable "db_engine_version" {
  description = "PostgreSQL engine version for RDS."
  type        = string
  default     = "16.4"
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage for RDS, in GiB."
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Initial database name."
  type        = string
  default     = "insurance_portal"
}

variable "db_username" {
  description = "Master username for the RDS instance."
  type        = string
  default     = "insurance_user"
}

variable "db_multi_az" {
  description = "Whether to deploy the RDS instance across multiple AZs for high availability."
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Number of days to retain automated RDS backups."
  type        = number
  default     = 7
}

# ---------------------------------------------------------------------------
# Storage
# ---------------------------------------------------------------------------

variable "enable_bucket_versioning" {
  description = "Enable S3 bucket versioning for the application storage bucket."
  type        = bool
  default     = true
}
