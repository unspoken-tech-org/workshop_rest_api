# Falha de backup

**Alertas:** `WorkshopPostgresWalArchiveFailing` e `WorkshopPostgresWalArchiveStalled`
(críticos); `WorkshopBackupMetricsAbsent`, `WorkshopBackupMetricsUnreadable`,
`WorkshopBackupFullStale`, `WorkshopBackupStale` e `WorkshopBackupStanzaError`
(avisos). Alarme primário: os checks full e diff do Healthchecks.io, fora de PRD e QA.
**Dono:** `platform`.

## Impacto

Sem backup recente ou sem WAL arquivado, a recuperação a um ponto no tempo perde
cobertura. Agenda: full domingo 03:00, diff seg–sáb 20:00, expire diário 04:00.

## Diagnóstico (PRD, só leitura)

1. Journal: `journalctl -t workshop-backup --since "-2d"`.
2. Estado do repositório: `docker exec -u postgres workshop-db pgbackrest info`.
3. Arquivamento: `docker exec -u postgres workshop-db pgbackrest --stanza=workshop check`
   e o painel de WAL no dashboard `PostgreSQL de PRD`.
4. Métricas ausentes ou ilegíveis: `ls -l /srv/pgbackrest/metrics/` (o
   node-exporter roda como `nobody`) e `crontab -l` do usuário `workshop` (bloco
   gerenciado pelo deploy da API).
5. Disco do repositório: [runbook de disco](disco-cheio.md).

## Mitigação

- Backup omitido ou com erro: rode o diff pelo script (`bin/pgbackrest-backup.sh`)
  com o responsável; ele manda os pings e grava as métricas.
- WAL parado: verifique o `archive_command` e o repositório antes que o `pg_wal` cresça.
- Crontab ausente: refaça o deploy da API pela tag vigente (fora do horário da oficina).

## Recuperação

Resolve quando um backup novo sem erro aparece no `pgbackrest info` e no textfile,
o arquivamento volta a avançar e o Healthchecks recebe o ping de sucesso.
