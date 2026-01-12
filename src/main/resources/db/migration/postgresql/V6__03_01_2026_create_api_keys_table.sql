CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_value VARCHAR(60) NOT NULL UNIQUE,
    client_name VARCHAR(100) NOT NULL,
    user_identifier VARCHAR(100) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'SERVICE',
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_api_keys_key_value ON api_keys(key_value);
CREATE INDEX idx_api_keys_active ON api_keys(active);
CREATE INDEX idx_api_keys_client_name ON api_keys(client_name);
CREATE INDEX idx_api_keys_user_identifier ON api_keys(user_identifier);
CREATE INDEX idx_api_keys_platform ON api_keys(platform);