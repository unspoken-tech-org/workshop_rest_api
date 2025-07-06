WITH models_agg AS (
  SELECT
    bmt.id_brand,
    bmt.id_type,
    json_agg(
      json_build_object(
        'idModel', m.id,
        'model', m.model
      ) ORDER BY m.model
    ) AS models
  FROM
    brands_models_types bmt
    JOIN models m ON m.id = bmt.id_model
  GROUP BY
    bmt.id_brand,
    bmt.id_type
),
brands_agg AS (
  SELECT
    ma.id_type,
    json_agg(
      json_build_object(
        'idBrand', b.id,
        'brand', b.brand,
        'models', ma.models
      ) ORDER BY b.brand
    ) AS brands
  FROM
    models_agg ma
    JOIN brands b ON b.id = ma.id_brand
  GROUP BY
    ma.id_type
)
SELECT
  json_agg(
    json_build_object(
      'idType', t.id,
      'type', t.type,
      'brands', ba.brands
    ) ORDER BY t.type
  ) AS resultado_json
FROM
  brands_agg ba
  JOIN types t ON t.id = ba.id_type
