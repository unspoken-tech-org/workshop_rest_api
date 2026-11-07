# QA ou monitoramento fora

**Sinais:** aviso por e-mail do Healthchecks.io de que o `Watchdog` parou;
`ObservabilityTargetDown` e `PrometheusRuleEvaluationFailures` (avisos).
**Dono:** `platform`.

## Impacto

Com a QA, o Prometheus ou o Alertmanager fora, nenhum alerta de PRD é entregue.
Risco aceito: com PRD e QA fora juntas, só o `Watchdog` e os checks de backup avisam.

## Regras

- O `Watchdog` vai só ao Healthchecks.io, a cada minuto, e nenhuma inibição o
  alcança. Nunca crie silêncio que o case, nem durante manutenção da QA: avise-se
  pelo próprio Healthchecks (pausa do check) se a parada for planejada.
- A URL de ping é segredo (`OBS_QA_HEALTHCHECKS_PING_URL`); não a cole em lugar nenhum.

## Diagnóstico

1. A VM de QA responde por Tailscale SSH? Se não, o problema é a VM ou a rede; a API
   de PRD pode estar sã (confira pelo navegador).
2. Na QA: `docker compose ps` do plano de controle; procure `prometheus`,
   `alertmanager` e `cloudflared` fora de `healthy`.
3. Alertmanager de pé e Watchdog parado: a saída para o Healthchecks
   (`cp-alert-egress`) ou o segredo em arquivo.
4. Telegram sem mensagens com o Watchdog ok: token ou chat inválidos; veja o log do
   Alertmanager (`docker logs --tail 100 alertmanager`), sem colar o token.
5. Falha de avaliação: `rule_group` no `/rules` do Prometheus (pelo Grafana).

## Mitigação

- Redeploy pelo `deploy-observability-control-plane-qa` (`main`, `-f confirm=DEPLOY`).
- Disco cheio na QA: [runbook de disco](disco-cheio.md).

## Recuperação

O check do Healthchecks volta a `up` no primeiro ping; os alvos voltam a `up 1`.
Depois de uma queda, confira o que disparou enquanto a QA estava fora.
