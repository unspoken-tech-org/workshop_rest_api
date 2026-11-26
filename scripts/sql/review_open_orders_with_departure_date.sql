-- Revisao das OS ABERTAS que ainda tem data de saida (8 em PRD em 25/11).
-- Plano: PLANO_SEPARACAO_APARELHO_OS, Fase 0 / decisao 6 (data de saida imutavel).
-- Provavelmente reaberturas antigas que mantiveram a data. Devem ser corrigidas
-- ANTES da migration de expansao (Fase 1).
--
-- SOMENTE LEITURA: nao mostra nome, CPF nem telefone; so ids, status e datas.
-- A correcao fica COMENTADA ao final: a decisao de cada OS e do usuario.
--
-- Uso:  psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f scripts/sql/review_open_orders_with_departure_date.sql

BEGIN TRANSACTION READ ONLY;

\echo '== A. OS abertas (status fora de ENTREGUE/DESCARTADO) com departure_date =='
SELECT d.id AS os, d.device_status, d.entry_date, d.departure_date, d.is_revision,
       (SELECT max(h.history_date) FROM device_history h
         WHERE h.id_device = d.id AND h.field_name = 'device_status'
           AND h.old_value IN ('ENTREGUE', 'DESCARTADO')) AS reaberta_em,
       (SELECT max(h.history_date) FROM device_history h
         WHERE h.id_device = d.id AND h.field_name = 'device_status'
           AND h.new_value IN ('ENTREGUE', 'DESCARTADO')) AS ultimo_encerramento
FROM devices d
WHERE d.departure_date IS NOT NULL
  AND d.device_status NOT IN ('ENTREGUE', 'DESCARTADO')
ORDER BY d.id;

\echo
\echo '== B. Historico de status dessas OS =='
SELECT h.id_device AS os, h.history_date, h.field_name, h.old_value, h.new_value
FROM device_history h
WHERE h.field_name IN ('device_status', 'departure_date')
  AND h.id_device IN (SELECT id FROM devices
                       WHERE departure_date IS NOT NULL
                         AND device_status NOT IN ('ENTREGUE', 'DESCARTADO'))
ORDER BY h.id_device, h.history_date;

\echo
\echo '== C. Pagamentos dessas OS (valor, nunca dados do cliente) =='
SELECT p.id_device AS os, count(*) AS pagamentos
FROM payments p
WHERE p.id_device IN (SELECT id FROM devices
                       WHERE departure_date IS NOT NULL
                         AND device_status NOT IN ('ENTREGUE', 'DESCARTADO'))
GROUP BY p.id_device ORDER BY p.id_device;

ROLLBACK;

-- ---------------------------------------------------------------------------
-- CORRECAO (decida por OS depois de ler A e B; rode em transacao, com backup):
--
-- Opcao 1: a OS foi reaberta de verdade e segue em atendimento -> limpar a data.
--   BEGIN;
--   UPDATE devices SET departure_date = NULL
--    WHERE id IN (/* ids */) AND device_status NOT IN ('ENTREGUE', 'DESCARTADO');
--   -- conferir: SELECT id, device_status, departure_date FROM devices WHERE id IN (/* ids */);
--   COMMIT;
--
-- Opcao 2: o aparelho foi de fato entregue e o status ficou errado -> corrigir o status.
--   BEGIN;
--   UPDATE devices SET device_status = 'ENTREGUE'
--    WHERE id IN (/* ids */) AND departure_date IS NOT NULL;
--   COMMIT;
-- (A API grava device_history nas mudancas feitas por ela; ajuste direto no SQL nao
--  grava. Se quiser o historico, prefira corrigir pela tela do app.)
-- ---------------------------------------------------------------------------
