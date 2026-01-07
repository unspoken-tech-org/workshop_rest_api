-- Insert test API Keys for each platform
INSERT INTO api_keys (id, key_value, client_name, platform, description, active, created_at)
VALUES 
    (1, 'sk_mobile_TEST_KEY_MOBILE_12345678901234567890', 'test_app', 'MOBILE', 'Test API Key - Mobile', true, CURRENT_TIMESTAMP),
    (2, 'sk_web_TEST_KEY_WEB_12345678901234567890', 'test_app', 'WEB', 'Test API Key - Web', true, CURRENT_TIMESTAMP),
    (3, 'sk_desktop_TEST_KEY_DESKTOP_12345678901234567890', 'test_app', 'DESKTOP', 'Test API Key - Desktop', true, CURRENT_TIMESTAMP),
    (4, 'sk_server_TEST_KEY_SERVER_12345678901234567890', 'test_app', 'SERVER', 'Test API Key - Server', true, CURRENT_TIMESTAMP);

-- Insert an expired API Key for testing
INSERT INTO api_keys (id, key_value, client_name, platform, description, active, created_at, expires_at)
VALUES 
    (5, 'sk_mobile_TEST_KEY_EXPIRED_12345678901234567890', 'test_expired', 'MOBILE', 'Test API Key - Expired', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Insert an inactive API Key for testing
INSERT INTO api_keys (id, key_value, client_name, platform, description, active, created_at)
VALUES 
    (6, 'sk_mobile_TEST_KEY_INACTIVE_12345678901234567890', 'test_inactive', 'MOBILE', 'Test API Key - Inactive', false, CURRENT_TIMESTAMP);

-- Insert an expired Refresh Token for testing (R2)
INSERT INTO refresh_tokens (id, token, client_id, device_id, expires_at, revoked, created_at)
VALUES
    (1, 'rt_EXPIRED_TOKEN_1234567890', 'test_app', 'test-device-expired', CURRENT_TIMESTAMP - INTERVAL '1 hour', false, CURRENT_TIMESTAMP - INTERVAL '1 day');
