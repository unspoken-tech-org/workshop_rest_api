# Alta latência da API

**Alertas:** `WorkshopApiLatencyBudgetBurnFast` (crítico),
`WorkshopApiLatencyBudgetBurnSlow` e `WorkshopApiProductionSlow` (avisos).
**Dono:** `platform`.

## Impacto

O SLO de latência é 99% das requisições de negócio abaixo de 500 ms em 28 dias.
O burn rate rápido indica que o orçamento acaba em poucos dias. A sonda lenta
(mais de 5 s) indica degradação percebida por fora.

## Diagnóstico

1. No dashboard `Workshop API — SLOs`, veja a razão de lentidão em 5m e 1h e as
   rotas afetadas.
2. No Tempo, filtre os traces de PRD acima de 500 ms e identifique o trecho lento
   (consulta ao banco, chamada externa, serialização).
3. Conferir saturação: CPU e memória do host e do container no dashboard `Hosts` e
   de containers; conexões do banco (`WorkshopPostgresConnectionsHigh`).
4. Só a sonda lenta e os SLOs normais: suspeite da rede da QA ou da Cloudflare.

## Mitigação

- Regressão após deploy: rollback para a tag anterior.
- Consulta lenta: corrija em PR normal (índice ou consulta) e implante fora do
  horário da oficina.
- Host saturado: verifique backup em execução (full domingo 03:00, diff 20:00).

## Recuperação

Resolve quando as razões de lentidão voltam abaixo do limiar nas duas janelas, ou
quando a sonda conclui em menos de 5 s.
