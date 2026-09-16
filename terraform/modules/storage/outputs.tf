output "bucket_name" {
  description = "Name of the application storage S3 bucket."
  value       = aws_s3_bucket.app_storage.bucket
}

output "bucket_arn" {
  description = "ARN of the application storage S3 bucket."
  value       = aws_s3_bucket.app_storage.arn
}
