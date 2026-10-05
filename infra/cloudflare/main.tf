locals {
  tunnel_cname_target = format(
    "%s.cfargotunnel.com",
    cloudflare_zero_trust_tunnel_cloudflared.observability.id,
  )
}

resource "cloudflare_zero_trust_tunnel_cloudflared" "observability" {
  account_id = var.cloudflare_account_id
  name       = var.tunnel_name
  config_src = "cloudflare"
}

resource "cloudflare_zero_trust_tunnel_cloudflared_config" "observability" {
  account_id = var.cloudflare_account_id
  tunnel_id  = cloudflare_zero_trust_tunnel_cloudflared.observability.id
  source     = "cloudflare"

  config = {
    ingress = [
      {
        hostname = var.grafana_hostname
        service  = var.caddy_origin_service
      },
      {
        service = "http_status:404"
      },
    ]
  }
}

resource "cloudflare_dns_record" "grafana" {
  zone_id = var.cloudflare_zone_id
  name    = var.grafana_hostname
  type    = "CNAME"
  content = local.tunnel_cname_target
  ttl     = 1
  proxied = true
  comment = "Observability Grafana hostname for the observability-qa Tunnel."
}

resource "cloudflare_zero_trust_access_policy" "grafana" {
  account_id = var.cloudflare_account_id
  name       = "Grafana observability operators"
  decision   = "allow"

  include = [
    for email_address in var.grafana_access_emails : {
      email = {
        email = email_address
      }
    }
  ]

  session_duration = var.grafana_access_session_duration
}

resource "cloudflare_zero_trust_access_application" "grafana" {
  account_id = var.cloudflare_account_id
  name       = "Grafana observability QA"
  domain     = var.grafana_hostname
  type       = "self_hosted"

  enable_binding_cookie      = true
  http_only_cookie_attribute = true
  same_site_cookie_attribute = "lax"
  service_auth_401_redirect  = true
  session_duration           = var.grafana_access_session_duration

  # Provider v5 attaches reusable account-level policies through this list;
  # application_id on a standalone policy was removed in provider v5.
  policies = [
    {
      id         = cloudflare_zero_trust_access_policy.grafana.id
      precedence = 1
    },
  ]
}

data "cloudflare_zero_trust_tunnel_cloudflared_token" "observability" {
  account_id = var.cloudflare_account_id
  tunnel_id  = cloudflare_zero_trust_tunnel_cloudflared.observability.id
}
