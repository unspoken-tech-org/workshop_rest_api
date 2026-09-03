-- ==============================================================================
-- Script: mask_data_qa.sql
-- Purpose: Sensitive data anonymization (PII / LGPD), production API keys 
--          revocation, and dynamic injection of QA API keys from Infisical.
-- ==============================================================================

BEGIN;

-- 1. Temporary function to generate mathematically valid CPFs (Official DV algorithm)
CREATE OR REPLACE FUNCTION generate_valid_cpf(seq INT) RETURNS VARCHAR AS $$
DECLARE
    base_num BIGINT;
    base_str TEXT;
    d INT[];
    sum1 INT := 0;
    sum2 INT := 0;
    dv1 INT;
    dv2 INT;
    i INT;
BEGIN
    base_num := 100000000 + (seq % 899999999);
    base_str := LPAD(base_num::TEXT, 9, '0');
    
    FOR i IN 1..9 LOOP
        d[i] := CAST(SUBSTRING(base_str FROM i FOR 1) AS INT);
    END LOOP;
    
    -- Calculate First Check Digit (DV1)
    FOR i IN 1..9 LOOP
        sum1 := sum1 + d[i] * (11 - i);
    END LOOP;
    dv1 := 11 - (sum1 % 11);
    IF dv1 >= 10 THEN
        dv1 := 0;
    END IF;
    
    -- Calculate Second Check Digit (DV2)
    FOR i IN 1..9 LOOP
        sum2 := sum2 + d[i] * (12 - i);
    END LOOP;
    sum2 := sum2 + (dv1 * 2);
    dv2 := 11 - (sum2 % 11);
    IF dv2 >= 10 THEN
        dv2 := 0;
    END IF;
    
    RETURN SUBSTRING(base_str FROM 1 FOR 3) || '.' ||
           SUBSTRING(base_str FROM 4 FOR 3) || '.' ||
           SUBSTRING(base_str FROM 7 FOR 3) || '-' ||
           dv1::TEXT || dv2::TEXT;
END;
$$ LANGUAGE plpgsql;

-- 2. Anonymize customers table with valid CPFs and formatted emails
UPDATE customers 
SET name = 'QA Customer ' || id,
    email = 'customer_' || id || '@qa.local',
    cpf = generate_valid_cpf(id)
WHERE email IS NOT NULL OR cpf IS NOT NULL;

-- 3. Anonymize phones table (Ensuring numeric DDD+Number format: '11999999999' for UNIQUE constraint)
UPDATE phones
SET number = '119' || LPAD((80000000 + (id % 9999999))::TEXT, 8, '0'),
    alias = 'QA Phone ' || id;

-- 4. Synchronize customer_contact with the main phone of the device owner
UPDATE customer_contact cc
SET phone = p.number
FROM devices d
JOIN customer_phones cp ON cp.id_customer = d.id_customer
JOIN phones p ON p.id = cp.id_phone
WHERE cc.id_device = d.id AND cp.is_main = true;

-- Fallback for contacts whose customers do not have a main phone
UPDATE customer_contact cc
SET phone = p.number
FROM devices d
JOIN customer_phones cp ON cp.id_customer = d.id_customer
JOIN phones p ON p.id = cp.id_phone
WHERE cc.id_device = d.id AND (cc.phone IS NULL OR cc.phone NOT LIKE '119%');

-- 5. Revoke active session tokens and restored production API Keys
TRUNCATE TABLE refresh_tokens CASCADE;

UPDATE api_keys 
SET active = false,
    key_value = 'revoked_prod_key_' || id;

-- 6. Cleanup temporary CPF function
DROP FUNCTION IF EXISTS generate_valid_cpf(INT);

COMMIT;
