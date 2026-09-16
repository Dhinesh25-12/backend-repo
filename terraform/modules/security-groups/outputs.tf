output "alb_security_group_id" {
  description = "Security group ID for the application load balancer."
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "Security group ID for the ECS Fargate service."
  value       = aws_security_group.ecs_service.id
}

output "database_security_group_id" {
  description = "Security group ID for the RDS database."
  value       = aws_security_group.database.id
}
