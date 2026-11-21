-- Widen api_keys.key_value to hold 64-character lowercase SHA-256 hex digests
ALTER TABLE api_keys
    ALTER COLUMN key_value TYPE VARCHAR(64);

-- Hash existing plaintext API keys (skip rows already stored as 64-char lowercase hex)
UPDATE api_keys
SET key_value = encode(sha256(convert_to(key_value, 'UTF8')), 'hex')
WHERE key_value !~ '^[0-9a-f]{64}$';

-- Hash existing plaintext refresh tokens (skip rows already stored as 64-char lowercase hex)
UPDATE refresh_tokens
SET token = encode(sha256(convert_to(token, 'UTF8')), 'hex')
WHERE token !~ '^[0-9a-f]{64}$';
