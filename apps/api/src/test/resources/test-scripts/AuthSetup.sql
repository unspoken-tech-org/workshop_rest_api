-- Insert test API Keys for each platform (stored as SHA-256 hex hashes matching TestAuthHelper plaintext keys)
-- Roles: ADMIN for the first 4 + id 8 to maintain compatibility with existing tests
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (1, encode(sha256(convert_to('sk_mobile_TEST_KEY_MOBILE_12345678901234567890', 'UTF8')), 'hex'), 'test_app', 'admin_user', 'MOBILE', 'ADMIN', 'Test API Key - Mobile Admin', true, CURRENT_TIMESTAMP),
    (2, encode(sha256(convert_to('sk_web_TEST_KEY_WEB_12345678901234567890', 'UTF8')), 'hex'), 'test_app', 'admin_user', 'WEB', 'ADMIN', 'Test API Key - Web Admin', true, CURRENT_TIMESTAMP),
    (3, encode(sha256(convert_to('sk_desktop_TEST_KEY_DESKTOP_12345678901234567890', 'UTF8')), 'hex'), 'test_app', 'admin_user', 'DESKTOP', 'ADMIN', 'Test API Key - Desktop Admin', true, CURRENT_TIMESTAMP),
    (4, encode(sha256(convert_to('sk_server_TEST_KEY_SERVER_12345678901234567890', 'UTF8')), 'hex'), 'test_app', 'admin_user', 'SERVER', 'ADMIN', 'Test API Key - Server Admin', true, CURRENT_TIMESTAMP),
    (8, encode(sha256(convert_to('sk_mobile_TEST_KEY_MOBILE_2_12345678901234567890', 'UTF8')), 'hex'), 'test_app', 'second_user', 'MOBILE', 'ADMIN', 'Test API Key - Mobile Admin 2', true, CURRENT_TIMESTAMP);

-- Insert a SERVICE role API Key for access restriction testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (7, encode(sha256(convert_to('sk_mobile_TEST_KEY_SERVICE_12345678901234567890', 'UTF8')), 'hex'), 'test_service_app', 'service_user', 'MOBILE', 'SERVICE', 'Test API Key - Service Only', true, CURRENT_TIMESTAMP);

-- Insert an expired API Key for testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at, expires_at)
VALUES
    (5, encode(sha256(convert_to('sk_mobile_TEST_KEY_EXPIRED_12345678901234567890', 'UTF8')), 'hex'), 'test_expired', 'expired_user', 'MOBILE', 'SERVICE', 'Test API Key - Expired', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Insert an inactive API Key for testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (6, encode(sha256(convert_to('sk_mobile_TEST_KEY_INACTIVE_12345678901234567890', 'UTF8')), 'hex'), 'test_inactive', 'inactive_user', 'MOBILE', 'SERVICE', 'Test API Key - Inactive', false, CURRENT_TIMESTAMP);

-- Insert an expired Refresh Token for testing (linked to api_key_id 1)
INSERT INTO refresh_tokens (id, token, api_key_id, expires_at, revoked, created_at)
VALUES
    (1, encode(sha256(convert_to('rt_EXPIRED_TOKEN_1234567890', 'UTF8')), 'hex'), 1, CURRENT_TIMESTAMP - INTERVAL '1 hour', false, CURRENT_TIMESTAMP - INTERVAL '1 day');
