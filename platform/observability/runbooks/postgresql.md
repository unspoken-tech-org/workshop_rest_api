# PostgreSQL de PRD

**Alertas:** `WorkshopPostgresDown`, `WorkshopPostgresMetricsAbsent`,
`WorkshopDbContainerAbsent` (críticos), `WorkshopPostgresConnectionsHigh` e
`WorkshopPostgresDeadlocks` (avisos). Arquivamento de WAL: ver
[falha de backup](falha-backup.md).
**Dono:** `platform`.

## Impacto

Sem o banco, a API falha nas requisições de negócio. `PostgresDown` com a API
saudável aponta para o caminho de monitoramento, não para o banco.

## Diagnóstico (PRD, só leitura)

1. `docker ps -a --filter name=workshop-db` e
   `docker inspect --format '{{.State.Health.Status}}' workshop-db`.
2. `docker logs --tail 200 workshop-db`: procure `FATAL`, `PANIC` e falta de espaço.
3. Rede de métricas: `docker inspect --format '{{json .NetworkSettings.Networks}}' workshop-db`
   deve listar `workshop-db-metrics`. Sem ela, o `postgres-exporter` da borda não
   alcança o banco (o rollback da API reconecta a rede).
4. `MetricsAbsent` com a borda viva: o `postgres-exporter` da borda está parado.
5. Conexões altas: veja `pg_stat_activity` pelo dashboard `PostgreSQL de PRD`.
6. Disco: confira o [runbook de disco](disco-cheio.md).

## Mitigação

- Banco parado ou em loop de reinício: qualquer ação de escrita em PRD (restart,
  recriação) só com o responsável. O volume `workshop_rest_api_db_data` nunca é
  removido.
- Antes do primeiro deploy com um Compose novo, procure redes com o mesmo par
  projeto/rede (incidente de 07/11/2026).
- Deadlock isolado: registre a consulta e corrija em PR.

## Recuperação

Resolve quando `pg_up` e o `up` do exporter voltam a 1 por 3 minutos, ou quando o
container volta a ser reportado.
