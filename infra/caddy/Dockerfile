FROM caddy:2.11.4-builder AS builder

RUN xcaddy build \
    --with github.com/caddy-dns/cloudflare@v0.2.4 \
    --with github.com/mholt/caddy-ratelimit=github.com/mholt/caddy-ratelimit@16aecbbcb8ca07dc1c671e263379606ff9493c55 \
    --with github.com/corazawaf/coraza-caddy/v2@v2.5.0

FROM caddy:2.11.4-alpine
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
