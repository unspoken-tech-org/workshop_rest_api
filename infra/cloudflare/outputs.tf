output "tunnel_id" {
  description = "ID of the remotely managed observability-qa Tunnel."
  value       = cloudflare_zero_trust_tunnel_cloudflared.observability.id
}

output "tunnel_cname_target" {
  description = "Cloudflare CNAME target used by the Grafana DNS record."
  value       = local.tunnel_cname_target
}

output "tunnel_token" {
  description = "Sensitive connector token read from the state by the control-plane deploy; never log it."
  value       = data.cloudflare_zero_trust_tunnel_cloudflared_token.observability.token
  sensitive   = true
}

output "grafana_hostname" {
  description = "Hostname protected by the Grafana Cloudflare Access application."
  value       = var.grafana_hostname
}

output "grafana_access_application_id" {
  description = "Cloudflare Access application ID for Grafana."
  value       = cloudflare_zero_trust_access_application.grafana.id
}
