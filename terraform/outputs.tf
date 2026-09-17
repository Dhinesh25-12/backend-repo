output "vpc_id" {
  description = "ID of the provisioned VPC."
  value       = module.network.vpc_id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets."
  value       = module.network.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private application subnets."
  value       = module.network.private_subnet_ids
}

output "database_subnet_ids" {
  description = "IDs of the private database subnets."
  value       = module.network.database_subnet_ids
}

output "alb_dns_name" {
  description = "Public DNS name of the application load balancer."
  value       = module.compute.alb_dns_name
}

output "web_security_group_id" {
  description = "Security group ID intended for a shared frontend/UI compute tier (e.g. the ui-repo EC2 instances) that reuses this VPC instead of provisioning its own network stack."
  value       = module.security_groups.web_security_group_id
}

output "alb_security_group_id" {
  description = "Security group ID for the application load balancer."
  value       = module.security_groups.alb_security_group_id
}

output "ecr_repository_url" {
  description = "URL of the ECR repository to push application images to."
  value       = module.compute.ecr_repository_url
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster."
  value       = module.compute.cluster_name
}

output "ecs_service_name" {
  description = "Name of the ECS service."
  value       = module.compute.service_name
}

output "db_endpoint" {
  description = "Connection endpoint of the RDS database."
  value       = module.database.db_endpoint
}

output "db_credentials_secret_arn" {
  description = "ARN of the Secrets Manager secret holding DB credentials."
  value       = module.database.db_credentials_secret_arn
}

output "storage_bucket_name" {
  description = "Name of the application storage S3 bucket."
  value       = module.storage.bucket_name
}
