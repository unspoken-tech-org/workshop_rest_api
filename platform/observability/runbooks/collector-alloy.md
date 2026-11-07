# Collector ou Alloy parado

**Alertas:** `OTelCollectorExportFailures`, `OTelCollectorEnqueueFailures`,
`OTelCollectorQueueBacklog`, `OTelCollectorReceiverRefused`,
`AlloyProductionMetricsAbsent` (críticos); `OTelCollectorQueueNearlyFull`,
`OTelCollectorMemoryPressure`, `AlloyQaMetricsAbsent`, `AlloyComponentUnhealthy`,
`AlloyDockerDiscoveryFailing`, `AlloyLogsQueueBacklog`, `DockerLogsProxyRefusals`
(avisos).
**Dono:** `platform`.

## Impacto

Métricas, traces ou logs param de chegar ou são descartados. A borda de PRD tem
fila em disco de até 4 horas; depois disso há perda.

## Diagnóstico

1. O rótulo `job` ou `exporter` diz qual Collector: `otel-collector-internal` é o
   da QA; `workshop/otelcol-contrib` é a borda de PRD.
2. Fila acumulando sem envio: o destino está fora. Na QA, confira `loki`, `tempo` e
   `prometheus` (`docker compose ps`). Na borda, o caminho até a QA (Tailscale).
3. Falha de enfileiramento ou recusa: fila cheia ou `memory_limiter`. Veja o RSS no
   dashboard do Collector.
4. Alloy: `docker logs --tail 100 alloy` (sem colar conteúdo de logs de cliente). Falha
   de discovery é o `docker-logs-proxy` fora ou a allowlist recusando.

## Mitigação

- QA: reinicie o serviço afetado pelo `deploy-observability-control-plane-qa`
  (`-f confirm=DEPLOY`), não à mão.
- Borda de PRD: `deploy-observability-edge-prod` só de `main`, com aprovação.
- Não apague a fila em disco: ela drena ao destino voltar.

## Recuperação

Resolve quando o envio volta (`sent` cresce), a fila esvazia e a recusa para. Depois
de uma queda longa, confira `PrometheusOutOfOrderSamplesRejected` e
`LokiDiscardedSamples` ([queda de ingestão](queda-ingestao.md)).
