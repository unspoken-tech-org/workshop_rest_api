# Filtro de scanners na borda da zona. Os hosts da API recebem varreduras
# genéricas (arquivos de segredo, WordPress, PHP) que hoje atravessam o túnel,
# o Caddy e o Spring Security só para levar 401. As regras abaixo cabem no plano
# Free: sem o operador `matches` (regex), uma regra customizada e a única regra
# de rate limit, com período e mitigação fixos em 10 s e contagem por IP.
#
# Cada fase aceita um único ruleset de entrada por zona. Se já houver regras
# criadas pelo painel nessas fases, importe o ruleset antes do apply.

locals {
  api_hosts_expression = format(
    "http.host in {%s}",
    join(" ", [for host in sort(tolist(var.api_hostnames)) : format("%q", host)]),
  )

  # Trechos de caminho que a API nunca serve (rotas em /v1 e /api/auth).
  scanner_path_contains = [
    "/.env",
    "/.git",
    "/.aws",
    "/.ssh",
    "/wp-",
    "/wordpress",
    "/phpmyadmin",
  ]

  scanner_path_ends_with = [
    ".php",
  ]

  scanner_path_expression = join(" or ", concat(
    [for fragment in local.scanner_path_contains : format("lower(http.request.uri.path) contains %q", fragment)],
    [for suffix in local.scanner_path_ends_with : format("ends_with(lower(http.request.uri.path), %q)", suffix)],
  ))
}

resource "cloudflare_ruleset" "zone_custom_firewall" {
  zone_id     = var.cloudflare_zone_id
  name        = "default"
  description = "Custom rules da zona, gerenciadas pelo Terraform."
  kind        = "zone"
  phase       = "http_request_firewall_custom"

  rules = [
    {
      ref         = "block_api_scanner_paths"
      description = "Bloqueia caminhos de scanner nos hosts da API"
      expression  = format("(%s) and (%s)", local.api_hosts_expression, local.scanner_path_expression)
      action      = "block"
      enabled     = true
    },
  ]
}

# No Free a expressão só aceita o caminho, então o limite vale para a zona toda,
# inclusive o Grafana. Os assets estáticos do Grafana (/public/) ficam fora da
# contagem para que o carregamento de um dashboard não esbarre no limite.
resource "cloudflare_ruleset" "zone_rate_limit" {
  zone_id     = var.cloudflare_zone_id
  name        = "default"
  description = "Rate limiting da zona, gerenciado pelo Terraform."
  kind        = "zone"
  phase       = "http_ratelimit"

  rules = [
    {
      ref         = "rate_limit_per_ip"
      description = "Limita requisições por IP fora dos assets estáticos"
      expression  = "not starts_with(http.request.uri.path, \"/public/\")"
      action      = "block"
      enabled     = true
      ratelimit = {
        characteristics     = ["cf.colo.id", "ip.src"]
        period              = 10
        requests_per_period = var.rate_limit_requests_per_10s
        mitigation_timeout  = 10
      }
    },
  ]
}
