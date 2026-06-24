FROM caddy:2.11.4-builder AS builder

RUN xcaddy build \
    --with github.com/caddy-dns/cloudflare@v0.2.4 \
    --with github.com/mholt/caddy-ratelimit=github.com/mholt/caddy-ratelimit@16aecbb24beddc9095da2716fa8d3a30fa2dc8ea \
    --with github.com/corazawaf/coraza-caddy/v2@v2.5.0

FROM caddy:2.11.4-alpine
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
