WITH all_statuses (status) AS (
    VALUES
        ('DESCARTADO'),
        ('PRONTO'),
        ('APROVADO'),
        ('AGUARDANDO'),
        ('NOVO'),
        ('EM_ANDAMENTO'),
        ('NAO_APROVADO'),
        ('ENTREGUE'),
        ('URGENTE'),
        ('REVISAO')
),
last_month_counts AS (
    SELECT
        CASE
            WHEN field_name = 'urgency' AND new_value = 'true' THEN 'URGENTE'
            WHEN field_name = 'revision' AND new_value = 'true' THEN 'REVISAO'
            WHEN field_name = 'device_status' THEN new_value
        END AS status,
        COUNT(*) AS changes_last_month
    FROM
        device_history
    WHERE
        history_date >= (CURRENT_TIMESTAMP - INTERVAL '1 month')
        AND field_name IN ('urgency', 'revision', 'device_status')
    GROUP BY
        status
),
base_data AS (
    SELECT
        d.device_status as "status",
        d.has_urgency,
        d.is_revision
    FROM
        devices d
),
total_counts AS (
    SELECT status, COUNT(*) AS total FROM base_data GROUP BY status
    UNION ALL
    SELECT 'URGENTE' AS status, COUNT(*) AS total FROM base_data WHERE has_urgency = TRUE
    UNION ALL
    SELECT 'REVISAO' AS status, COUNT(*) AS total FROM base_data WHERE is_revision = TRUE
),
final_counts AS (
    SELECT
        s.status,
        COALESCE(tc.total, 0) AS total,
        COALESCE(lmc.changes_last_month, 0) AS last_month
    FROM
        all_statuses s
    LEFT JOIN
        total_counts tc ON s.status = tc.status
    LEFT JOIN
        last_month_counts lmc ON s.status = lmc.status
),
last_viewed_devices AS (
    select d.id as device_id, c.id as customer_id, c."name" as customer_name, t."type", b.brand, m.model,
    d.device_status as "status", d.problem, d.observation, d.has_urgency, d.is_revision as has_revision, d.entry_date, d.departure_date
    from devices d
    left join customers c on d.id_customer = c.id
    left join brands_models_types bmt on bmt.id = d.id_brand_model_type
    left join brands b on b.id = bmt.id_brand
    left join models m on m.id = bmt.id_model
    left join "types" t on t.id  = bmt.id_type
    where d.last_viewed_at is not null
    order by d.last_viewed_at desc
    limit 10
)
SELECT
    (SELECT jsonb_object_agg(
        status,
        jsonb_build_object(
            'total', total,
            'last_month', last_month
        )
    ) FROM final_counts)
    ||
    jsonb_build_object('last_viewed_devices', COALESCE((SELECT jsonb_agg(lvd) FROM last_viewed_devices lvd), '[]'::jsonb));