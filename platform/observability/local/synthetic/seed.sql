-- Base sintética da Fase 0.5 (R23): nenhum dado de cliente, só a API key
-- local usada pela jornada sentinela. O valor vem do secret montado e não
-- fica no repositório. Executado depois que o Flyway da API criou o schema.
\set ON_ERROR_STOP on
\set api_key `cat /run/secrets/synthetic_api_key`

INSERT INTO api_keys (key_value, client_name, user_identifier, platform, role, description, active)
VALUES (:'api_key', 'local-synthetic', 'synthetic-operator', 'SERVER', 'ADMIN',
        'Chave sintética da stack local de observabilidade', true)
ON CONFLICT (key_value) DO NOTHING;
