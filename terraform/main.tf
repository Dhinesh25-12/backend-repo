module "network" {
  source = "./modules/network"

  project_name          = var.project_name
  environment           = var.environment
  vpc_cidr              = var.vpc_cidr
  availability_zones    = var.availability_zones
  public_subnet_cidrs   = var.public_subnet_cidrs
  private_subnet_cidrs  = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
  single_nat_gateway    = var.single_nat_gateway
}

module "security_groups" {
  source = "./modules/security-groups"

  project_name   = var.project_name
  environment    = var.environment
  vpc_id         = module.network.vpc_id
  container_port = var.container_port
}

module "storage" {
  source = "./modules/storage"

  project_name      = var.project_name
  environment       = var.environment
  enable_versioning = var.enable_bucket_versioning
}

module "database" {
  source = "./modules/database"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.network.vpc_id
  database_subnet_ids   = module.network.database_subnet_ids
  security_group_id     = module.security_groups.database_security_group_id
  engine_version        = var.db_engine_version
  instance_class        = var.db_instance_class
  allocated_storage     = var.db_allocated_storage
  db_name               = var.db_name
  db_username           = var.db_username
  multi_az              = var.db_multi_az
  backup_retention_days = var.db_backup_retention_days
}

module "compute" {
  source = "./modules/compute"

  project_name              = var.project_name
  environment               = var.environment
  vpc_id                    = module.network.vpc_id
  public_subnet_ids         = module.network.public_subnet_ids
  private_subnet_ids        = module.network.private_subnet_ids
  alb_security_group_id     = module.security_groups.alb_security_group_id
  ecs_security_group_id     = module.security_groups.ecs_security_group_id
  container_image           = var.container_image
  container_port            = var.container_port
  task_cpu                  = var.task_cpu
  task_memory               = var.task_memory
  desired_count             = var.desired_count
  health_check_path         = var.health_check_path
  container_environment     = var.container_environment
  jwt_secret_arn            = var.jwt_secret_arn
  db_credentials_secret_arn = module.database.db_credentials_secret_arn
}
