terraform {
  required_version = ">= 1.10.0"

  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "5.24.0"
    }
  }

  # Cloudflare R2 (S3 compatível). Bucket, key e endpoint vêm do ambiente
  # confiável por -backend-config; aqui ficam só os ajustes fixos do R2. O lock
  # usa escrita condicional do R2 (use_lockfile, Terraform >= 1.10).
  backend "s3" {
    region                      = "auto"
    use_lockfile                = true
    use_path_style              = true
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    skip_s3_checksum            = true
  }
}

provider "cloudflare" {
  api_token = var.obs_cf_api_token
}
