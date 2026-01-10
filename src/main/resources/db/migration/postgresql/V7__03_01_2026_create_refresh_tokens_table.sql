CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    api_key_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_token_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id)
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_api_key_id ON refresh_tokens(api_key_id);