# API indisponível

**Alertas:** `WorkshopApiProductionDown`, `WorkshopApiProductionProbeAbsent`,
`WorkshopApiScrapeFailing`, `WorkshopApiMetricsAbsent`, `WorkshopApiContainerAbsent`,
`WorkshopContainerRestarting`, `WorkshopApiErrorBudgetBurnFast` (críticos) e
`WorkshopApiErrorBudgetBurnSlow`, `WorkshopApiQaScrapeFailing` (avisos).
**Dono:** `platform`.

## Impacto

Clientes da oficina não conseguem usar o sistema, ou parte das requisições volta
com `5xx`. O burn rate rápido consome 2% do orçamento de 28 dias em 1 h.

## Diagnóstico

1. **Sonda pública** (`WorkshopApiProductionDown`): a sonda sai da VM de QA pela
   internet e pela Cloudflare. Se `WorkshopEdgeTelemetryAbsent` também dispara, a VM
   de PRD inteira pode estar fora. Se só a sonda falha, compare com o acesso pelo
   navegador antes de concluir: a rede da QA também dispara este alerta.
2. **Sonda sem resultado** (`ProbeAbsent`): o problema é o blackbox na QA. A
   detecção está cega; veja [QA ou monitoramento fora](qa-monitoramento.md).
3. **Container** (`ContainerAbsent` ou `ContainerRestarting`): em PRD, só leitura:
   `docker ps -a --filter name=workshop-api`, `docker logs --tail 200 workshop-api`
   (não cole dados de cliente) e `docker inspect --format '{{.State.Health.Status}}' workshop-api`.
4. **Erros 5xx** (burn rate): no Grafana, abra o dashboard `Workshop API — SLOs` e
   os traces e logs da janela no Tempo e no Loki. Procure por rota e exceção.
5. **Banco**: se o PostgreSQL também alerta, comece pelo [runbook do PostgreSQL](postgresql.md).

## Mitigação

- Deploy recente? Faça rollback para a tag anterior pelo `deploy-api-prod`
  (`--ref` da tag, aprovação do Environment `production`).
- Container parado sem deploy: o `restart` pelo compose de produção é mudança em PRD;
  combine com o responsável antes.
- QA ou rede da QA fora: a API pode estar sã; trate pelo runbook da QA.

## Recuperação

O alerta resolve quando a sonda volta a `probe_success 1` (3 min), o `up` do scrape
volta a 1 ou as razões de erro caem abaixo do limiar. O Telegram recebe a
mensagem de resolução.
