# Gateway (Caddy)

**Alertas:** `WorkshopGatewayScrapeFailing`, `WorkshopGatewayMetricsAbsent` (críticos) e
`WorkshopGatewayUpstreamErrors`, `WorkshopGatewayQaScrapeFailing` (avisos).
**Dono:** `platform`.

## Impacto

- **Scrape falhando ou métricas ausentes:** perdemos a visão interna do gateway
  (tráfego, códigos, latência). Para o cliente, o gateway fora já aparece na sonda
  pública (`WorkshopApiProductionDown`).
- **502/503/504:** parte das requisições à API volta com erro do gateway porque o
  upstream (`workshop-api:8080`) está fora, recusando conexões ou lento.

## Diagnóstico

1. **Borda de PRD** (`MetricsAbsent`): se `WorkshopEdgeTelemetryAbsent` também
   dispara, o problema é a borda; ele inibe este alerta. Siga
   [Queda de ingestão](queda-ingestao.md).
2. **Container** (`ScrapeFailing`): em PRD, só leitura:
   `docker ps -a --filter name=caddy-gateway`, `docker logs --tail 200 caddy-gateway`
   e `docker exec caddy-gateway curl -sf -o /dev/null http://127.0.0.1:9180/metrics`.
   O site `:9180` vem do Caddyfile; um deploy que não recriou o container mantém o
   Caddyfile antigo (o deploy de PRD e o de QA usam `--force-recreate`).
3. **502/503/504** (`UpstreamErrors`): no Grafana, compare
   `caddy_http_request_duration_seconds_count` por `code` e `server` com o
   `up` da API. Se a API está fora ou reiniciando, siga
   [API indisponível](api-indisponivel.md); a sonda e o container ausente inibem
   este alerta. Erro com a API sã aponta para o `reverse_proxy` (timeout, rede
   `workshop-metrics`): veja `caddy_reverse_proxy_upstreams_healthy`.
4. **QA** (`QaScrapeFailing`): na VM de QA, o mesmo do item 2 com
   `caddy-gateway-qa` e o alvo `workshop/caddy-gateway-qa` no Prometheus.

## Mitigação

- Deploy recente do gateway? Rode o `deploy-gateway-prod` com o `--ref` da release
  anterior (aprovação do Environment `production`, `confirm=DEPLOY`).
- Container parado sem deploy: o `restart` pelo compose de produção é mudança em PRD;
  combine com o responsável antes.
- 502/503/504 com a API fora: trate a API; o alerta resolve junto.

## Recuperação

Resolve quando o `up` do gateway volta a 1 ou os 502/503/504 ficam abaixo de 3 em
5 minutos. O Telegram recebe a mensagem de resolução.
