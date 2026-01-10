-- Insert test API Keys for each platform
-- Roles: ADMIN for the first 4 to maintain compatibility with existing tests
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (1, 'sk_mobile_TEST_KEY_MOBILE_12345678901234567890', 'test_app', 'admin_user', 'MOBILE', 'ADMIN', 'Test API Key - Mobile Admin', true, CURRENT_TIMESTAMP),
    (2, 'sk_web_TEST_KEY_WEB_12345678901234567890', 'test_app', 'admin_user', 'WEB', 'ADMIN', 'Test API Key - Web Admin', true, CURRENT_TIMESTAMP),
    (3, 'sk_desktop_TEST_KEY_DESKTOP_12345678901234567890', 'test_app', 'admin_user', 'DESKTOP', 'ADMIN', 'Test API Key - Desktop Admin', true, CURRENT_TIMESTAMP),
    (4, 'sk_server_TEST_KEY_SERVER_12345678901234567890', 'test_app', 'admin_user', 'SERVER', 'ADMIN', 'Test API Key - Server Admin', true, CURRENT_TIMESTAMP);

-- Insert a SERVICE role API Key for access restriction testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (7, 'sk_mobile_TEST_KEY_SERVICE_12345678901234567890', 'test_service_app', 'service_user', 'MOBILE', 'SERVICE', 'Test API Key - Service Only', true, CURRENT_TIMESTAMP);

-- Insert an expired API Key for testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at, expires_at)
VALUES
    (5, 'sk_mobile_TEST_KEY_EXPIRED_12345678901234567890', 'test_expired', 'expired_user', 'MOBILE', 'SERVICE', 'Test API Key - Expired', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Insert an inactive API Key for testing
INSERT INTO api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at)
VALUES
    (6, 'sk_mobile_TEST_KEY_INACTIVE_12345678901234567890', 'test_inactive', 'inactive_user', 'MOBILE', 'SERVICE', 'Test API Key - Inactive', false, CURRENT_TIMESTAMP);

-- Insert an expired Refresh Token for testing (linked to api_key_id 1)
INSERT INTO refresh_tokens (id, token, api_key_id, device_id, expires_at, revoked, created_at)
VALUES
    (1, 'rt_EXPIRED_TOKEN_1234567890', 1, 'test-device-expired', CURRENT_TIMESTAMP - INTERVAL '1 hour', false, CURRENT_TIMESTAMP - INTERVAL '1 day');