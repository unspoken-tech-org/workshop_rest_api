# Queda de ingestão

**Alertas:** `WorkshopEdgeTelemetryAbsent`, `WorkshopHostMetricsAbsent`,
`WorkshopHostScrapeFailing`, `WorkshopContainerMetricsAbsent` (críticos);
`PrometheusOutOfOrderSamplesRejected`, `LokiDiscardedSamples`,
`ProductionLogsAbsent`, `QaLogsAbsent` (avisos).
**Dono:** `platform`.

## Impacto

O monitoramento de PRD fica cego, total ou parcialmente. Ausência de dados não é
estado saudável.

## Diagnóstico

1. **`WorkshopEdgeTelemetryAbsent`** inibe as ausências derivadas (host, containers,
   banco, API, Alloy e logs de PRD). Cruze com a sonda pública:
   - sonda falhando também: a VM de PRD pode estar fora; siga o
     [runbook da API](api-indisponivel.md);
   - sonda saudável: a borda, o Tailscale ou o Collector interno da QA estão fora.
     Confira `OTelCollectorQueueBacklog` e `ObservabilityTargetDown{job="otel-collector-internal"}`.
2. Só uma ausência (host ou containers) com a borda viva: o coletor específico da
   borda (`node-exporter`, `containers-collector`, `docker-api-proxy`) está parado.
3. Amostras rejeitadas ou linhas descartadas: replay fora da janela de 6 horas,
   limite de taxa ou linha grande demais (`reason` no rótulo). Houve perda.
4. Logs ausentes por 24 h (PRD) ou 72 h (QA): sem tráfego é possível; confirme com
   uma requisição real antes de agir.

## Mitigação

- Borda: redeploy pelo `deploy-observability-edge-prod` (só `main`, aprovação).
- Collector interno da QA: `deploy-observability-control-plane-qa`.
- Perda confirmada: registre a janela perdida no plano.

## Recuperação

Resolve quando as séries voltam (janela de 5 ou 10 minutos). O replay da fila
traz o atraso; amostras antigas não devem parecer atuais nos dashboards.
