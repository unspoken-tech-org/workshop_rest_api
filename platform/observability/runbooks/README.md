# Runbooks de alertas

Cada alerta do Prometheus e do Loki do plano de controle (VM de QA) aponta para um
destes arquivos pela annotation `runbook_url`. A mensagem do Telegram traz o link.

## Roteamento (`control-plane/alertmanager/alertmanager.yml`)

| Rótulo | Valores |
|---|---|
| `owner` | `platform` (único dono hoje) |
| `environment` | `production` (PRD), `qa` (homologação), `monitoring` (a própria cadeia de observabilidade) |
| `service` | `workshop-api`, `postgres`, `backup`, `host`, `telemetry`, `monitoring` |
| `severity` | `critical`, `warning`, `none` (só o `Watchdog`) |

| Rota | Receiver | Espera / intervalo / repetição |
|---|---|---|
| `alertname="Watchdog"` | `healthchecks` (Healthchecks.io) | 0s / 1m / 1m |
| `owner="platform"`, `severity="critical"` | `telegram-critical` | 10s / 2m / 1h |
| `owner="platform"`, `environment="qa"` | `telegram` | 30s / 5m / 24h |
| `owner="platform"`, demais avisos | `telegram` | 30s / 5m / 12h |
| sem dono (raiz) | `telegram` | 30s / 5m / 12h |

Agrupamento por `alertname`, `service`, `environment` e `severity`; a recuperação
também é notificada (`send_resolved`). O destinatário secundário dos críticos é o
mesmo bot e o mesmo chat do Telegram (decisão do responsável em 07/11/2026; risco
aceito: sem o Telegram, só o `Watchdog` e os checks de backup avisam por fora).

Inibições:

- crítico cala o aviso do mesmo `service`, `environment` e `mountpoint`;
- `WorkshopEdgeTelemetryAbsent` cala as ausências de host, containers, banco, API,
  Alloy e logs de PRD, que dependem da borda;
- container da API ausente ou API fora na sonda calam `WorkshopApiScrapeFailing`;
  container do banco ausente cala `WorkshopPostgresDown`;
- o `Watchdog` nunca é alvo de inibição. Não crie silêncio que o alcance.

## Alertas por runbook

| Runbook | Alertas |
|---|---|
| [API indisponível](api-indisponivel.md) | `WorkshopApiProductionDown`, `WorkshopApiProductionProbeAbsent`, `WorkshopApiScrapeFailing`, `WorkshopApiMetricsAbsent`, `WorkshopApiContainerAbsent`, `WorkshopContainerRestarting`, `WorkshopApiErrorBudgetBurnFast`, `WorkshopApiErrorBudgetBurnSlow`, `WorkshopApiQaScrapeFailing` |
| [Alta latência](alta-latencia.md) | `WorkshopApiProductionSlow`, `WorkshopApiLatencyBudgetBurnFast`, `WorkshopApiLatencyBudgetBurnSlow` |
| [PostgreSQL](postgresql.md) | `WorkshopPostgresDown`, `WorkshopPostgresMetricsAbsent`, `WorkshopDbContainerAbsent`, `WorkshopPostgresConnectionsHigh`, `WorkshopPostgresDeadlocks` |
| [Disco cheio](disco-cheio.md) | `WorkshopHostFilesystemLow`, `WorkshopHostFilesystemCritical`, `QaHostFilesystemLow`, `QaHostFilesystemCritical` |
| [Collector/Alloy parado](collector-alloy.md) | `OTelCollector*`, `Alloy*`, `DockerLogsProxyRefusals` |
| [Queda de ingestão](queda-ingestao.md) | `WorkshopEdgeTelemetryAbsent`, `WorkshopHostMetricsAbsent`, `WorkshopHostScrapeFailing`, `WorkshopContainerMetricsAbsent`, `PrometheusOutOfOrderSamplesRejected`, `LokiDiscardedSamples`, `ProductionLogsAbsent`, `QaLogsAbsent` |
| [Falha de backup](falha-backup.md) | `WorkshopBackup*`, `WorkshopPostgresWalArchiveFailing`, `WorkshopPostgresWalArchiveStalled`, checks de backup do Healthchecks.io |
| [QA ou monitoramento fora](qa-monitoramento.md) | `Watchdog` (aviso do Healthchecks.io), `ObservabilityTargetDown`, `PrometheusRuleEvaluationFailures` |

## Regras gerais

- Acesso às VMs só por Tailscale SSH. Diagnóstico em PRD é só leitura; correção em
  PRD segue o fluxo de deploy (tag `vX.Y.Z` ou `release/*`, fora do horário da oficina).
- Nunca cole tokens, URLs do Healthchecks, `.env` ou valores de env de container em
  chat, issue ou PR.
- Ao encerrar, confirme a mensagem de recuperação no Telegram e registre o incidente
  no plano de observabilidade.
