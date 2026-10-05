# Configuração descartável do drill de lock e restauração do state R2.
# Usa o mesmo backend da borda Cloudflare (infra/cloudflare/versions.tf), mas
# numa key isolada em drills/, e nenhum provider externo. Só o workflow
# terraform-state-drill-observability.yml a executa.

terraform {
  required_version = ">= 1.10.0"

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

variable "marker" {
  description = "Value recorded in the drill state."
  type        = string
}

variable "hold_seconds" {
  description = "Seconds the apply keeps the state lock while creating the marker."
  type        = number
  default     = 0
}

resource "terraform_data" "marker" {
  input            = var.marker
  triggers_replace = var.marker

  # Provisioners run only on create; a new marker replaces the resource, so the
  # apply sleeps while holding the lock.
  provisioner "local-exec" {
    command = "sleep ${var.hold_seconds}"
  }
}

output "marker" {
  value = terraform_data.marker.output
}
