variable "obs_cf_api_token" {
  description = "Scoped Cloudflare API token injected from OBS_CF_API_TOKEN by the trusted deploy environment."
  type        = string
  sensitive   = true
  nullable    = false
}

variable "cloudflare_account_id" {
  description = "Cloudflare account ID that owns the observability Tunnel and Access application."
  type        = string
  nullable    = false
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID for eletroluk.com; supplied out-of-band, never guessed."
  type        = string
  nullable    = false
}

variable "tunnel_name" {
  description = "Stable name of the remotely managed observability Tunnel."
  type        = string
  default     = "observability-qa"
  nullable    = false
}

variable "grafana_hostname" {
  description = "Public hostname protected by Cloudflare Access for Grafana. Temporary name while the legacy grafana.eletroluk.com stays on the PRD gateway (R8)."
  type        = string
  default     = "grafana-obs.eletroluk.com"
  nullable    = false
}

variable "caddy_origin_service" {
  description = "HTTP service address reachable by cloudflared on the dedicated QA observability network."
  type        = string
  default     = "http://caddy:8080"
  nullable    = false

  validation {
    condition     = can(regex("^https?://[^[:space:]]+$", var.caddy_origin_service))
    error_message = "caddy_origin_service must be an HTTP or HTTPS URL reachable from cloudflared."
  }
}

variable "grafana_access_emails" {
  description = "Exact email addresses allowed to authenticate to Grafana through Cloudflare Access."
  type        = set(string)
  sensitive   = true
  nullable    = false

  validation {
    condition = length(var.grafana_access_emails) > 0 && alltrue([
      for email in var.grafana_access_emails : can(regex("^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$", email))
    ])
    error_message = "grafana_access_emails must contain at least one valid email address."
  }
}

variable "grafana_access_session_duration" {
  description = "Maximum lifetime of a Grafana Access session."
  type        = string
  default     = "8h"
  nullable    = false

  validation {
    condition     = can(regex("^[1-9][0-9]*(m|h)$", var.grafana_access_session_duration))
    error_message = "grafana_access_session_duration must be a positive duration in minutes or hours, for example 8h."
  }
}
