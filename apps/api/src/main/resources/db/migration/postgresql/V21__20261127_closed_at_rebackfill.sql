-- V21 - closed_at das OS encerradas por uma imagem anterior a V20 (rollback)

UPDATE devices
SET closed_at = departure_date
WHERE device_status IN ('ENTREGUE', 'DESCARTADO')
  AND closed_at IS NULL
  AND departure_date IS NOT NULL;

UPDATE devices d
SET closed_at = h.last_close
FROM (
    SELECT id_device, MAX(history_date) AS last_close
    FROM device_history
    WHERE field_name = 'device_status'
      AND new_value IN ('ENTREGUE', 'DESCARTADO')
    GROUP BY id_device
) h
WHERE h.id_device = d.id
  AND d.device_status IN ('ENTREGUE', 'DESCARTADO')
  AND d.closed_at IS NULL;

UPDATE devices
SET closed_at = last_update
WHERE device_status IN ('ENTREGUE', 'DESCARTADO')
  AND closed_at IS NULL
  AND last_update >= TIMESTAMP '2026-06-16 00:00:00';
