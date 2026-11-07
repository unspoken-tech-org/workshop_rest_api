FROM caddy:2.11.7-builder@sha256:c4cd20eaa0b69112fd5d4f721ffc9671687e1d5d0b155c8620cf7546dbef94c0 AS builder

RUN xcaddy build \
    --with github.com/caddy-dns/cloudflare@v0.2.4 \
    --with github.com/mholt/caddy-ratelimit=github.com/mholt/caddy-ratelimit@16aecbbcb8ca07dc1c671e263379606ff9493c55 \
    --with github.com/corazawaf/coraza-caddy/v2@v2.5.0

FROM caddy:2.11.7-alpine@sha256:e70b164da81ef698b3ad19af799ea6086ce6d9e48ed4079eb9fe26f6fbf14ee5
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
