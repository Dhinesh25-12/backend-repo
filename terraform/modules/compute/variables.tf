variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "alb_security_group_id" {
  type = string
}

variable "ecs_security_group_id" {
  type = string
}

variable "container_image" {
  type = string
}

variable "container_port" {
  type = number
}

variable "task_cpu" {
  type = number
}

variable "task_memory" {
  type = number
}

variable "desired_count" {
  type = number
}

variable "health_check_path" {
  type = string
}

variable "container_environment" {
  type    = map(string)
  default = {}
}

variable "db_credentials_secret_arn" {
  type = string
}

variable "jwt_secret_arn" {
  description = "ARN of a Secrets Manager secret (plaintext string) holding the JWT signing secret. Leave empty to fall back to the application's default (insecure) JWT_SECRET."
  type        = string
  default     = ""
}
