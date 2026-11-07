# Disco cheio

**Alertas:** `WorkshopHostFilesystemCritical` e `QaHostFilesystemCritical`
(críticos, menos de 15% livre), `WorkshopHostFilesystemLow` e `QaHostFilesystemLow`
(avisos, menos de 30%). O crítico inibe o aviso do mesmo ponto de montagem.
**Dono:** `platform`.

## Impacto

- PRD: o PostgreSQL para de aceitar escritas e o pgBackRest falha.
- QA: disco único com homologação e observabilidade; o PostgreSQL de QA e o
  Prometheus param primeiro.

## Diagnóstico (só leitura)

1. `df -h` e `docker system df`.
2. PRD: `du -sh /srv/pgbackrest` e o tamanho dos logs do Docker
   (`du -sh /var/lib/docker/containers`). Não rode `find /` em PRD.
3. QA: volumes de `prometheus`, `loki`, `tempo` e a fila do Collector
   (`docker system df -v`). Fila crescendo indica destino fora; veja
   [Collector/Alloy](collector-alloy.md).

## Mitigação

- QA: reduza retenção ou ingestão (Prometheus 30d/8GB, Loki 168h) por PR e deploy do
  plano de controle; imagens antigas saem com `docker image prune` (QA apenas).
- PRD: revise a retenção do pgBackRest e a rotação de logs do Docker por PR.
  Limpeza manual em PRD só com o responsável.

## Recuperação

Resolve quando o ponto de montagem volta acima de 15% (crítico) ou 30% (aviso).
