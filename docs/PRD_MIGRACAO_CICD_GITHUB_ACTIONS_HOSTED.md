# PRD — Migração de CI/CD para GitHub Actions Hosted Runners

**Projeto:** Workshop REST API
**Repositório:** `unspoken-tech-org/workshop_rest_api` (público)
**Data:** 20/06/2026
**Autor:** Time de Engenharia
**Status:** Em implementação — Fases 0.3, 0.11 e 0.13 (produção) concluídas; auditoria de SHAs concluída (versão v1.12); migração para Tailscale (v1.13); `deploy.yml` reescrito com Tailscale (v1.14); adendo sobre usuário dedicado CI/CD (v1.15); cloudflared-ssh removido do servidor (v1.16); Tailscale SSH habilitado, deploy-wrapper.sh e chaves SSH removidos (v1.17); deploy.yml validado end-to-end com GHCR pull+retag, concurrency e timeouts (v1.18); Docker layer cache (setup-buildx + type=gha) e correção de script injection (v1.19); harden-runner (StepSecurity) em modo audit (v1.20)
**Relacionado:** `SECURITY_ASSESSMENT.md` (item 2 — CI/CD)

---

## 0. Registro de Progresso da Implementação (v1.8 — 21/06/2026)

### Fase 0.7 — ⚠️ DESCONTINUADA (cloudflared-ssh removido do servidor)

> **Nota v1.16:** O `cloudflared-ssh.service` (tunnel SSH nativo) foi **removido do servidor** em 23/06/2026. O CI/CD utiliza **Tailscale** desde a v1.13; o acesso SSH manual agora é feito diretamente via Tailscale (sem necessidade de tunnel Cloudflare). Os containers Docker `cloudflared-prod` e `cloudflared-qa` permanecem ativos como **proxy reverso HTTP** para os endpoints (`api.eletroluk.com`, `api-qa.eletroluk.com`, `grafana.eletroluk.com`). A Fase 0.7 abaixo permanece documentada como **histórico** do que foi implementado e posteriormente descontinuado.

| Bloco | Tarefa | Status | Observação |
|-------|--------|--------|------------|
| **A** | 0.7.1a — Baixar `cloudflared` em `~/.local/bin/` na máquina de dev | ✅ | Versão 2026.5.1 instalada |
| **A** | 0.7.1b — Autenticar no Cloudflare via browser | ✅ | `~/.cloudflared/cert.pem` gerado |
| **A** | 0.7.1c — Criar tunnel `workshop-ssh` (prod) | ✅ | Tunnel ID: `d5bda623-2bb9-4a66-8f47-83f8b36dd7ee` |
| **A** | 0.7.1d — Criar tunnel `workshop-ssh-qa` | ❌ | Adiado — foco apenas em prod por enquanto |
| **A** | 0.7.2a — Rota DNS prod: `ssh.eletroluk.com` | ✅ | CNAME criado no Cloudflare |
| **A** | 0.7.2b — Rota DNS QA: `ssh.qa.eletroluk.com` | ❌ | Adiado |
| **A** | 0.7.3a — Ingress tunnel prod | ✅ | Configurado com `service: ssh://localhost:22` |
| **B** | 0.7.4 — Instalar `cloudflared` via APT no servidor | ✅ | Binário em `/usr/bin/cloudflared` |
| **B** | 0.7.5 — Criar grupo `cloudflared` + credentials files | ✅ | Credentials file em `/etc/cloudflared/` |
| **B** | 0.7.6a — Criar `/etc/cloudflared/config-ssh.yml` | ✅ | Tunnel prod configurado |
| **B** | 0.7.7a — Criar `cloudflared-ssh.service` | ✅ | Systemd service criado e habilitado |
| **B** | 0.7.8 — Habilitar serviço | ✅ | `systemctl enable --now cloudflared-ssh` |
| **B** | 0.7.9 — Validar serviço | ✅ | `active (running)`, 4 conexões com Edge |
| **C** | 0.7.10a — Criar Service Token `workshop-ci` | ✅ | Token criado no Zero Trust Dashboard |
| **C** | 0.7.10b — Criar Application "Workshop SSH Production" | ✅ | Self-hosted, policy `Service Auth` |
| **C** | 0.7.11 — Adicionar secrets ao GitHub | ⏳ | Pendente (Fase 0.11) |
| **C** | 0.7.12 — Smoke test ponta-a-ponta | ✅ | `SSH via Cloudflare Access OK` |
| **C** | 0.7.13 — Validar restart | ✅ | `Restart=always` funcionou; restart counter=222; reconexão Edge em 6s; SSH pós-restart OK |

### Problemas Encontrados e Resolvidos na Fase 0.7

1. **Bug na versão 2026.6.0 do `cloudflared`**: A versão 2026.6.0 tem um bug que ignora Service Tokens para SSH (issue [#1673](https://github.com/cloudflare/cloudflared/issues/1673)). **Solução:** downgrade para versão 2026.5.1.

2. **Erro `websocket: bad handshake`**: O `cloudflared access ssh` não conseguia autenticar via Service Token. **Causa raiz:** configuração do ingress do tunnel com `service: http_status:200` em vez de `service: ssh://localhost:22`. **Solução:** atualizar o ingress para `service: ssh://localhost:22` no `config-ssh.yml` e no dashboard Cloudflare.

3. **Aplicação Infrastructure incompatível**: A aplicação do tipo Infrastructure requer um target com IP IPv4/IPv6, mas o servidor está atrás de NAT sem IP público. **Decisão:** usar aplicação Self-hosted (funcional com a correção do ingress).

### Fase 0.3 — ⚠️ DESCONTINUADA (chaves SSH e deploy-wrapper.sh removidos)

> **Nota v1.17:** Com o Tailscale SSH habilitado (`tailscale up --ssh`), a autenticação SSH é feita via **identidade Tailscale** (OIDC), não via chaves SSH em `authorized_keys`. O Tailscale SSH **ignora** o arquivo `authorized_keys` — as flags `no-port-forwarding`, `no-X11-forwarding`, `no-agent-forwarding`, `no-pty` e o `command="/usr/local/bin/deploy-wrapper.sh"` nunca são avaliadas. Portanto: (1) o `deploy-wrapper.sh` foi **removido** do servidor; (2) as linhas das chaves públicas (`deploy_key_prod.pub`, `deploy_key_qa.pub`) foram **removidas** do `authorized_keys`; (3) os secrets `PROD_SSH_KEY` e `QA_SSH_KEY` no GitHub são **desnecessários** (podem ser removidos); (4) `PROD_SSH_HOST` e `PROD_SSH_PORT` também são obsoletos (o runner conecta via IP Tailscale diretamente, sem porta ou key). A Fase 0.3 abaixo permanece documentada como **histórico** do que foi implementado e posteriormente descontinuado.

| Item | Tarefa | Status | Observação |
|------|--------|--------|------------|
| 0.2 | Gerar chaves SSH Ed25519 (`deploy_key_prod`, `deploy_key_qa`) | ✅ | Chaves geradas com sucesso (mas não são mais usadas — Tailscale SSH autentica via OIDC) |
| 0.3a | Copiar `deploy_key_prod.pub` para `authorized_keys` no servidor | ❌ | **Removido** — Tailscale SSH ignora `authorized_keys` |
| 0.3b | Criar `/usr/local/bin/deploy-wrapper.sh` | ❌ | **Removido** do servidor — sem `command=` no `authorized_keys`, o wrapper nunca é invocado |
| 0.3c | Validar acesso SSH via Cloudflare Access | ✅ | Concluído na época; agora substituído por Tailscale SSH |

> **Nota:** Chave QA (`deploy_key_qa`) nunca foi adicionada ao servidor — obsoleta.

### Fase 0.11 — ⚠️ PARCIALMENTE OBSOLETA (v1.17 — secrets SSH desnecessários)

| Secret | Status | Observação |
|--------|--------|------------|
| ~~`PROD_SSH_HOST`~~ | ⚠️ OBSOLETO | Tailscale SSH conecta via IP direto (`TS_TAILSCALE_IP`) |
| ~~`PROD_SSH_PORT`~~ | ⚠️ OBSOLETO | Tailscale SSH usa porta 22 diretamente |
| `PROD_SSH_USER` | ✅ | `workshop` (ainda necessário para Tailscale SSH) |
| ~~`PROD_SSH_KEY`~~ | ⚠️ OBSOLETO | Tailscale SSH autentica via OIDC; `authorized_keys` ignorado |
| ~~`QA_SSH_HOST`~~ | ⚠️ OBSOLETO | idem `PROD_SSH_HOST` |
| ~~`QA_SSH_PORT`~~ | ⚠️ OBSOLETO | idem `PROD_SSH_PORT` |
| `QA_SSH_USER` | ✅ | `workshop` (ainda necessário) |
| ~~`QA_SSH_KEY`~~ | ⚠️ OBSOLETO | Nunca foi adicionada ao servidor; Tailscale SSH autentica via OIDC |

> **Nota v1.17:** 6 de 8 secrets SSH são obsoletos. Apenas `PROD_SSH_USER` e `QA_SSH_USER` continuam necessários. Os 6 secrets obsoletos podem ser removidos do GitHub para reduzir superfície de ataque.

### 0.14 — Migração para Tailscale (v1.13)

**Problema:** Após extensa testagem da abordagem Cloudflare Access SSH com Service Tokens, a conexão entre GitHub Actions hosted runners e o servidor mostrou-se **não confiável para CI/CD**. Múltiplas execuções de workflows falharam com erros como `"username is empty"`, `"websocket: bad handshake"` e `"error in libcrypto"`. A autenticação via Service Token do Cloudflare não encaminha o username corretamente do runner para o servidor, e o comportamento varia entre versões do `cloudflared`.

**Solução adotada:** **Tailscale** como meio de transporte entre runners hosted e o servidor.

| Aspecto | Detalhe |
|---------|---------|
| Mecanismo | `tailscale/github-action@v4` cria um nó **efêmero** na rede mesh do Tailscale no runner |
| Transporte | WireGuard (UDP, criptografia ponta-a-ponta) |
| Endereçamento | Runner recebe IP na faixa `100.x.x.x`; servidor também tem IP Tailscale (`TS_TAILSCALE_IP`) |
| Servidor | Permanece atrás de NAT; nenhuma porta pública necessária |
| Plano | **Tailscale Personal** — gratuito (1.000 minutos efêmeros/mês; suficiente para ~200 deploys) |
| CI/CD | O Tailscale é usado **exclusivamente** para CI/CD (deploy via SSH) |
| Acesso manual | **Tailscale SSH** — acesso manual ao servidor via IP Tailscale (sem tunnel Cloudflare) |

**Por que não Cloudflare Access SSH para CI/CD:**
- Service Token do Zero Trust não encaminha o username de forma consistente
- Comportamento instável entre versões do `cloudflared` (bug #1673 na v2026.6.0)
- Erros de autenticação intermitentes que não podem ser reproduzidos de forma confiável
- Necessidade de baixar e configurar `cloudflared` no runner a cada job (overhead + ponto de falha)

**Instalação no servidor:**
```bash
curl -fsSL https://tailscale.com/install.sh | sh
tailscale up --ssh
```

**Nota v1.16:** A Fase 0.7 (Cloudflare tunnel SSH) foi **descontinuada** — o `cloudflared-ssh.service` foi removido do servidor. O CI/CD e o acesso SSH manual agora utilizam **Tailscale**. Os containers Docker HTTP (`cloudflared-prod`, `cloudflared-qa`) permanecem ativos como proxy reverso para os endpoints web.

### Próximos Passos (Pendentes — v1.18)

1. **Remover secrets obsoletos do GitHub** — `PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_KEY`, `QA_SSH_HOST`, `QA_SSH_PORT`, `QA_SSH_KEY` (6 secrets obsoletos desde v1.17).

2. **Remover chaves SSH órfãos** — `SSH_PRIVATE_KEY`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSPHRASE` (4 secrets antigos, Fase 0.5).

3. ~~**Corrigir bugs no `deploy.yml`**~~ ✅ **CONCLUÍDO (v1.18)** — GHCR login via `export` no SSH (Padrão 1); `workflow_dispatch` ref corrigido; `packages:read` adicionado ao deploy job; pull por `sha-<full-commit>` + retag.

4. ~~**Testar `deploy.yml` end-to-end**~~ ✅ **CONCLUÍDO (v1.18)** — Tag `v1.3.0-rc-test` validada com sucesso (build 1m45s, deploy 1m4s, verify 19s, cleanup 16s).

5. **Migrar workflows restantes** — `deploy-qa.yml`, `deploy-gateway.yml`, `deploy-observability.yml` para hosted runner + Tailscale.

6. **Hardening do repositório** — Secret scanning, dependabot, CODEOWNERS, CODEQL, CI workflow (Fases 0.5 e 0.6).

### Configuração Atual do Servidor (v1.17)

```bash
# Tailscale
Status: ativo (systemd: tailscaled enabled)
SSH via Tailscale: habilitado (tailscale up --ssh)
IP Tailscale: TS_TAILSCALE_IP (100.x.x.x)
Autenticação SSH: Tailscale OIDC (identidade Tailscale)
authorized_keys: sem linhas de deploy (chaves SSH removidas — v1.17)
deploy-wrapper.sh: removido (v1.17)

# Containers Docker (proxy reverso HTTP)
cloudflared-prod: ativo (proxy reverso → api.eletroluk.com)
cloudflared-qa: ativo (proxy reverso → api-qa.eletroluk.com)
caddy-gateway: ativo (proxy reverso local)
```

---

## 1. Contexto e Problema

### 1.1 Estado Atual

O pipeline de CI/CD do Workshop REST API roda em **runner self-hosted** instalado no mesmo servidor que hospeda a aplicação. Os quatro workflows existentes (`.github/workflows/deploy.yml`, `deploy-gateway.yml`, `deploy-qa.yml`, `deploy-observability.yml`) executam comandos Docker diretamente, o que requer acesso ao Docker socket do host.

**Observação arquitetural:** o servidor de produção está **atrás de NAT em rede doméstica**, sem IP público. O acesso SSH ao servidor (tanto CI/CD quanto manual) é feito via **Tailscale** — o runner hosted executa `tailscale/github-action@v4` que cria um nó efêmero na rede mesh, conectando ao servidor via WireGuard (UDP). O domínio delegado ao Cloudflare é `eletroluk.com` (verificado em 20/06/2026 via `dig NS` — nameservers `konnor.ns.cloudflare.com` e `nola.ns.cloudflare.com`; subdomínios `api.eletroluk.com`, `api-qa.eletroluk.com`, `grafana.eletroluk.com` já em uso no `Caddyfile-gateway`). Cloudflare atua exclusivamente como **proxy reverso HTTP** para os endpoints web, via containers Docker (`cloudflared-prod`, `cloudflared-qa`).

**Mudança de transporte para CI/CD (v1.13):** após testes extensivos com Cloudflare Access SSH (Service Tokens), a abordagem mostrou-se não confiável para automação — erros intermitentes de autenticação (`username is empty`, `websocket: bad handshake`) impediram deploys consistentes. O transporte de CI/CD foi migrado para **Tailscale** (`tailscale/github-action@v4`), que cria nós efêmeros na rede mesh do Tailscale via WireGuard. O servidor permanece atrás de NAT; o acesso SSH manual também utiliza Tailscale (o `cloudflared-ssh.service` foi removido do servidor em v1.16).

### 1.2 Riscos Identificados

Conforme o `SECURITY_ASSESSMENT.md` (item 2, severidade **CRÍTICA**), o setup atual apresenta os seguintes vetores de ataque:

| # | Risco | Origem no `SECURITY_ASSESSMENT.md` |
|---|-------|-----------------------------------|
| 1 | **Persistência entre jobs** — workflow malicioso pode modificar `~/.bashrc`, instalar cron jobs, alterar binário do runner. Caso real: worm **Shai-Hulud (Nov/2025)** usava runners como C2. | Item 2, linha 62 |
| 2 | **Docker socket = root no host** — `docker run --privileged --pid=host -v /:/hostfs alpine chroot /hostfs` concede root no servidor. | Item 2, linha 63 |
| 3 | **CVE-2025-32955** — `disable-sudo` do Harden-Runner é bypassed via Docker. | Item 2, linha 64 |
| 4 | **Sem isolamento de rede** — runner está na rede doméstica, sem VLAN separada. | Item 2, linha 65 |
| 5 | **Secrets em arquivos no disco** — `.env` e chaves JWT persistem após deploy. | Item 11, linhas 752-754 |
| 6 | **Atende vetor de ataque via fork PR** — repo é público com `allow_forking: true`. | Item 1, linha 21 (recomendação) |

### 1.3 Estado Atual do Repositório (verificado via `gh api` em 16/06/2026)

#### 1.3.1 Configurações Gerais

| Propriedade | Valor |
|-------------|-------|
| Visibilidade | `public` |
| Stars / Forks | 1 / 0 |
| Default branch | `main` (protegida) |
| Permite fork PRs | `allow_forking: true` |
| Merge commit | ✅ Permitido |
| Rebase merge | ✅ Permitido |
| Squash merge | ✅ Permitido |
| Auto merge | ❌ Desabilitado |
| Delete branch on merge | ❌ Desabilitado |
| Web commit signoff | ❌ Não exigido |

#### 1.3.2 Segurança do Repositório

| Feature | Estado |
|---------|--------|
| Secret scanning | ❌ Desabilitado |
| Secret scanning push protection | ❌ Desabilitado |
| Dependabot security updates | ❌ Desabilitado |
| Dependabot auto-fix | ❌ Desabilitado |
| CodeQL / code scanning | ❌ Não configurado |
| Repository rulesets | ❌ Nenhum |

#### 1.3.3 Branch Protection (`main`)

| Regra | Estado |
|-------|--------|
| Branch protegido | ✅ Sim |
| Required PR reviews | 1 aprovador |
| Dismiss stale reviews | ✅ Sim |
| Require code owner reviews | ✅ Sim (mas sem CODEOWNERS — ineficaz) |
| Require last push approval | ❌ Não |
| Required signatures (GPG) | ❌ Não |
| Enforce admins | ❌ Não (admins bypassam) |
| Required linear history | ❌ Não |
| Allow force pushes | ⚠️ **Sim (risco)** |
| Allow deletions | ❌ Não |
| Required status checks | ❌ **Nenhum configurado** |
| Required conversation resolution | ❌ Não |
| Lock branch | ❌ Não |

#### 1.3.4 Ambientes e Runners

| Item | Estado |
|------|--------|
| Ambiente `production` | Existe, sem required reviewers, admins bypassam |
| Runner self-hosted | Online (`workshop-HP-EliteDesk-800-G3-DM-35W-Brazil`) |
| Actions permissions | `allowed_actions: all`, `sha_pinning_required: false` |
| CODEOWNERS | ❌ Não existe |
| dependabot.yml | ❌ Não existe |
| Branches antigas sem proteção | ~24 branches |

#### 1.3.5 Secrets Existentes (26 total)

| Secret | Criado em |
|--------|-----------|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | 2025-08-06 |
| `ADMIN_EMAIL` | 2026-01-10 |
| `JWT_RSA_PRIVATE_KEY_B64`, `JWT_RSA_PUBLIC_KEY_B64` | 2026-01-05 |
| `CF_API_TOKEN`, `CF_API_TOKEN_QA` | 2026-01-07, 2026-01-10 |
| `CLOUDFLARE_TUNNEL_TOKEN`, `CLOUDFLARE_TUNNEL_TOKEN_QA` | 2026-01-10 |
| `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | 2025-12-23 |
| `SSH_PRIVATE_KEY`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSPHRASE` | 2025-08-06 |
| `HOST_BIND_IP`, `LAN_CIDR` | 2025-08-10 |
| `QA_*` (7 secrets com prefixo QA) | 2026-01-10/11 |

### 1.4 Por que o runner self-hosted é vetor de ataque em repo público

> Citação literal do `SECURITY_ASSESSMENT.md` (item 2, linha 75):
> *"Nunca usar self-hosted runner em repositórios públicos — qualquer fork pode abrir PR e executar código no runner."*

> Citação do `SECURITY_ASSESSMENT.md` (item 1, solução de curto prazo, linha 76):
> *"Restringir `GITHUB_TOKEN` nos workflows com `permissions: {}`"*

---

## 2. Objetivo

Migrar o pipeline de CI/CD do Workshop REST API de **GitHub Actions self-hosted runner** (com Docker socket, persistente, no mesmo host da aplicação) para **GitHub Actions hosted runners** (efêmeros, isolados, gerenciados pelo GitHub), atendendo as soluções arquiteturais #4 e #5 do item 2 do `SECURITY_ASSESSMENT.md`.

### 2.1 Objetivos Específicos

- ✅ **OE1.** Eliminar acesso ao Docker socket do runner (solução #5 do `SECURITY_ASSESSMENT.md`).
- ✅ **OE2.** Garantir que cada job rode em VM descartável/efêmera (solução #4 do `SECURITY_ASSESSMENT.md`).
- ✅ **OE3.** Manter o pipeline funcional (build, test, deploy prod, deploy qa, deploy gateway, deploy observability) sem regressão.
- ✅ **OE4.** Preservar todos os secrets atuais (DB, JWT, Cloudflare, Grafana) sem necessidade de rotacionar.
- ✅ **OE5.** Não introduzir nova infraestrutura a ser mantida pelo time (sem VPS, sem cluster K8s).
- ✅ **OE6.** Custo operacional zero (repo público = GitHub-hosted runners são gratuitos e ilimitados).
- ✅ **OE7.** Endurecer o pipeline conforme o item 2 do `SECURITY_ASSESSMENT.md` (permissões mínimas, secrets protegidos, supply chain via SHA pins).

### 2.2 Não-Objetivos

- ❌ **NÃO** migrar o código para outra plataforma (continuará no GitHub).
- ❌ **NÃO** mudar a estratégia de deploy (continua sendo SSH no servidor de produção).
- ❌ **NÃO** introduzir K8s, Docker-in-Docker, ou nova infraestrutura.
- ❌ **NÃO** alterar o build da aplicação (continua Gradle + Spring Boot).
- ❌ **NÃO** rotacionar secrets existentes nesta fase (será tratado em ação futura, item 16 do `SECURITY_ASSESSMENT.md`).
- ❌ **NÃO** alterar os demais itens do `SECURITY_ASSESSMENT.md` (Caddy, Loki, JWT filter, etc.).

---

## 3. Solução Proposta

### 3.1 Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│ GitHub (repositório público: unspoken-tech-org/workshop_rest_api)   │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │ Workflows (.github/workflows/*.yml)                            │   │
│  │   runs-on: ubuntu-latest  ← ANTES: self-hosted                 │   │
│  │   permissions: contents: read                                  │   │
│  │   secrets: gerenciados pelo GitHub (sem .env no disco)         │   │
│  │   SSH dest: TS_TAILSCALE_IP (IP Tailscale do servidor)         │   │
│  └───────────────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ GitHub-hosted ephemeral VM (Azure)
                           │  - descartada após cada job
                           │  - sem Docker socket exposto
                           │  - sem persistência entre jobs
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Steps do Job                                                          │
│   1. actions/checkout@<SHA>                                          │
│   2. actions/setup-java@<SHA> (JDK 21)                              │
│   3. ./gradlew build + test (cache)                                  │
│   4. docker/build-push-action@<SHA> → GHCR                          │
│   5. tailscale/github-action@v4 ( nó efêmero na mesh Tailscale)     │
│   6. ssh/scp → TS_TAILSCALE_IP:22 (Tailscale SSH, auth via OIDC)   │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ WireGuard (UDP, criptografia ponta-a-ponta)
                           │ 100.x.x.x (runner) → TS_TAILSCALE_IP (servidor)
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Rede Tailscale (mesh criptografada, sem portas expostas)              │
│   - Runner recebe IP efêmero na faixa 100.x.x.x                    │
│   - Servidor com IP Tailscale fixo (TS_TAILSCALE_IP)               │
│   - Transporte WireGuard (UDP) — sem HTTPS intermediary            │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Servidor de Produção (rede doméstica, atrás de NAT)                   │
│                                                                       │
│  Tailscale (nativo):                                                  │
│    tailscaled (systemd) — IP Tailscale: TS_TAILSCALE_IP             │
│    Tailscale SSH habilitado (auth via OIDC)                          │
│    sshd escutando em 0.0.0.0:22                                      │
│                                                                       │
│  Containers Docker:                                                   │
│    caddy-gateway (proxy reverso HTTP/HTTPS)                          │
│    cloudflared-prod, cloudflared-qa (proxy reverso Cloudflare)       │
│                                                                       │
│  docker compose pull + up -d                                         │
└─────────────────────────────────────────────────────────────────────┘
```

> **Nota arquitetural (v1.17):** o acesso SSH do CI/CD e do acesso manual foi migrado para **Tailscale**. O runner hosted executa a action `tailscale/github-action@v4` que cria um nó efêmero na rede mesh Tailscale, conectando ao servidor via WireGuard (UDP). O servidor permanece atrás de NAT; nenhuma porta pública é necessária. O `cloudflared-ssh.service` foi removido do servidor (v1.16). **Com Tailscale SSH habilitado (v1.17):** autenticação é via identidade Tailscale (OIDC), não via chaves SSH. `authorized_keys` e `deploy-wrapper.sh` foram removidos. Os containers Docker `cloudflared-prod` e `cloudflared-qa` permanecem ativos exclusivamente como **proxy reverso HTTP** para os endpoints web (`api.eletroluk.com`, `api-qa.eletroluk.com`, `grafana.eletroluk.com`).

### 3.2 Decisões Técnicas

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Plataforma CI | GitHub Actions hosted runners | Resolve integralmente o item 2 do `SECURITY_ASSESSMENT.md`; custo dentro da quota gratuita mensal (repo público em jun/2026) |
| Runner | `ubuntu-latest` (GitHub-hosted) | Efêmero por design, sem Docker socket, sem persistência |
| Conectividade runner → servidor | **Tailscale (WireGuard via `tailscale/github-action@v4`)** | Servidor sem IP público; WireGuard é UDP (funciona atrás de NAT sem port mapping); Tailscale Personal gratuito (1.000 min efêmeros/mês); substituiu Cloudflare Access SSH por instabilidade na autenticação via Service Token |
| Autenticação CI/CD | **Tailscale SSH (OIDC via action)** | Tailscale autentica o nó efêmero na rede mesh e o usuário no servidor via identidade Tailscale. Chaves SSH em `authorized_keys` são ignoradas quando `tailscale up --ssh` está habilitado. Não há segunda camada de autenticação SSH (chave SSH foi removida — v1.17) |
| Acesso SSH manual (debugging) | **Tailscale SSH** — acesso direto via IP Tailscale | Sem tunnel; acesso manual via `workshop@TS_TAILSCALE_IP` |
| Hospedagem dos tunnels HTTP | Containers Docker (`cloudflared-prod`, `cloudflared-qa`) | Proxy reverso Cloudflare para endpoints web (`api.eletroluk.com`, `api-qa.eletroluk.com`, `grafana.eletroluk.com`) |
| Instalação do Tailscale no servidor | Script oficial (`tailscale.com/install.sh`) + `tailscale up --ssh` | Instalação simplificada; `--ssh` habilita SSH via Tailscale (alternative auth) |
| Build de imagem | `docker/setup-buildx-action@v3.11.0` + `docker/build-push-action@v6` (Buildx + GHA cache) | Constrói em buildkit isolado do runner hosted (sem expor o Docker socket do servidor de produção); cache GHA (`type=gha,mode=max`) reutiliza layers entre builds (v1.19) |
| Registry | GHCR (GitHub Container Registry) | Incluso no GitHub, sem custo extra, suporta tags imutáveis |
| Deploy | **SSH direto via `ssh`/`scp`** (sem action de terceiros) | Conexão via Tailscale SSH (`TS_TAILSCALE_IP:22`); autenticação feita pelo Tailscale (OIDC) — sem precisar de `-i key`. Segue o Padrão 1 da indústria: `GITHUB_TOKEN` passado via `export` no SSH para `docker login` no servidor (v1.18) |
| Versões de Actions | Pinned por SHA completo | Mitiga supply chain attack (tj-actions/changed-files, Mar/2025) |
| **Triggers de deploy** | `push: tags v*` (prod, gateway, observability) + `workflow_dispatch` (todos) | `branches` removido — deploys disparam apenas na criação de tags, não em commits em branches |
| **Secrets** | Migrados do self-hosted para GitHub Secrets (inalterados) + novos (2 SSH user + 1 Tailscale Auth Key, sem CF Access) | **Tailscale Auth Key** (`TS_AUTH_KEY`) compartilhado prod+QA (auth key, não OAuth client — v1.18); `TS_TAILSCALE_IP` é o IP do servidor na mesh; `GITHUB_TOKEN` usado para GHCR pull no servidor (export via SSH — Padrão 1 da indústria) |

### 3.3 Mapeamento Workflow por Workflow

#### 3.3.1 `deploy.yml` (Deploy Produção)

**Trigger (revisado v1.5):** `push: tags v[0-9]+.[0-9]+.[0-9]+*` em `release/*` **ou** `workflow_dispatch`

> **Branch de release:** `release/*` (ex.: `release/1.3.0`). Push de tag `v*` dentro de qualquer `release/*` dispara o deploy. Workflows que rodam em PRs para `release/*` usam o `ci.yml` (Fase 0.6) e **não** disparam deploy.

**Jobs atuais:** `build` → `deploy` → `verify` → `cleanup`

**Mudanças:**

| Job | Step atual (self-hosted) | Step novo (hosted) |
|-----|--------------------------|---------------------|
| build | `actions/checkout@v4` (tag) | `actions/checkout@v4` (tag) — **manter** |
| build | `rsync ./ → DEPLOY_DIR` | **Remover** (não há DEPLOY_DIR no runner) |
| build | `cat > .env <<EOF ...` | **Remover** (secrets não vão para .env) |
| build | `chmod 600 .env` | **Remover** |
| build | `mkdir -p config/keys` | **Remover** (chaves vão por SSH direto) |
| build | `echo $JWT_RSA_PRIVATE_KEY_B64 | base64 -d > key` | **Remover** |
| build | `docker compose build --no-cache` | **Substituir por** `docker/build-push-action@v6` (Buildx) → GHCR |
| build | `docker tag API_IMAGE:latest API_IMAGE:backup` | **Mover para step no servidor** (via SSH) |
| build | — | **Novo:** `docker/login-action` + push GHCR com tags `<git-tag>`, `sha-<full-commit>`, `latest` |
| build | — | **v1.19:** `docker/setup-buildx-action@v3.11.0` (SHA pinnado) habilita BuildKit para cache; `cache-from: type=gha` + `cache-to: type=gha,mode=max` no `docker/build-push-action` — reutiliza layers entre builds (10 GB limite por repo); todas as expressões `${{ }}` em `run:` blocks movidas para `env:` (correção de script injection) |
| todos | — | **v1.20:** `step-security/harden-runner@v2.12.1` (SHA pinnado) adicionado como primeiro step em todos os 4 jobs (build, deploy, verify, cleanup) com `egress-policy: audit` — monitora tráfego de rede, arquivos e processos sem bloquear; baseline será usada para criar allowlist e migrar para `block` |
| deploy | `docker compose up -d` no host | **Substituir por** `ssh`/`scp` direto via Tailscale SSH (`TS_TAILSCALE_IP:22`) — sem action de terceiros (v1.18) |
| deploy | `docker inspect ... health check` | **Mover para step SSH no servidor** |
| deploy | Rollback com backup image | **Mover para step SSH no servidor** — rollback via tag `:backup` retida localmente no servidor (a tag `latest` no GHCR é sobrescrita a cada push e **não pode** ser usada para reversão) |
| deploy | — | **Gate humana:** env `production` com required reviewers (aplicada já na Fase 1) |
| deploy | — | **v1.13:** step de setup Tailscale: `tailscale/github-action@v4` com `authkey` (TS_AUTH_KEY), `tags: tag:ci`, `ping: TS_TAILSCALE_IP` — cria nó efêmero na rede mesh |
| deploy | — | **v1.18:** GHCR login no servidor via `export GH_USER/GH_TOKEN` no SSH (Padrão 1 da indústria). Imagem puxada por `sha-<full-commit>` + retag para nome local do compose (Opção B). `packages: read` necessário no deploy job |
| verify | `pgbackrest check` | **Mover para step SSH no servidor** |
| verify | `docker compose ps` | **Mover para step SSH no servidor** |
| cleanup | `docker image prune -f` | **Mover para step SSH no servidor** |

**Imagem GHCR:** `ghcr.io/unspoken-tech-org/workshop_rest_api` com tags `<git-tag>`, `sha-<full-commit>`, `latest`.

**Pull no servidor (v1.18 — Opção B):** o deploy script puxa a imagem do GHCR por `sha-<full-commit>` (tag imutável) e retag para o nome local do compose (`workshop_rest_api-workshop_spring_app:latest`). O `docker compose up` usa a imagem local. Isso evita alterar o `docker-compose-production.yml` e funciona tanto para deploy remoto quanto para build local.

**Testes no CI release:** **não** roda `./gradlew test` por enquanto (paridade com workflow atual; CI em PRs roda testes via `ci.yml` da Fase 0.6). Meta futura: adicionar `./gradlew test` no job `build` da release.

**Validação (v1.19):** cache de Docker layers validado com sucesso via `workflow_dispatch` em 23/06/2026 — 2 execuções consecutivas no branch `release/1.3.0`; 2ª execução mostrou layers `CACHED` no step "Build and push image". Script injection corrigido em 3 steps (Prepare image tag, Show build info, Cleanup Summary) — todas as expressões `${{ }}` com valores user-controllable movidas para `env:` blocks.

#### 3.3.2 `deploy-gateway.yml` (Deploy Gateway Stack)

**Trigger (revisado v1.5):** `push: tags v[0-9]+.[0-9]+.[0-9]+*` em `release/*` **ou** `workflow_dispatch`

> Dispara **junto com o deploy prod** em toda tag. Reusa os secrets SSH do prod (`PROD_SSH_*`) — mesmo servidor, mesmo usuário `workshop`.

**Jobs atuais:** `deploy-gateway` (job único)

**Mudanças:**

| Step atual (self-hosted) | Step novo (hosted — v1.13) |
|--------------------------|---------------------|
| `actions/checkout@v4` | **Manter** |
| `rsync ./ → DEPLOY_DIR` | **Remover** |
| `cat > .env (CF_*, TUNNEL_TOKEN_*)` | **Remover** (env injetado via SSH) |
| `docker compose build caddy-gateway` | **Substituir por** `docker/build-push-action@v6` (Buildx) → GHCR |
| `docker compose up -d` | **Mover para step SSH no servidor** |
| `docker ps | grep caddy-gateway` (health check) | **Mover para step SSH no servidor** |
| — | **Gate humana:** env `production` com required reviewers (igual deploy prod) |
| — | **Backup:** `docker tag caddy-gateway:latest caddy-gateway:backup` (sem rollback automático; uso manual) |
| — | **v1.13:** step `tailscale/github-action@v4` cria nó efêmero na rede mesh antes do `ssh`/`scp` (mesmo Tailscale do deploy prod) |

**Imagem GHCR:** `ghcr.io/unspoken-tech-org/workshop_rest_api-caddy-gateway` (subpacote do owner) com tags `<git-tag>`, `sha-<7>`, `latest`.

#### 3.3.3 `deploy-qa.yml` (Deploy QA)

**Trigger (revisado v1.5):** `workflow_dispatch` **apenas** (manual)

> **Mudança crítica:** o workflow atual dispara em `push: branches release/1.2.0` (deploy automático a cada push na branch). Com a v1.5, QA fica **100% manual** — cada deploy QA exigirá ir em GitHub Actions → Run workflow. Branch `release/1.2.0` permanece como branch de trabalho manual; CI workflow (`ci.yml`) roda em PRs para essa branch mas **não** dispara deploy QA.

**Jobs atuais:** `build` → `deploy` → `validate`

**Mudanças:**

- Mesma reestruturação do `deploy.yml` (hosted + GHCR + `ssh`/`scp` via Tailscale).
- Sem rollback automático (decisão mantida do workflow atual).
- Sem pgBackRest (QA é ambiente descartável).
- Chave SSH **separada** da prod (`QA_SSH_KEY`).
- Imagem GHCR: tag `qa-<sha-7>` apenas (**sem `latest`**); reaproveita a imagem da prod (configuração vem de `.env` + compose).
- Sem gate humana de env (deploy manual via `workflow_dispatch` já exige clique).
- **Step de setup Tailscale (v1.13)**: `tailscale/github-action@v4` cria nó efêmero na rede mesh antes do `ssh`/`scp` (mesmo Tailscale do prod).
- **Concurrency (v1.7)**: `concurrency: { group: qa-deploy, cancel-in-progress: true }` — QA é descartável; cancels em curso liberam o servidor.
- Smoke test: `curl https://api-qa.eletroluk.com/api/health` retorna 200; `./gradlew integrationTests --args='--spring.profiles.active=qa'` passa localmente.

#### 3.3.4 `deploy-observability.yml` (Deploy Observability)

**Trigger (revisado v1.5):** `push: tags v[0-9]+.[0-9]+.[0-9]+*` em `release/*` **ou** `workflow_dispatch`

> **Mudança:** o `paths:` filter original (`docker-compose-observability.yml`, `infra/promtail-config-*.yaml`, `infra/grafana-provisioning/**`, `.github/workflows/deploy-observability.yml`) **removido** — filtros `paths:` só se aplicam a branches, não a tags; com o trigger revisado o filter perde propósito e foi simplificado.

**Jobs atuais:** `build` → `deploy` → `validate`

**Mudanças:**

- Mesma reestruturação do `deploy-gateway.yml` (hosted + `ssh`/`scp` via Tailscale).
- **Sem build de imagem no GHCR** — Loki/Grafana/Promtail são imagens Docker Hub oficiais. Não requer `packages: write` no `GITHUB_TOKEN`.
- Gate humana do env `production` com required reviewers (igual prod/gateway).
- Reusa `PROD_SSH_*` (mesmo servidor).
- Sem rollback automático.
- **v1.13:** step `tailscale/github-action@v4` cria nó efêmero na rede mesh antes do `ssh`/`scp` (mesmo Tailscale dos deploys prod/gateway — observability está no mesmo host de prod).
- Validação simplificada (item 4 da Fase 4):
  - **Gate de falha:** Promtail rodando (3 instâncias: prod, qa, gateway); labels em Loki (campo `environment`); datasource Loki no Grafana.
  - **Warning apenas:** ingestão de logs (pode dar falso negativo em janelas de manutenção).
- `workflow_dispatch` default `ref: main`.

### 3.4 Secrets Utilizados (apenas remapeamento, sem rotação)

| Secret | Workflow | Uso |
|--------|----------|-----|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL` | deploy.yml, deploy-qa.yml | Injetado como env via SSH no servidor |
| `JWT_RSA_PRIVATE_KEY_B64`, `JWT_RSA_PUBLIC_KEY_B64` | deploy.yml | Injetado como arquivo via SSH no servidor |
| `QA_DB_NAME`, `QA_DB_USERNAME`, `QA_DB_PASSWORD`, `QA_ADMIN_EMAIL` | deploy-qa.yml | Idem, escopo QA |
| `QA_JWT_RSA_PRIVATE_KEY_B64`, `QA_JWT_RSA_PUBLIC_KEY_B64` | deploy-qa.yml | Idem |
| `CF_API_TOKEN`, `CF_API_TOKEN_QA` | deploy-gateway.yml | Injetado como env via SSH |
| `CLOUDFLARE_TUNNEL_TOKEN`, `CLOUDFLARE_TUNNEL_TOKEN_QA` | deploy-gateway.yml | Tokens dos tunnels HTTP (containers Docker) |
| `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | deploy-observability.yml | Injetado como env via SSH |
| **`PROD_SSH_HOST`** | ~~deploy.yml, deploy-gateway.yml, deploy-observability.yml~~ | ⚠️ **OBSOLETO (v1.17)** — Tailscale SSH conecta via IP direto (`TS_TAILSCALE_IP`), sem host intermediário. Pode ser removido do GitHub. |
| **`PROD_SSH_PORT`** | ~~deploy.yml, deploy-gateway.yml, deploy-observability.yml~~ | ⚠️ **OBSOLETO (v1.17)** — Tailscale SSH usa porta 22 diretamente. Pode ser removido do GitHub. |
| **`PROD_SSH_USER`** | deploy.yml, deploy-gateway.yml, deploy-observability.yml | `workshop` (usuário SSH no servidor — ainda necessário para Tailscale SSH) |
| **`PROD_SSH_KEY`** | ~~deploy.yml, deploy-gateway.yml, deploy-observability.yml~~ | ⚠️ **OBSOLETO (v1.17)** — Tailscale SSH autentica via OIDC (identidade Tailscale), não via chave SSH. `authorized_keys` é ignorado quando `tailscale up --ssh` está habilitado. Pode ser removido do GitHub. |
| **`QA_SSH_HOST`** | ~~deploy-qa.yml~~ | ⚠️ **OBSOLETO (v1.17)** — idem `PROD_SSH_HOST`. |
| **`QA_SSH_PORT`** | ~~deploy-qa.yml~~ | ⚠️ **OBSOLETO (v1.17)** — idem `PROD_SSH_PORT`. |
| **`QA_SSH_USER`** | deploy-qa.yml | `workshop` (ainda necessário) |
| **`QA_SSH_KEY`** | ~~deploy-qa.yml~~ | ⚠️ **OBSOLETO (v1.17)** — idem `PROD_SSH_KEY`. Nunca foi adicionada ao servidor. |
| **`TS_AUTH_KEY` (NOVO, v1.13, revisado v1.18)** | deploy.yml, deploy-gateway.yml, deploy-observability.yml, deploy-qa.yml | Auth key do Tailscale (usado pela `tailscale/github-action@v4` para autenticar nós efêmeros na rede mesh; expira em 90 dias — renovação manual). **Nota v1.18:** era documentado como OAuth Client (v1.13); agora é auth key (revisado após migração de OAuth para auth key no Tailscale Admin) |
| **`TS_TAILSCALE_IP` (NOVO, v1.13)** | deploy.yml, deploy-gateway.yml, deploy-observability.yml, deploy-qa.yml | IP Tailscale do servidor na rede mesh (ex.: `100.x.x.x`) — destino do `ssh`/`scp` |

> **Total de secrets novos (v1.13, revisado v1.18):** Originalmente **5** (4 SSH prod + 3 Tailscale − 2 CF Access antigos). Com v1.17, os 4 secrets SSH são **obsoletos**. Com v1.18, `TS_AUTH_KEY` é auth key (não OAuth). Secret efetivamente necessário: **`PROD_SSH_USER`** (1) + **`QA_SSH_USER`** (1) + **`TS_AUTH_KEY`** (1) + **`TS_TAILSCALE_IP`** (1) = **4 secrets novos**. `GITHUB_TOKEN` (secreto automático) é usado para GHCR pull no servidor via `export` no SSH (Padrão 1 da indústria) — não requer secret extra. Os secrets `CF_ACCESS_CLIENT_ID` e `CF_ACCESS_CLIENT_SECRET` foram **removidos** (Service Token do Cloudflare Zero Trust não é mais necessário — o cloudflared-ssh.service foi descontinuado). Os secrets `CF_API_TOKEN`, `CF_API_TOKEN_QA`, `CLOUDFLARE_TUNNEL_TOKEN` e `CLOUDFLARE_TUNNEL_TOKEN_QA` permanecem para os containers Docker HTTP (`cloudflared-prod`, `cloudflared-qa`).

> **Decisão sobre `PROD_SSH_HOST` (v1.13 → obsoleto v1.17):** originalmente, `PROD_SSH_HOST` mudou de `localhost` (antigo, para listener do `cloudflared access ssh`) para o **IP Tailscale do servidor** (`TS_TAILSCALE_IP`). Com Tailscale SSH habilitado (v1.17), o runner conecta diretamente em `TS_TAILSCALE_IP:22` — não há need de `PROD_SSH_HOST` ou `PROD_SSH_PORT` separados.

> **Nota sobre SSH (v1.13 → descontinuado v1.17):** As chaves SSH (`*_SSH_KEY`) foram originalmente geradas como chaves dedicadas e adicionadas ao `~/.ssh/authorized_keys` do usuário `workshop` no servidor com `command="/usr/local/bin/deploy-wrapper.sh"` e flags `no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty` (Fase 0.3). **Com Tailscale SSH habilitado (v1.17):** (1) o Tailscale SSH autentica via identidade Tailscale (OIDC), **ignorando** `authorized_keys` completamente; (2) o `deploy-wrapper.sh` foi removido do servidor; (3) as linhas das chaves públicas foram removidas do `authorized_keys`; (4) os secrets `PROD_SSH_KEY` e `QA_SSH_KEY` são desnecessários. A autenticação CI/CD agora depende 100% do Tailscale ACL.

### 3.5 Permissões Mínimas (`permissions:`)

Conforme `SECURITY_ASSESSMENT.md` (item 2, solução de curto prazo #2, linha 76):
> *"Restringir `GITHUB_TOKEN` nos workflows com `permissions: {}` e adicionar apenas o necessário."*

Todos os workflows terão, no nível do workflow:

```yaml
permissions:
  contents: read   # Para checkout
```

**Exceção (revisada v1.18):** os jobs `build` que fazem push para o GHCR (`deploy.yml` e `deploy-gateway.yml`) requerem `packages: write`. Os jobs `deploy` que fazem pull do GHCR requerem `packages: read` (v1.18 — necessário para `docker pull` no servidor via `GITHUB_TOKEN`). Esses jobs declaram permissões elevadas **apenas no escopo do próprio job**, mantendo o restante do workflow com `contents: read` apenas:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write   # Necessário para push no GHCR
    steps: ...

  deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: read    # Necessário para pull no GHCR (v1.18)
    steps: ...
```

Workflows que não fazem push de imagem (`deploy-observability.yml`, `deploy-qa.yml` reaproveita a imagem da prod) usam `contents: read` em todos os jobs.

Nenhum workflow terá `id-token: write` ou `pages: write`. O deploy é feito exclusivamente via SSH, sem uso de OIDC federation. O `GITHUB_TOKEN` é descartado ao fim de cada job hosted.

### 3.6 Pinning de Actions por SHA

Para mitigar supply chain attack (vide `SECURITY_ASSESSMENT.md` item 2, referência ao tj-actions):

```yaml
# ❌ Errado (vulnerável a tag hijack)
- uses: actions/checkout@v4

# ✅ Correto (pinned por SHA imutável)
- uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11 # v4.1.1
```

Actions a pinar (com SHA da última release estável conhecida em 16/06/2026):

| Action | SHA | Versão |
|--------|-----|--------|
| `actions/checkout` | `b4ffde65f46336ab88eb53be808477a3936bae11` | v4.1.1 |
| `actions/setup-java` | `9704b39bf258b59bc04b50fa2dd55e9ed76b47a8` | v4.1.0 |
| `actions/cache` | `1bd1e32a3bdc45362d1e726936510720a7c30a57` | v4.2.0 |
| `docker/build-push-action` | `c382f710d39a5bb4e430307530a720f50c2d3318` | v6.0.0 |
| `docker/setup-buildx-action` | `18ce135bb5112fa8ce4ed6c17ab05699d7f3a5e0` | v3.11.0 |
| `step-security/harden-runner` | `002fdce3c6a235733a90a27c80493a3241e56863` | v2.12.1 |
| `docker/login-action` | `0d4c9c5ea7693da7b068278f7b52bda2a190a446` | v3.2.0 |
| `tailscale/github-action` | `306e68a486fd2350f2bfc3b19fcd143891a4a2d8` | v4.1.2 |

> **Nota sobre SSH (v1.18):** o deploy usa `ssh`/`scp` direto (sem action de terceiros). O `tailscale/github-action` cria o nó efêmero na rede mesh; o `ssh`/`scp` conecta diretamente via Tailscale SSH (OIDC). Não há dependência de `appleboy/ssh-action` ou qualquer action de SSH.

#### 3.6.1 Como Encontrar e Validar SHAs de Actions (v1.12)

A tabela da seção 3.6 lista SHAs de actions pinadas por commit imutável, mas SHAs podem **divergir** quando actions são re-taggadas. Há 3 métodos para encontrar o SHA correto:

**Método 1 — GitHub CLI (recomendado):**

```bash
# Obter SHA completo de uma tag
gh api repos/{owner}/{repo}/git/refs/tags/{TAG} --jq '.object.sha'

# Exemplo: actions/checkout v4.1.1
gh api repos/actions/checkout/git/refs/tags/v4.1.1 --jq '.object.sha'
# Saída: b4ffde65f46336ab88eb53be808477a3936bae11
```

**Método 2 — cURL + jq (sem GitHub CLI):**

```bash
curl -fsSL "https://api.github.com/repos/{owner}/{repo}/git/refs/tags/{TAG}" | jq -r '.object.sha'
```

**Método 3 — Dependabot (automático):**

Configurar `.github/dependabot.yml` com ecossistema `github-actions`. Dependabot abre PRs semanais com versões atualizadas, e cada PR mostra o SHA correto no diff.

**Como validar um SHA existente:**

```bash
# Verifica se o SHA existe no repo
gh api repos/{owner}/{repo}/commits/{SHA} --jq '.sha'
# Se 404: SHA inválido. Se retornar mesmo SHA: válido.
```

**Auditoria em lote:**

O projeto inclui o script `scripts/audit-action-shas.sh` (Tarefa 0.6.1) que audita todos os SHAs de um workflow de uma vez:

```bash
./scripts/audit-action-shas.sh .github/workflows/deploy.yml
```

**Quando auditar:**
- Antes de cada release (tag `v*`)
- Após merge de PR do Dependabot
- Anualmente, para confirmar que SHAs não foram comprometidos

> **Nota sobre `github/codeql-action` (v1.7):** o documento v1.6 usava o mesmo SHA para `init`, `autobuild` e `analyze` — **corrigido em v1.7**. Cada sub-action tem seu próprio SHA. Auditoria a ser feita na Fase 0.6 antes do merge (consultar `https://github.com/github/codeql-action/releases` para os SHAs das sub-actions na versão estável de jun/2026).

> **Pinning do `cloudflared` no runner (v1.13 — obsoleto para CI/CD):** a seção anterior documentava o pinning do binário `cloudflared` no runner hosted (versão + SHA256). Com a migração para Tailscale (v1.13), o CI/CD **não baixa mais `cloudflared` no runner** — a action `tailscale/github-action@v4` cria o nó efêmero na rede mesh. O pinning do `cloudflared` continua relevante apenas para o servidor (`apt upgrade` controlado). O SHA do `tailscale/github-action` deve ser pinado por SHA imutável (seção 3.6).

---

## 4. Plano de Implementação por Fases

### Fase 0 — Preparação (30 min)

**Objetivo (revisado v1.18):** Mapear o estado atual e preparar o repositório para a migração. ~~Gerar SSH keys dedicadas~~ (obsoleto v1.17 — Tailscale SSH autentica via OIDC).

| # | Tarefa (revisada v1.7) | Owner | Entregável |
|---|--------|-------|------------|
| 0.1 | Confirmar triggers: **`release/*` é a branch de release**; push de tag `v*` em `release/*` dispara prod/gateway/observability; QA é 100% manual via `workflow_dispatch`; branch `release/1.2.0` permanece como branch de trabalho manual (CI workflow roda em PRs mas não dispara deploy QA) | Eng. | Mapeamento de triggers documentado |
| 0.2 | ~~Gerar **2 pares de chaves SSH Ed25519** dedicados para deploy~~ | ⚠️ | **OBSOLETO (v1.17):** Tailscale SSH autentica via OIDC; chaves SSH não são usadas. |
| 0.3 | ~~Adicionar `deploy_key_prod.pub` e `deploy_key_qa.pub` ao `~/.ssh/authorized_keys` com `command="..."` e flags~~ | ⚠️ | **OBSOLETO (v1.17):** Tailscale SSH ignora `authorized_keys`; `deploy-wrapper.sh` removido. |
| 0.4 | Habilitar features de segurança do GitHub (item 5 deste PRD) | Eng. | Secret scanning, dependabot, push protection, fork approval |
| 0.5 | **(v1.7 — NEW, atualizado v1.17)** Inventariar secrets para remoção: (1) **4 secrets órfãos antigos** — `SSH_PRIVATE_KEY`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSPHRASE` (criados em 2025-08-06). (2) **6 secrets SSH obsoletos (v1.17)** — `PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_KEY`, `QA_SSH_HOST`, `QA_SSH_PORT`, `QA_SSH_KEY` (Tailscale SSH autentica via OIDC, sem chaves SSH). **Total: 10 secrets a remover.** Confirmar que nenhum workflow ativo os referencia antes de remover (`grep` em `.github/workflows/*.yml`) | Eng. | Lista de 10 secrets a remover |

**Comando de geração (referência):**
```bash
ssh-keygen -t ed25519 -a 100 -C "workshop-deploy-prod@<host>" -f deploy_key_prod
ssh-keygen -t ed25519 -a 100 -C "workshop-deploy-qa@<host>" -f deploy_key_qa
```

**Validação:** `ssh -i deploy_key_prod workshop@<HOST> 'echo OK'` retorna "OK" (e idem para `deploy_key_qa`).

**Template do `authorized_keys` com `command="..."` (referência — OBSOLETO v1.17):**

> **Nota v1.17:** o template abaixo é histórico. Com Tailscale SSH habilitado, `authorized_keys` é ignorado. Chaves SSH e `deploy-wrapper.sh` foram removidos do servidor.

```
command="/usr/local/bin/deploy-wrapper.sh",no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAAC3... workshop-deploy-prod
command="/usr/local/bin/deploy-wrapper.sh",no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAAC3... workshop-deploy-qa
```

**Template de `/usr/local/bin/deploy-wrapper.sh` (referência — OBSOLETO v1.17):**

> **Nota v1.17:** o script abaixo é histórico. `deploy-wrapper.sh` foi removido do servidor.

```bash
#!/usr/bin/env bash
# Recebe o comando via $SSH_ORIGINAL_COMMAND
# Whitelist de comandos permitidos
CMD="$SSH_ORIGINAL_COMMAND"
LOG="/var/log/deploy-wrapper.log"

echo "[$(date -Is)] user=$(whoami) ssh_keyfp=$(ssh-keygen -lf /dev/stdin <<<"$SSH_KEY_FP" 2>/dev/null) cmd=$CMD" >> "$LOG"

# Whitelist: apenas comandos que começam com os prefixos abaixo
case "$CMD" in
  "docker compose "*|"docker system "*|"docker image "*|"docker rm "*|"docker ps "*|"docker logs "*)
    exec /bin/bash -c "$CMD"
    ;;
  "cd ~/workshop && "*|"cd /home/workshop && "*)
    exec /bin/bash -c "$CMD"
    ;;
  "echo OK"|"whoami"|"date"|"uptime")
    exec /bin/bash -c "$CMD"
    ;;
  "")
    exec /bin/bash --login
    ;;
  *)
    echo "Comando bloqueado pelo deploy-wrapper: $CMD" >&2
    exit 1
    ;;
esac
```

> **Observação:** o wrapper acima é uma referência; ajustar a whitelist conforme os comandos reais dos workflows. Manter o script **fora** do PATH do usuário comum (`/usr/local/bin/`) e com `chmod 755` e `chown root:root`.

### Fase 0.7 — Instalar `cloudflared-ssh` nativo e configurar tunnel HTTP + Cloudflare Zero Trust (1,5h)

**Objetivo (revisado v1.13):** Instalar o binário `cloudflared` nativamente no servidor (systemd), criar **2 tunnels HTTP dedicados** para acesso SSH **manual** ao servidor (`workshop-ssh` e `workshop-ssh-qa`), configurar **Cloudflare Zero Trust Free** com **Service Token** para autenticação no Edge, e validar a conectividade ponta-a-ponta. O transporte é **HTTPS** (não TCP), portanto **não exige Cloudflare Pro+** — funciona no Free tier. O servidor continua atrás de NAT em rede doméstica. **Nota v1.13:** o CI/CD (GitHub Actions hosted runners) foi migrado para **Tailscale** — este tunnel é usado exclusivamente para acesso SSH manual (debugging, manutenção).

#### Bloco A — Provisionar `cloudflared` na máquina de desenvolvimento e criar tunnels (decisão v1.5 mantida: opção A)

| # | Tarefa | Comando/ação | Dependência |
|---|--------|--------------|-------------|
| 0.7.0a | **Pré-requisito:** confirmar que `eletroluk.com` está no Cloudflare Free (verificado em 20/06/2026: NS = `konnor.ns.cloudflare.com`, `nola.ns.cloudflare.com`) e que a organização Zero Trust está habilitada em `https://one.dash.cloudflare.com/` (criar se não existir — free, até 50 usuários) | Verificação manual no dashboard | — |
| 0.7.0b | **Pré-requisito:** confirmar que o servidor já roda `cloudflared` em Docker para os tunnels HTTP do gateway (`cloudflared-prod`, `cloudflared-qa` em `docker-compose-gateway.yml`) — o usuário confirmou em 20/06/2026. Reaproveitar a mesma conta/org Cloudflare | Verificação manual no servidor | — |
| 0.7.1a | Baixar binário `cloudflared` em `~/.local/bin/` na máquina de dev (sem Docker, sem root) | `curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o ~/.local/bin/cloudflared && chmod +x ~/.local/bin/cloudflared && ~/.local/bin/cloudflared --version` | — |
| 0.7.1b | Autenticar no Cloudflare via browser | `~/.local/bin/cloudflared tunnel login` (abre browser → login Cloudflare → seleciona domínio `eletroluk.com`; grava `~/.cloudflared/cert.pem`) | 0.7.0a |
| 0.7.1c | Criar tunnel `workshop-ssh` (produção) | `~/.local/bin/cloudflared tunnel create workshop-ssh` (gera `<TUNNEL_ID_PROD>.json` em `~/.cloudflared/`) | 0.7.1b |
| 0.7.1d | Criar tunnel `workshop-ssh-qa` | `~/.local/bin/cloudflared tunnel create workshop-ssh-qa` (gera `<TUNNEL_ID_QA>.json` em `~/.cloudflared/`) | 0.7.1b |
| 0.7.2a | Adicionar rota DNS prod: `cloudflared tunnel route dns workshop-ssh ssh.eletroluk.com` (cria CNAME no Cloudflare; subdomínio dedicado) | 0.7.1c |
| 0.7.2b | Adicionar rota DNS QA: `cloudflared tunnel route dns workshop-ssh-qa ssh.qa.eletroluk.com` | 0.7.1d |
| 0.7.3a | Configurar ingress do tunnel prod (Dashboard ou API): `service: ssh://localhost:22` para roteamento SSH + fallback `http_status:404`. **CORREÇÃO v1.8:** usar `ssh://localhost:22` (não `http_status:200`) para que o `cloudflared access ssh` consiga tunelar o SSH para o servidor | 0.7.2a |
| 0.7.3b | Configurar ingress do tunnel QA (idêntico) | 0.7.2b |

> **Observação:** o `cert.pem` gerado em 0.7.1b é usado apenas para o subconjunto de operações administrativas (criar tunnel + rota DNS). **Não é um secret sensível de produção** — escopo limitado, gerado localmente na máquina do mantenedor, e pode ser revogado/regerado a qualquer momento.

#### Bloco B — Instalar `cloudflared` no servidor e configurar systemd

| # | Tarefa | Comando/ação | Dependência |
|---|--------|--------------|-------------|
| 0.7.4 | Instalar `cloudflared` via APT oficial do Cloudflare (repo `noble main` para Ubuntu 24.04); caminho do binário: `/usr/bin/cloudflared` | adicionar repo + `apt install cloudflared` | 0.7.0b |
| 0.7.5 | Criar grupo `cloudflared` (`groupadd --system cloudflared`) e adicionar `nobody` (`usermod -aG cloudflared nobody`); instalar **2 credentials files** em `/etc/cloudflared/<TUNNEL_ID_PROD>.json` e `<TUNNEL_ID_QA>.json` (transferidos via `scp` da máquina de dev) com `chown root:cloudflared` e `chmod 640` | 0.7.1c, 0.7.1d |
| 0.7.6a | Criar `/etc/cloudflared/config-ssh.yml` para tunnel prod (conteúdo abaixo) | 0.7.3a, 0.7.5 |
| 0.7.6b | Criar `/etc/cloudflared/config-ssh-qa.yml` para tunnel QA (idêntico, com `<TUNNEL_ID_QA>`) | 0.7.3b, 0.7.5 |
| 0.7.7a | Criar `/etc/systemd/system/cloudflared-ssh.service` (conteúdo abaixo) | 0.7.4 |
| 0.7.7b | Criar `/etc/systemd/system/cloudflared-ssh-qa.service` (mesmo template, com `config-ssh-qa.yml` e `workshop-ssh-qa`) | 0.7.4 |
| 0.7.8 | `systemctl daemon-reload && systemctl enable --now cloudflared-ssh cloudflared-ssh-qa` | 0.7.7a, 0.7.7b |
| 0.7.9 | Validar: `systemctl status cloudflared-ssh cloudflared-ssh-qa` → ambos `active (running)`; `journalctl -u cloudflared-ssh -n 50` mostra conexão HEALTHY com Cloudflare Edge; `curl -I https://ssh.eletroluk.com` → resposta 200/404/304 do Edge (prova que o tunnel HTTP está respondendo) | 0.7.8 |

#### Bloco C — Cloudflare Zero Trust, Service Token, secrets e validação final

| # | Tarefa (revisada v1.7) | Comando/ação | Dependência |
|---|--------|--------------|-------------|
| 0.7.10a | Criar **Service Token** `workshop-ci` (par `client_id` + `client_secret`) em `https://one.dash.cloudflare.com/` → **Access → Service Auth → Create Service Token** | — |
| 0.7.10b | Criar **Self-hosted Application** "Workshop SSH Production" → Application domain: `ssh.eletroluk.com` → Session duration: 24h → Service Auth: ON → Policy "CI Deploy" → Action: Allow → Include → **Service Auth** = `workshop-ci` | 0.7.10a |
| 0.7.10c | Criar **Self-hosted Application** "Workshop SSH QA" → Application domain: `ssh.qa.eletroluk.com` → mesma policy `workshop-ci` (par único compartilhado prod+QA) | 0.7.10a |
| 0.7.11 | Adicionar **10 secrets novos** ao GitHub: `PROD_SSH_HOST=localhost`, `PROD_SSH_PORT=2222`, `PROD_SSH_USER=workshop`, `PROD_SSH_KEY` (conteúdo de `deploy_key_prod`), `QA_SSH_HOST=localhost`, `QA_SSH_PORT=2223`, `QA_SSH_USER=workshop`, `QA_SSH_KEY` (conteúdo de `deploy_key_qa`), `CF_ACCESS_CLIENT_ID` (do Service Token), `CF_ACCESS_CLIENT_SECRET` (do Service Token). **Não** adicionar `CF_TUNNEL_TOKEN_SSH` — credentials files permanecem apenas no servidor; o runner se autentica via Service Token, não via credentials file. **Nota v1.13:** `CF_ACCESS_CLIENT_ID` e `CF_ACCESS_CLIENT_SECRET` não são mais usados para CI/CD (migrado para Tailscale); podem ser revogados no dashboard se não houver outro uso | `gh secret set` ou Web UI | 0.7.10a |
| 0.7.12 | Smoke test ponta-a-ponta na máquina de dev (validação manual do tunnel — **não usado para CI/CD**): em background, `cloudflared access ssh --hostname ssh.eletroluk.com --listener localhost:2222 --service-token-id "$CFID" --service-token-secret "$CFSEC"`; aguardar `nc -z localhost 2222`; em outro shell, `ssh -p 2222 -i deploy_key_prod workshop@localhost` → prompt do servidor (sem pedir senha) | 0.7.9, 0.7.11 |
| 0.7.13 | Validar restart: `sudo systemctl kill cloudflared-ssh` (SIGTERM) → aguardar 5-10s → `systemctl status cloudflared-ssh` deve estar `active (running)` (graças ao `Restart=always`) → `journalctl -u cloudflared-ssh --since "30 seconds ago"` mostra restart | 0.7.9 |

**Conteúdo de `/etc/systemd/system/cloudflared-ssh.service`:**

```ini
[Unit]
Description=Cloudflare Tunnel (SSH) for Workshop deploys - production
Documentation=https://github.com/cloudflare/cloudflared
After=network-online.target nss-lookup.target
Wants=network-online.target

[Service]
Type=simple
User=nobody
Group=cloudflared
ExecStart=/usr/bin/cloudflared --no-autoupdate tunnel --config /etc/cloudflared/config-ssh.yml run workshop-ssh
Restart=always
RestartSec=5
WatchdogSec=60
LimitNOFILE=65536
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ReadWritePaths=/etc/cloudflared

[Install]
WantedBy=multi-user.target
```

> **Variação para `cloudflared-ssh-qa.service`:** trocar `config-ssh.yml` por `config-ssh-qa.yml` e `workshop-ssh` por `workshop-ssh-qa`.

**Conteúdo de `/etc/cloudflared/config-ssh.yml`:**

```yaml
tunnel: <TUNNEL_ID_PROD>
credentials-file: /etc/cloudflared/<TUNNEL_ID_PROD>.json

ingress:
  - hostname: ssh.eletroluk.com
    service: ssh://localhost:22
  - service: http_status:404
```

> **CORREÇÃO v1.8:** o ingress deve usar `service: ssh://localhost:22` (não `http_status:200`) para que o `cloudflared access ssh` no runner consiga tunelar o SSH para o servidor. O `service: http_status:200` apenas expõe o hostname via HTTPS, mas não roteia tráfego SSH. Com `ssh://localhost:22`, o Edge encaminha o tráfego SSH para o `cloudflared-ssh.service` no servidor, que por sua vez conecta ao `sshd` local em `localhost:22`.

**Justificativa das diretivas de hardening do systemd:**
- `User=nobody` + `Group=cloudflared` — binário roda sem privilégios; grupo dedicado permite ler o credentials file (`chmod 640`) sem expor a outros serviços
- `Restart=always` + `RestartSec=5` — recupera de crash em até 5 segundos
- `WatchdogSec=60` — systemd mata e reinicia o serviço se ele travar (sem heartbeat)
- `NoNewPrivileges=true` + `ProtectSystem=strict` + `ProtectHome=true` + `PrivateTmp=true` — namespaces isolados; serviço não consegue modificar sistema, ler `$HOME` de outros usuários, nem escalar privilégios
- `ReadWritePaths=/etc/cloudflared` — única escrita permitida: a pasta do credentials file
- `--no-autoupdate` — atualizações do `cloudflared` são controladas manualmente via `apt upgrade`, evitando que um update automático quebre compatibilidade

**Por que `cloudflared-ssh` é nativo (não container):** SSH é infraestrutura crítica de deploy. Se o Docker daemon crashar (OOM, bug, disco cheio), os containers `cloudflared-prod` e `cloudflared-qa` caem, mas o `cloudflared-ssh` nativo continua disponível — incluindo a capacidade de reiniciar o próprio Docker. Os tunnels HTTP permanecem em Docker por decisão de escopo (não refatorar o que funciona); a mistura é documentada no `AGENTS.md` quando este PRD for implementado.

**Por que HTTPS + Zero Trust (e não TCP puro):** Cloudflare Tunnel TCP exige plano Pro+ (US$ 5+/mês). Como o projeto roda no **Free tier** (verificado: `eletroluk.com` está em conta Free), o transporte via **HTTP tunnel + Zero Trust com Service Token** é a alternativa gratuita. O Service Token é **revogável** no dashboard, e a chave SSH continua sendo a autenticação final no `sshd` (defesa em profundidade). **Nota v1.13:** essa configuração permanece para acesso SSH **manual** — o CI/CD foi migrado para Tailscale por instabilidade dos Service Tokens em automação.

### Fase 0.6.1 — Workflow `build-image.yml` (camada L2 — build isolado, sem deploy) (15 min)

**Objetivo:** Isolar o build de imagem do deploy. Permite validar Buildx + GHCR antes de envolver SSH. Roda em runner hosted, sem efeito colateral no servidor. Complementa o `ci.yml` (camada L1) que valida testes unitários, mas não constrói imagem.

| # | Tarefa (v1.6) | Dependência |
|---|--------|-------------|
| 0.6.1.1 | Criar `.github/workflows/build-image.yml` com `runs-on: ubuntu-latest`, `permissions: { contents: read, packages: write }`, triggers: `workflow_dispatch` (com input `ref`) + `push: branches: [release/*]` | Fase 0.6 (`ci.yml` já em vigor) |
| 0.6.1.2 | Adicionar conteúdo sugerido em **seção 5.2.7** (build + push GHCR + smoke test efêmero + validação de JDK 21) | — |
| 0.6.1.3 | Disparar manualmente via `gh workflow run build-image.yml -f ref=release/1.3.0` e validar que as tags `sha-<7>` e `dev-<ref>` aparecem no GHCR | 0.6.1.2 |
| 0.6.1.4 | Documentar no `AGENTS.md` como disparar build isolado e interpretar resultado | 0.6.1.3 |

**Validações automáticas no job (gate de sucesso):**
- `docker buildx imagetools inspect ghcr.io/.../workshop_rest_api:sha-${{ github.sha }}` confirma que a tag existe
- `docker run --rm <image> java -version` confirma JDK 21 dentro do container
- Smoke test efêmero: `docker run --rm -d --name ci-smoke -p 8080:8080 <image>` + loop de 30 × 2s em `curl -fsS http://localhost:8080/actuator/health` → `200 UP` + `docker rm -f ci-smoke`

**Quando usar:**
- Após cada push em `release/*` (validação automática, fail-fast) — custo zero no orçamento de Actions (repo público, ilimitado)
- Antes de uma tag `v*` quando o mantenedor quer confirmar que o build está saudável sem fazer deploy real
- Durante rollout das Fases 1-4 como smoke test para validar que mudanças no `Dockerfile` ou `build.gradle` não quebram o buildx remoto

**Por que L2 ≠ L1:** L1 (`ci.yml`) valida que o código compila e os testes passam, mas **não constrói imagem**. L2 constrói a imagem real no runner hosted, valida JDK, e confirma que o artefato é executável — exatamente o que o GHCR vai hospedar. L2 não toca em SSH, então é seguro rodar a qualquer momento.

**Por que L2 não cobre L3/L4:** L2 valida **a imagem**; L3/L4 validam o **deploy** (SSH via Tailscale, `docker compose` no servidor real, health check ponta-a-ponta). Essas camadas dependem da conectividade com o servidor (Tailscale para CI/CD) e de ambiente descartável ou de homologação (fora do escopo deste PRD).

### Fase 1 — Migração do `deploy.yml` (Produção) (1-1.5h)

**Objetivo:** Substituir o runner self-hosted pelo hosted no deploy de produção, sem alterar comportamento observável. Adotar imagem no GHCR (tags `<git-tag>`, `sha-<7>`, `latest`) e gate humana do env `production` (decisão v1.5). **Transporte SSH via Tailscale** (v1.13).

| # | Tarefa (revisada v1.13) | Dependência |
|---|--------|-------------|
| 1.1 | Reescrever `.github/workflows/deploy.yml` com `runs-on: ubuntu-latest`. Job `build`: `actions/checkout` + `actions/setup-java` (JDK 21) + `actions/cache` (Gradle) + `./gradlew build -x test` + `docker/login-action` + `docker/build-push-action@v6` → GHCR com tags `<git-tag>`, `sha-<full-commit>`, `latest` (permissões elevadas: `contents: read` + `packages: write`). Job `deploy`: **`tailscale/github-action@v4`** (step 1 — nó efêmero na rede mesh) + `ssh`/`scp` direto via Tailscale SSH para materializar `.env`/chaves JWT, GHCR pull por SHA + retag, `docker compose up -d`, health check (90 * 5s), rollback (tag `:backup` no servidor), pgBackRest check, `docker image prune`. **packages: read** necessário no deploy job para GHCR pull (v1.18) | Fase 0.7 |
| 1.1b | Configurar env `production` no GitHub com **required reviewers** (mantenedor como reviewer inicial) — gate humana aplicada **já na Fase 1**, não na Fase 5 | 1.1 |
| 1.2 | Adicionar **5 secrets novos** ao GitHub (`PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_USER`, `PROD_SSH_KEY`, `TS_AUTH_KEY`, `TS_TAILSCALE_IP`) | 1.1 |
| 1.3 | Testar em tag de homologação: criar tag `v1.3.0-rc1` em branch de release (`release/1.3.0`) | 1.2 |
| 1.4 | Validar: deploy rodou, health check passou, rollback funcionou em falha simulada | 1.3 |
| 1.5 | Documentar mudanças no `AGENTS.md` | 1.4 |

> **Testes no CI release:** o job `build` **não** roda `./gradlew test` por enquanto (paridade com workflow atual; testes rodam em PRs via `ci.yml` da Fase 0.6). **Meta futura:** adicionar `./gradlew test` no job `build` da release.

> **Gate humana:** toda tag `v*` em `release/*` exige aprovação manual de um reviewer do env `production` antes de o job `deploy` ser executado. O job `build` (push GHCR) roda automaticamente; o `deploy` para na aprovação.

**Validação (modo seguro conforme `SECURITY_ASSESSMENT.md` item 9, linhas 1046-1049):**
- Manter runner self-hosted **rodando em paralelo** por 2 semanas após a migração do `deploy.yml`
- Workflow de produção atual continua disponível como fallback
- Monitorar execuções diariamente

**Rollback:** Reverter o commit do workflow para a versão anterior (self-hosted). Efeito: próximo deploy usa o runner antigo.

### Fase 2 — Migração do `deploy-qa.yml` (1.5h)

**Objetivo:** Aplicar o mesmo padrão ao deploy de QA, validando primeiro o pipeline hosted em ambiente não-crítico. QA é 100% manual (decisão v1.5).

| # | Tarefa (revisada v1.5) | Dependência |
|---|--------|-------------|
| 2.0 | **Tunnel SSH de QA já provisionado na Fase 0.7 (v1.7):** tunnel `workshop-ssh-qa` com DNS `ssh.qa.eletroluk.com` e service systemd `cloudflared-ssh-qa.service` já estão ativos. **Nada a fazer aqui** — apenas consumir nas tarefas 2.1-2.4 | Fase 1 |
| 2.1 | Reescrever `.github/workflows/deploy-qa.yml` (mesmo padrão do `deploy.yml`). **Trigger:** `workflow_dispatch` apenas (sem `push: branches`). **Imagem GHCR:** tag `qa-<sha-7>` apenas (sem `latest`); reaproveita a imagem da prod (configuração vem de `.env` + `docker-compose-qa.yml`). **Sem rollback automático. Sem pgBackRest.** | 2.0 |
| 2.2 | Adicionar **4 secrets novos** ao GitHub (`QA_SSH_HOST`, `QA_SSH_PORT`, `QA_SSH_USER`, `QA_SSH_KEY` — chave Ed25519 **separada** da `PROD_SSH_KEY`, defense-in-depth) | 2.1 |
| 2.3 | Testar via `workflow_dispatch` (QA não tem mais trigger automático de branch) | 2.2 |
| 2.4 | Validar deploy QA + smoke test manual (`curl https://api-qa.eletroluk.com/api/health` retorna 200) + `./gradlew integrationTests --args='--spring.profiles.active=qa'` | 2.3 |

**Validação:** smoke test manual + integração passa.

> **Branch `release/1.2.0` (decisão v1.5):** mantida como branch de trabalho manual. CI workflow (`ci.yml` da Fase 0.6) roda em PRs para essa branch mas **não** dispara deploy QA. Em um momento futuro, pode-se usar `release/X.Y` como branch de release para a versão X.Y, mantendo o ciclo de QA como manual entre releases.

### Fase 3 — Migração do `deploy-gateway.yml` (45 min)

**Objetivo:** Aplicar o padrão ao deploy do gateway Caddy + Cloudflare tunnels. Imagem do Caddy vai para GHCR (subpacote `workshop_rest_api-caddy-gateway`).

| # | Tarefa (revisada v1.5) | Dependência |
|---|--------|-------------|
| 3.1 | Reescrever `.github/workflows/deploy-gateway.yml`. **Trigger:** `push: tags v*` em `release/*` + `workflow_dispatch` (junto com deploy prod). **Imagem GHCR:** `ghcr.io/unspoken-tech-org/workshop_rest_api-caddy-gateway` (subpacote do owner) com tags `<git-tag>`, `sha-<7>`, `latest`. **SSH:** reusa `PROD_SSH_*` (mesmo servidor, mesmo usuário `workshop`). **Env `production`:** required reviewers (mesma gate humana do deploy prod). **Sem rollback automático**; tag `:backup` no servidor para uso manual. Build context: pasta do Caddy; build incremental com cache de camadas | Fase 2 |
| 3.2 | Validar: Caddy rodando + 2 tunnels HTTP (`cloudflared-gateway-prod`, `cloudflared-gateway-qa`) HEALTHY | 3.1 |

### Fase 4 — Migração do `deploy-observability.yml` (45 min)

**Objetivo:** Aplicar o padrão ao deploy da stack de observabilidade (Loki, Promtail, Grafana). Sem build de imagem (Loki/Grafana/Promtail são Docker Hub oficial); auditoria de bind de portas confirmou que nenhuma porta é publicada no host.

| # | Tarefa (revisada v1.5) | Dependência |
|---|--------|-------------|
| 4.0 | **Auditoria de bind de portas do `docker-compose-observability.yml` (concluída):** nenhum serviço tem bloco `ports:` declarado. Loki só acessível via `observability-network` (interna); Promtail sem porta (coletor); Grafana acessível via `workshop-ingress` (rede do Caddy) e atrás de auth (`GF_USERS_ALLOW_SIGN_UP=false` + `GRAFANA_ADMIN_USER/PASSWORD`). **Nenhuma correção necessária.** | — |
| 4.1 | Reescrever `.github/workflows/deploy-observability.yml`. **Trigger:** `push: tags v*` em `release/*` + `workflow_dispatch` (sem `paths:` filter — irrelevante para tags). **Sem build GHCR** (imagens Docker Hub oficiais). **SSH:** reusa `PROD_SSH_*`. **Env `production`:** required reviewers (igual prod/gateway). **Sem rollback.** **Permissões:** `contents: read` em todos os jobs (sem `packages: write` — não há push de imagem) | Fase 3 |
| 4.2 | Validar: Loki retorna `ready` em `:3100`, Grafana retorna 200 em `:3000/api/health`, Promtail coleta logs (3 instâncias: prod, qa, gateway). **Validação refinada (v1.5):** gates de falha = Promtail rodando + labels em Loki (`environment`) + datasource Loki no Grafana; ingestão de logs = **warning apenas** (pode dar falso negativo em janelas de manutenção). `workflow_dispatch` default `ref: main` | 4.1 |

### Fase 5 — Descomissionamento do Self-Hosted Runner (após 2 semanas de estabilidade) (30 min)

**Objetivo:** Remover o runner self-hosted do GitHub, marcando o item 2 do `SECURITY_ASSESSMENT.md` como resolvido.

| # | Tarefa (revisada v1.7) | Dependência |
|---|--------|-------------|
| 5.0 | **Forçar runners hosted em PRs (defense-in-depth):** GitHub → `Settings → Rules → Rulesets → New ruleset → Branch` → escopo: `main` + `release/*` → critério: "Require workflows to pass before merging" + "Restrict workflows that can run" → marcar apenas runners `ubuntu-latest` (GitHub-hosted). **Limitação conhecida (v1.7):** rulesets não conseguem filtrar runners em PRs de fork (workflows de fork nem rodam sem approval); a defesa em PRs de fork é "Require approval for first-time contributors" (Frente 1, 5.1.6). Documentar explicitamente | Fases 1-4 |
| 5.1 | Confirmar 2+ semanas de execuções 100% bem-sucedidas nos hosted runners; **se houver qualquer falha durante o período, estender por mais 1 semana** | 5.0 |
| 5.2 | Acessar GitHub → `Settings → Actions → Runners → Remove` | 5.1 |
| 5.3 | **Remover tudo (opção B — sem backup):** `systemctl stop actions.runner.* && rm -rf ~/actions-runner`. Reverter exige gerar novo token de registro no GitHub (processo de ~5min) | 5.2 |
| 5.4 | Atualizar `SECURITY_ASSESSMENT.md`: marcar item 2 como **✅ Resolvido em 20/06/2026** (data ajustada de v1.5) | 5.3 |
| 5.5 | Atualizar `AGENTS.md`: remover referências ao runner self-hosted | 5.4 |
| **5.6 (NEW, v1.7, atualizado v1.17)** | **Remover 10 secrets obsoletos do GitHub** (inventariados na Fase 0.5): 4 órfãos antigos (`SSH_PRIVATE_KEY`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSPHRASE`) + 6 SSH obsoletos v1.17 (`PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_KEY`, `QA_SSH_HOST`, `QA_SSH_PORT`, `QA_SSH_KEY`). Comando: `gh secret delete <SECRET_NAME>` para cada um. **Pré-condição:** confirmar que nenhum workflow ativo referencia esses secrets (grep em `.github/workflows/*.yml`) | 5.5 |
| ~~**5.7 (NEW, v1.7)**~~ | ~~**Confirmar que `deploy-wrapper.sh` está em produção**~~ | ⚠️ **OBSOLETO (v1.17):** `deploy-wrapper.sh` foi removido do servidor. |

### Fase 6 — Hardening Adicional Contínuo (2h)

**Objetivo:** Endurecer o pipeline conforme as práticas recomendadas pelo `SECURITY_ASSESSMENT.md` (item 14 — varredura CVEs no CI, item 15 — limpeza de secrets).

| # | Tarefa (revisada v1.7) | Dependência |
|---|--------|-------------|
| **6.1 (v1.7 — endurecimento gradual)** | Adicionar step de scan com `aquasec/trivy` em **2 etapas**: (a) **D+2 a D+16:** `trivy fs --exit-code 0 --severity HIGH,CRITICAL .` (relatório, não-bloqueante) para gerar baseline de findings; (b) **D+16 em diante:** endurecer para `trivy fs --exit-code 1 --severity HIGH,CRITICAL .` (bloqueante) após findings conhecidos serem mitigados ou aceitos. Scope **filesystem** apenas (não scan de imagens Docker Hub oficiais) | Fases 1-4 |
| 6.2 | Adicionar step `audit-temp-files` no job `cleanup` (via SSH): identificar cópias temporárias (`.env.bak`, `*.pem.bak`) no servidor; se houver, `shred -u` nelas. **NÃO** aplicar `shred` em `.env`/chaves ativas (o app Spring Boot lê em runtime). Auditoria inicial para confirmar ausência de cópias; se vazio, no-op | 6.1 |
| 6.3 | Configurar GitHub Environments com "Required reviewers" para `production` (proteção extra contra deploy acidental). Já aplicado na Fase 1 (decisão v1.5) — esta tarefa vira "noop" | 6.2 |
| 6.4 | Adicionar `concurrency:` em **todos os workflows** (v1.7 — também em `deploy-qa.yml`): prod `group: production, cancel-in-progress: false`; QA `group: qa-deploy, cancel-in-progress: true`; gateway e observability `group: gateway-deploy` / `group: observability-deploy, cancel-in-progress: false` (deploys em andamento não devem ser cancelados) | 6.3 |
| 6.5 | Adicionar `timeout-minutes:` em cada job: `build` 20 min, `deploy` **45 min** (metade do anterior — monitorar tempos reais para reduzir; alvo: ≤30 min), `verify` 30 min, `cleanup` 10 min | 6.4 |
| **6.6 (NEW, v1.7)** | **Adicionar `attestations: write`** no job `build` (além de `packages: write`) para gerar **build provenance** (SLSA). Exemplo: `permissions: { contents: read, packages: write, attestations: write }` no job `build` + usar `docker/build-push-action` com `provenance: true` (default) + step adicional `actions/attest-build-provenance` se necessário. Sem impacto em deploy — só adiciona assinatura criptográfica à imagem no GHCR | 6.5 |
| **6.7 (NEW, v1.7)** | **Habilitar DNSSEC em `eletroluk.com`** (atualmente `delegationSigned: false` conforme RDAP verificado em 20/06/2026). Cloudflare oferece DNSSEC on/off no dashboard (`DNS → Settings → DNSSEC`). Mitiga risco de DNS spoofing em `ssh.eletroluk.com` e `ssh.qa.eletroluk.com` (reduz superfície de ataque do transport SSH) | 6.6 |

---

## 5. Endurecimento de Segurança do Repositório

Conforme `SECURITY_ASSESSMENT.md` (itens 1, 2, 11) e a auditoria completa do repositório realizada em 16/06/2026. As proteções estão organizadas em 3 frentes de implementação:

### 5.1 Frente 1 — Via `gh` CLI (automatizável, ~5 min)

| # | Proteção | Estado atual | Estado desejado | Comando `gh` |
|---|----------|--------------|-----------------|---------------|
| 5.1.1 | Secret scanning | ❌ Desabilitado | ✅ Habilitado | `gh api -X PATCH repos/unspoken-tech-org/workshop_rest_api -f security_and_analysis[secret_scanning][status]=enabled` |
| 5.1.2 | Secret scanning push protection | ❌ Desabilitado | ✅ Habilitado | `gh api -X PATCH repos/unspoken-tech-org/workshop_rest_api -f security_and_analysis[secret_scanning_push_protection][status]=enabled` |
| 5.1.3 | Dependabot security updates | ❌ Desabilitado | ✅ Habilitado | `gh api -X PATCH repos/unspoken-tech-org/workshop_rest_api -f security_and_analysis[dependabot_security_updates][status]=enabled` |
| 5.1.4 | Bloquear force push no `main` | ⚠️ Permitido | ✅ Bloqueado | `gh api -X DELETE repos/unspoken-tech-org/workshop_rest_api/branches/main/protection/force_push` |
| 5.1.5 | Desabilitar bypass de admins | ❌ Admins bypassam | ✅ Admins sujeitos às regras | `gh api -X PUT repos/unspoken-tech-org/workshop_rest_api/branches/main/protection/enforce_admins` |
| 5.1.6 | Fork PR approval | ❌ Default (pode executar) | ✅ "Require approval for first-time contributors" | Configurar via `Settings → Actions → General → Fork pull request workflows` |

> **Nota sobre 5.1.4:** O force push no `main` é um risco crítico — permite reescrever histórico e potencialmente injetar código malicioso em branches protegidas.

> **Nota sobre 5.1.6:** O fork PR approval não bloqueia imediatamente o `pull_request` event porque os workflows atuais não usam esse trigger. Mas é uma camada extra de defesa caso workflows futuros adicionem `on: pull_request`.

### 5.2 Frente 2 — Via git commit (arquivos no repo, ~45 min)

| # | Proteção | Estado atual | Estado desejado | Ação |
|---|----------|--------------|-----------------|------|
| 5.2.1 | CODEOWNERS | ❌ Não existe | ✅ Criado | Criar `.github/CODEOWNERS` |
| 5.2.2 | dependabot.yml | ❌ Não existe | ✅ Criado | Criar `.github/dependabot.yml` |
| 5.2.3 | SECURITY.md | ❌ Não existe | ✅ Criado | Criar `.github/SECURITY.md` com política de disclosure |
| 5.2.4 | CodeQL workflow | ❌ Não existe | ✅ Criado | Criar `.github/workflows/codeql.yml` (análise estática Java/Kotlin) |
| 5.2.5 | CI test workflow (camada L1) | ❌ Não existe | ✅ Criado | Criar `.github/workflows/ci.yml` (roda `./gradlew test` em PRs e push em `main`; ver Fase 0.6) |
| 5.2.6 | Permissions blocks | ❌ Nenhum workflow tem | ✅ Todos definidos | Adicionar `permissions: contents: read` nos 4 workflows existentes |
| 5.2.7 | Build image workflow (camada L2) | ❌ Não existe | ✅ Criado | Criar `.github/workflows/build-image.yml` (build isolado + push GHCR + smoke efêmero; ver Fase 0.6.1) |

**Conteúdo sugerido para `.github/CODEOWNERS`:**

```
# Default: todos os arquivos requerem revisão de maintainer
* @unspoken-tech-org/maintainers

# Workflows requerem revisão extra
.github/workflows/ @unspoken-tech-org/maintainers
```

**Conteúdo sugerido para `.github/dependabot.yml`:**

```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
  - package-ecosystem: "docker"   # v1.7: adicionado para escanear Dockerfile base images
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
```

**Conteúdo sugerido para `.github/SECURITY.md`:**

```markdown
# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. **DO NOT** open a public GitHub issue
2. Email security@eletroluk.com (or contact a maintainer directly)
3. Include: description, steps to reproduce, potential impact
4. We will acknowledge within 48 hours and provide a timeline for fix

## Scope

- Workshop REST API (this repository)
- Deployed instances at api.eletroluk.com and api-qa.eletroluk.com

## Out of Scope

- Third-party dependencies (report to their maintainers)
- Social engineering attacks
```

**Conteúdo sugerido para `.github/workflows/codeql.yml`:**

```yaml
name: "CodeQL Analysis"

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: "0 6 * * 1"  # Segunda-feira, 6h UTC

jobs:
  analyze:
    runs-on: ubuntu-latest
    permissions:
      security-events: write
      contents: read
    strategy:
      matrix:
        language: ["java"]
    steps:
      - uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11 # v4.1.1
      # v1.7: SHAs distintos por sub-action (corrige bug de v1.6 que usava o mesmo SHA para 3 sub-actions)
      # Auditoria: consultar https://github.com/github/codeql-action/releases para SHAs estáveis em jun/2026
      - uses: github/codeql-action/init@<SHA-INIT>     # v1.7: SHA próprio
        with:
          languages: ${{ matrix.language }}
      - uses: github/codeql-action/autobuild@<SHA-AUTOBUILD>  # v1.7: SHA próprio
      - uses: github/codeql-action/analyze@<SHA-ANALYZE>     # v1.7: SHA próprio
```

**Conteúdo sugerido para `.github/workflows/ci.yml` (camada L1):**

```yaml
name: CI Tests

on:
  pull_request:
    branches: [main, release/*]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11 # v4.1.1
      - uses: actions/setup-java@c5195efec3a88e3aec3b1ee006011f1d9047b7f3 # v4.1.0
        with:
          distribution: temurin
          java-version: "21"
      - uses: actions/cache@1bd1e32a3bdc45362d1e726936510720a7c30a57 # v4.2.0
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
          restore-keys: ${{ runner.os }}-gradle-
      - run: ./gradlew test
```

> **L1 (camada local/PR):** valida que o código compila, que `./gradlew test` passa, e que os SHA pins dos actions ainda existem (via `actionlint` em job dedicado ou lint manual). **L1 não constrói imagem** — isso é responsabilidade do L2 (próximo item).
>
> **Comandos locais equivalentes antes do PR:**
> ```bash
> ./gradlew clean build
> ./gradlew test
> actionlint .github/workflows/*.yml
> yamllint .github/workflows/*.yml
> ```

**Conteúdo sugerido para `.github/workflows/build-image.yml` (camada L2):**

```yaml
name: Build Image (no deploy)

on:
  workflow_dispatch:
    inputs:
      ref:
        description: "Branch ou tag para checkout (default: main)"
        required: false
        default: "main"
        type: string
  push:
    branches: [release/*]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11 # v4.1.1
        with:
          ref: ${{ github.event.inputs.ref || github.ref }}

      - uses: actions/setup-java@c5195efec3a88e3aec3b1ee006011f1d9047b7f3 # v4.1.0
        with:
          distribution: temurin
          java-version: "21"

      - uses: actions/cache@1bd1e32a3bdc45362d1e726936510720a7c30a57 # v4.2.0
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
          restore-keys: ${{ runner.os }}-gradle-

      - run: ./gradlew build -x test

      - uses: docker/login-action@<SHA-v3.2.0>
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/build-push-action@0565861a31f2c772f9f039e2a5e74e9b8e26c0a3 # v6.0.0
        with:
          context: .
          push: true
          tags: |
            ghcr.io/unspoken-tech-org/workshop_rest_api:sha-${{ github.sha }}
            ghcr.io/unspoken-tech-org/workshop_rest_api:dev-${{ github.event.inputs.ref || github.ref_name }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Validate image (JDK 21)
        run: |
          docker buildx imagetools inspect ghcr.io/unspoken-tech-org/workshop_rest_api:sha-${{ github.sha }}
          docker run --rm ghcr.io/unspoken-tech-org/workshop_rest_api:sha-${{ github.sha }} java -version

      - name: Smoke test (ephemeral container)
        run: |
          docker run --rm -d --name ci-smoke -p 8080:8080 \
            ghcr.io/unspoken-tech-org/workshop_rest_api:sha-${{ github.sha }}
          for i in {1..30}; do
            if curl -fsS http://localhost:8080/actuator/health | grep -q UP; then
              echo "Health OK"
              docker rm -f ci-smoke
              exit 0
            fi
            sleep 2
          done
          echo "Health check failed"
          docker logs ci-smoke
          docker rm -f ci-smoke || true
          exit 1
```

> **Notas de implementação do L2:**
> - O SHA de `docker/login-action` está descrito na tabela da seção 3.6 com placeholder `<SHA-v3.2.0>` — substituir pelo SHA completo do commit no momento do merge (mesmo padrão dos demais pinos).
> - `cache-from: type=gha` reaproveita camadas Docker entre builds (GitHub Actions cache), acelerando o ciclo.
> - O smoke test efêmero é o **diferencial** em relação ao `deploy.yml` (que faz health check no servidor de produção). L2 valida que a imagem é auto-suficiente; L4 valida o deploy real.
> - L2 falha o job se qualquer um dos 3 gates falhar: tag ausente no GHCR, JDK incorreto, ou health check não responde em 60s. Essa fail-fast é o valor real do L2.
> - **L2 não toca em SSH nem no servidor de produção** — pode rodar a qualquer momento sem risco.

### 5.3 Frente 3 — Via Web UI (configurações do GitHub, ~20 min)

| # | Proteção | Estado atual | Estado desejado | Caminho no GitHub |
|---|----------|--------------|-----------------|-------------------|
| 5.3.1 | Status checks obrigatórios | ❌ Nenhum | ✅ CI check obrigatório | `Settings → Branches → Branch protection rules → main → Require status checks` → adicionar `test` |
| 5.3.2 | Environment `production` — required reviewers | ❌ Sem reviewers | ✅ 1 reviewer obrigatório | `Settings → Environments → production → Required reviewers` → adicionar mantenedor |
| 5.3.3 | Environment `production` — bypass de admins | ⚠️ Admins bypassam | ✅ Admins não bypassam | `Settings → Environments → production` → desmarcar "Allow specified actors to bypass" |
| 5.3.4 | Webhook de notificações | ❌ Nenhum | ✅ Slack/Discord para alerts | `Settings → Webhooks → Add webhook` → URL do canal, eventos: `workflow_run`, `security_alert` |
| 5.3.5 | 2º collaborator admin | ❌ 1 admin apenas | ✅ 2+ admins | `Settings → Collaborators → Add people` → papel `Admin` |

### 5.4 Itens NÃO incluídos (decisão do mantenedor)

| Item | Motivo da exclusão |
|------|-------------------|
| Delete branch on merge | Escolha do mantenedor — manter branches após merge para auditoria/hotfix |

### 5.5 Resumo das Proteções

```
Frente 1 (gh CLI)          Frente 2 (git commit)         Frente 3 (Web UI)
─────────────────          ─────────────────────         ──────────────────
✅ Secret scanning         ✅ CODEOWNERS                 ✅ Status checks
✅ Push protection         ✅ dependabot.yml             ✅ Env required reviewers
✅ Dependabot updates      ✅ SECURITY.md                ✅ Env bypass disabled
✅ Block force push        ✅ CodeQL workflow            ✅ Webhook notificações
✅ Disable admin bypass    ✅ CI test workflow           ✅ 2º admin collaborator
✅ Fork PR approval        ✅ Permissions blocks
```

**Total: 19 proteções** (6 via CLI + 7 via git + 5 via UI + 1 existente mantida)

---

## 6. Critérios de Aceitação

### 6.1 Funcionais

- [ ] **AC1.** Push de tag `v*` em `release/*` dispara deploy de produção, gateway e observability (3 workflows) bem-sucedidos via hosted runner.
- [ ] **AC2.** `workflow_dispatch` no `deploy-qa.yml` permite deploy manual de QA.
- [ ] **AC3.** `workflow_dispatch` permite deploy manual de qualquer ambiente (prod, gateway, observability, QA).
- [ ] **AC4.** Health check da API pós-deploy retorna 200 em **≤ 5 minutos** após o `docker compose up -d`. Tolerância operacional do loop: **90 iterações × 5s = 7,5 min** (corrige inconsistência de v1.6 que dizia "menos de 5 minutos" com loop de 7,5 min).
- [ ] **AC5.** Rollback automático funciona quando health check falha (apenas no `deploy.yml`; QA/gateway/observability sem rollback). **O rollback é feito exclusivamente pela tag `:backup` da imagem retida no servidor** — **NÃO** via GHCR `latest` (que é sobrescrita a cada push e perderia a referência à imagem anterior; corrigido em v1.7).
- [ ] **AC6.** Secrets não aparecem em logs do GitHub Actions (validação visual de cada run).
- [ ] **AC7.** `./gradlew build` continua passando localmente (testes rodam no `ci.yml` em PRs; CI release não roda `./gradlew test` por enquanto — meta futura).
- [ ] **AC8.** Workflow `ci.yml` (camada L1) roda `./gradlew test` em todo PR aberto contra `main` ou `release/*`, e em todo push em `main`. Falha de teste bloqueia merge (status check obrigatório configurado na seção 5.3.1).
- [ ] **AC9.** Workflow `build-image.yml` (camada L2) consegue buildar a imagem no runner hosted e fazer push para o GHCR com tags `sha-<7>` e `dev-<ref>` em menos de 10 minutos (cold start + build + push).
- [ ] **AC10.** Step "Smoke test (ephemeral container)" do `build-image.yml` valida que a imagem responde `{"status":"UP"}` em `/actuator/health` dentro de 60 segundos após `docker run`. Falha desse step quebra o job.
- [ ] **AC11.** Step "Validate image (JDK 21)" do `build-image.yml` confirma que a imagem executa `java -version` com saída contendo `21`. Falha desse step quebra o job.

### 6.2 Segurança (alinhados ao `SECURITY_ASSESSMENT.md`)

- [ ] **AS1.** Nenhum workflow contém `runs-on: self-hosted` após a Fase 5.
- [ ] **AS2.** Todos os workflows têm `permissions: contents: read` no nível do workflow; jobs `build` que fazem push GHCR adicionam `packages: write` **apenas no escopo do job**.
- [ ] **AS3.** Todas as actions de terceiros estão pinadas por SHA completo (não por tag mutável).
- [ ] **AS4.** Item 2 do `SECURITY_ASSESSMENT.md` marcado como **✅ Resolvido**.
- [ ] **AS5.** Secret scanning, push protection e dependabot habilitados no repositório (Frente 1).
- [ ] **AS6.** Fork PRs requerem approval de mantenedor antes de executar workflows (Frente 1).
- [ ] **AS7.** Chave SSH do runner self-hosted removida do servidor (passo 5.3 do plano).
- [ ] **AS8.** `GITHUB_TOKEN` não tem permissões de escrita amplas (`packages: write` apenas nos jobs `build` do `deploy.yml` e `deploy-gateway.yml`; demais jobs com `contents: read` apenas).
- [ ] **AS9.** Force push no `main` está bloqueado (Frente 1).
- [ ] **AS10.** Admins são sujeitos às branch protection rules (Frente 1).
- [ ] **AS11.** Arquivo `.github/CODEOWNERS` existe e é válido (Frente 2).
- [ ] **AS12.** Arquivo `.github/dependabot.yml` existe e monitora `gradle` + `github-actions` (Frente 2).
- [ ] **AS13.** Status checks obrigatórios configurados no `main` (Frente 3).
- [ ] **AS14.** Environment `production` possui required reviewers (Frente 3; aplicada já na Fase 1).
- [ ] **AS15.** Environment `production` não permite bypass de admins (Frente 3).
- [ ] **AS16.** Arquivo `.github/SECURITY.md` existe com política de disclosure (Frente 2).
- [ ] **AS17.** Workflow CodeQL roda em PRs e push em `main` (Frente 2).
- [ ] **AS18.** Workflow CI roda `./gradlew test` em PRs (Frente 2).
- [ ] **AS19.** Todos os workflows têm `permissions:` definido explicitamente (Frente 2).
- [ ] **AS20.** Pelo menos 2 collaborators com papel admin no repo (Frente 3).
- [ ] **AS21.** Repository ruleset ativo restringe runners em `main` e `release/*` apenas a `ubuntu-latest` (Fase 5.0, defense-in-depth contra re-introdução acidental de self-hosted).

### 6.3 Operacionais

- [ ] **AO1.** Nenhum secret novo precisa ser gerado além dos **5 secrets novos** (4 SSH prod: `PROD_SSH_HOST/PORT/USER/KEY`; 3 Tailscale: `TS_AUTH_KEY`, `TS_TAILSCALE_IP`). Secrets Cloudflare para containers Docker HTTP mantidos (`CF_API_TOKEN`, `CLOUDFLARE_TUNNEL_TOKEN`).
- [ ] **AO2.** `AGENTS.md` atualizado com novos comandos e referências.
- [ ] **AO3.** Documentação no `SECURITY_ASSESSMENT.md` reflete a nova arquitetura.
- [ ] **AO4.** Não há regressão no tempo médio de deploy (alvo: ≤ baseline + 2 min; tolerância para cold start do runner hosted + pull de imagem GHCR; `timeout-minutes: 45` para `deploy` na Fase 6).
- [ ] **AO5.** Tailscale `tailscaled` ativo e resiliente a reboot: `systemctl is-enabled tailscaled` retorna `enabled`; módulo `tun` configurado em `/etc/modules-load.d/tun.conf`; `tailscale status` mostra nó conectado com SSH habilitado.
- [ ] **AO6.** Smoke test ponta-a-ponta via Tailscale (CI/CD): `tailscale/github-action@v4` cria nó efêmero → `ssh workshop@TS_TAILSCALE_IP` conecta sem pedir senha, valida autenticação via Tailscale SSH no servidor.
- [ ] **AO7.** `AGENTS.md` documenta o uso de Tailscale para CI/CD e acesso SSH manual, os **5 secrets novos** do GitHub (4 SSH + 3 Tailscale, com `TS_AUTH_KEY` no lugar de `TS_OAUTH_CLIENT_ID/SECRET`), os novos triggers (tag `v*` para prod/gateway/observability; `workflow_dispatch` para QA), e o wrapper `deploy-wrapper.sh` com `command="..."` no `authorized_keys` (Fase 0.3 v1.7).

---

## 7. Análise de Riscos da Migração

### 7.1 Riscos Técnicos

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Build no hosted runner falha por diferença de ambiente (locale, timezone, etc.) | Média | Médio | Usar `ubuntu-latest` (mesmo OS); `actions/setup-java@v4` padroniza JDK 21; **camada L2 (`build-image.yml` da Fase 0.6.1) exercita build + smoke efêmero a cada push em `release/*`, falhando cedo antes de envolver SSH** |
| SSH para o servidor falha por firewall/porta bloqueada | Baixa | Alto | Tailscale usa **WireGuard (UDP)** — funciona atrás de NAT sem port mapping; nenhuma porta pública necessária no servidor |
| Tempo de deploy aumenta (cold start do hosted runner) | Média | Baixo | ~30-60s extras por deploy (cold start + pull de imagem GHCR); aceitável para cadência de release atual; `timeout-minutes: 45` no `deploy` (Fase 6) |
| Secrets de SSH vazam em logs | Baixa | Alto | `ssh`/`scp` direto não loga conteúdo de comandos por padrão; `TS_AUTH_KEY` passado como **env var** (não CLI arg) para não aparecer em `ps aux` |
| **Script Injection em `run:` blocks (v1.19)** | Baixa | Crítico | Expressões `${{ github.event.inputs.ref }}`, `${{ github.ref_name }}`, `${{ github.sha }}`, `${{ github.actor }}`, `${{ github.event_name }}` movidas para `env:` blocks antes de uso em `run:` — valores user-controllable nunca passam diretamente pelo shell. Corrigido em 3 steps do `deploy.yml` (Prepare image tag, Show build info, Cleanup Summary) |
| `packages: write` no job `build` amplia blast radius do `GITHUB_TOKEN` | Baixa | Médio | Permissão declarada **apenas no escopo do job** (não no workflow inteiro); `contents: read` em todos os demais jobs; **follow-up (v1.7):** considerar `attestations: write` (Fase 6) para build provenance SLSA |
| Trivy scan bloqueia deploy por falso positivo (modo fail desde o início) | Média | Médio | **v1.7 — endurecimento gradual:** iniciar com `trivy fs --exit-code 0 --severity HIGH,CRITICAL` (relatório, não-bloqueante) por 2 semanas para gerar baseline; endurecer para `--exit-code 1` apenas após findings conhecidos serem mitigados ou aceitos. Build roda em jobs separados para fácil rollback do scan |
| **Limite do plano Tailscale Personal (1.000 min efêmeros/mês)** | Média | Alto | Monitorar uso mensal via dashboard Tailscale; upgrade para Standard ($8/mês) se necessário; a taxa atual (~200 deploys/mês) cabe no limite; deploys rápidos (<5 min) consomem pouco |
| Tailscale Auth Key vaza | Baixa | Alto | Auth keys são **revogáveis** no admin console (`Settings → Keys`); chave permite criar nós efêmeros na rede mesh mas não dá acesso ao servidor diretamente — Tailscale SSH + chave SSH Ed25519 ainda são necessários (defesa em profundidade); monitorar uso anômalo no dashboard Tailscale; auth key expira em 90 dias (renovação manual) |
| Tailscale não reconecta após reboot do servidor | Baixa | Alto | `tailscaled` habilitado no systemd (`systemctl is-enabled tailscaled`); módulo `tun` configurado em `/etc/modules-load.d/tun.conf`; state persistente em `/var/lib/tailscale/`; acesso físico ao servidor como contingência final |

### 7.2 Riscos de Segurança Residuais

| Risco residual | Vetor | Mitigação |
|----------------|-------|-----------|
| Ataque via fork PR | Repo público + `allow_forking: true` | Workflows não usam `pull_request`; "Require approval for all outside collaborators" (Frente 1); **Fase 5.0 adiciona ruleset restringindo runners a `ubuntu-latest` em `main` e `release/*`** (defense-in-depth) |
| Supply chain via actions de terceiros | `tj-actions/changed-files` incidente Mar/2025 | Pinning por SHA imutável (seção 3.6); Dependabot monitora releases (Frente 2) |
| Exfiltração de secrets via logs | Secrets printados acidentalmente | Marcar todos os secrets como masked (padrão GitHub); não usar `set -x` em steps com secrets |
| Atacante obtém `GITHUB_TOKEN` válido | Phishing, credencial roubada | `GITHUB_TOKEN` tem `contents: read` por padrão; `packages: write` apenas em jobs `build` (escopo limitado); deploy depende de SSH key separada (não atrelada ao `GITHUB_TOKEN`) |
| Branch protection bypass | Workflow com `pull_request_target` | Nenhum workflow usa `pull_request_target` (verificado) |
| Chave SSH do QA vaza (ex.: devs com acesso ao workflow do QA) | Comprometimento do secret `QA_SSH_KEY` | Chave `QA_SSH_KEY` é **separada** da `PROD_SSH_KEY` (defense-in-depth); prod e QA compartilham o mesmo host, então vazar `QA_SSH_KEY` dá shell no servidor de prod. **Mitigação adotada em v1.7 (Fase 0.3, promoted from "opcional" no v1.6):** `authorized_keys` com `command="/usr/local/bin/deploy-wrapper.sh"` + flags `no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty` — shell interativo fica **bloqueado**; apenas comandos whitelisted pelo wrapper são executados |

### 7.3 Riscos Operacionais

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Mantenedor esquece de atualizar SHA de uma action após release | Baixa | Baixo | Documentar processo no `AGENTS.md`; revisar mensalmente |
| 3 aprovações humanas por release (prod, gateway, observability) sobrecarregam mantenedor único | Média | Baixo | Janela de release alinhada com horário do mantenedor; 2º admin reduz gargalo (Fase 5.3.5); opcionalmente fundir jobs com matriz (`deploy.yml` e `deploy-gateway.yml` no mesmo workflow via matrix) |
| Runner self-hosted é esquecido e cobra $0.002/min após mar/2026 | Média | Baixo | Fase 5 remove o runner completamente (opção B — sem backup) |
| Downtime do GitHub bloqueia deploys | Muito baixa | Médio | GitHub SLA 99.9%; plano de contingência: registrar novo runner self-hosted (~5min via token de registro) **mas apenas como contingência, não como rotina** |

---

## 8. Métricas de Sucesso

| Métrica | Baseline (atual) | Meta (após Fase 5) |
|---------|------------------|---------------------|
| Tempo médio de deploy de produção | ~8-12 min | ≤ 14 min (baseline + 2 min para cold start + pull GHCR) |
| Cold start do runner | N/A (persistente) | ≤ 60s (cold start isolado; pull GHCR é separado) |
| Secrets em arquivos no disco (deploy) | 4 arquivos (`config/keys/*.pem`, `.env`) | 0 (apenas injetados via SSH, descartados no fim) |
| Superfície de ataque do CI | Docker socket + rede doméstica | Apenas SSH key em host ephemeral |
| Número de hosts com acesso a secrets | 1 (servidor) | 1 (servidor, mesmo de antes) |
| Custo mensal de CI | $0 (mas $0.002/min após mar/2026) | $0 (repo público, dentro da quota gratuita mensal de jun/2026) — **corrige v1.6 que dizia "ilimitado"** |
| **Secrets novos no GitHub** (v1.13) | — | **5** (4 SSH prod + 1 Tailscale Auth Key) — `TS_AUTH_KEY` no lugar de `TS_OAUTH_CLIENT_ID/SECRET`; `CF_ACCESS_CLIENT_ID/SECRET` removidos |

---

## 9. Cronograma Sugerido (v1.7)

| Fase | Duração | Início | Término | Bloqueio |
|------|---------|--------|---------|----------|
| **B. Pré-requisitos (v1.7 — NEW)** — confirmar CF Free + Zero Trust org; tunnels Docker existentes | 15 min (paralelo) | D+0 | D+0 | — |
| Fase 0 — Preparação (2 chaves Ed25519, `command="..."` em `authorized_keys` via wrapper, inventário de secrets órfãos) | 30 min | D+0 | D+0 | — |
| Fase 0.5 — Proteções do repo (Frentes 1-3) | 30 min (paralelo) | D+0 | D+0 | — |
| Fase 0.6 — Ações complementares (SECURITY.md, CodeQL com **SHAs distintos por sub-action v1.7**, **L1: `ci.yml`**, permissions) | 30 min (paralelo) | D+0 | D+0 | — |
| **Fase 0.6.1 — Workflow `build-image.yml` (camada L2, build GHCR isolado, sem deploy)** | **15 min (paralelo)** | **D+0** | **D+0** | **Fase 0.6** |
| **Fase 0.7 (descontinuada v1.16)** — ~~Instalar `cloudflared-ssh` nativo + 2 tunnels HTTP + Cloudflare Zero Trust + Service Token~~ — cloudflared-ssh removido do servidor; containers Docker HTTP permanecem ativos | **N/A (histórico)** | **N/A** | **N/A** |
| Fase 1 — Migração `deploy.yml` (gate humana já aplicada, GHCR com `latest`, **rollback via tag `:backup` local v1.7**, **transporte via Tailscale v1.13**) | 1,5h | D+0 | D+0 | Fase 0, Fase 0.7 |
| Fase 2 — Migração `deploy-qa.yml` (tunnel QA já provisionado na Fase 0.7 — **redução de 30 min vs. v1.5**; `concurrency: qa-deploy` v1.7; QA 100% manual) | 1h | D+1 | D+1 | Fase 1 |
| Fase 3 — Migração `deploy-gateway.yml` (subpacote GHCR; mesma gate humana) | 45 min | D+1 | D+1 | Fase 2 |
| Fase 4 — Migração `deploy-observability.yml` (sem `paths:`; validação refinada) | 45 min | D+2 | D+2 | Fase 3 |
| Período de observação (paralelo) | 2 semanas | D+2 | D+16 | Fases 1-4 |
| **Fase 5 — Descomissionamento** (item 5.0 rulesets → 5.3 sem backup → **5.5 NEW: remover 4 secrets órfãos v1.7** → 5.6 atualizar SECURITY_ASSESSMENT) | 30 min | D+16 | D+16 | Observação |
| Fase 6 — Hardening contínuo (1h antes da Fase 5 + 1h após 1 mês) — **v1.7: Trivy em modo relatório com endurecimento gradual; `attestations: write` para build provenance; DNSSEC em `eletroluk.com`** | 2h (distribuídas) | D+2 | D+45 | — |

**Tempo total ativo:** ~8,25 horas distribuídas em 2 dias (+30 min vs. v1.6 para a Fase 0.7 revisada; −30 min na Fase 2 pelo reaproveitamento do tunnel QA já provisionado).
**Tempo total de janela de risco:** 2 semanas (período de observação com self-hosted em paralelo).

---

## 10. Referências

### 10.1 Documentos do Projeto
- `SECURITY_ASSESSMENT.md` — Itens 1-16 do relatório de segurança, especialmente item 2 (CI/CD)
- `AGENTS.md` — Convenções e comandos do projeto

### 10.2 Documentação Oficial GitHub
- [GitHub Actions: Choosing runners for jobs](https://docs.github.com/en/actions/hosting-your-runners/managing-self-hosted-runners/about-self-hosted-runners)
- [GitHub Actions: Security hardening](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [GitHub Actions: Permissions reference](https://docs.github.com/en/actions/security-guides/automatic-token-authentication)
- [GitHub Actions: Fork pull request workflows](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository#enabling-workflows-for-forked-repositories)
- [GitHub Actions: Pricing 2026 (repo público permanece gratuito dentro da quota)](https://github.blog/changelog/2025-12-16-coming-soon-simpler-pricing-and-a-better-experience-for-github-actions/)
- [Cloudflare Zero Trust Free tier](https://developers.cloudflare.com/cloudflare-one/plans/zero-trust-free/) — confirma suporte a Tunnel HTTP + Service Tokens no plano Free (sem exigir Pro+)
- [Cloudflare Tunnel — SSH via Access](https://developers.cloudflare.com/cloudflare-one/applications/non-http/) — documentação oficial de como `cloudflared access ssh` estabelece sessões SSH via tunnel HTTP
- [Cloudflare Tunnel TCP vs HTTP (limitação do Free tier)](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/configure-tunnels/) — TCP tunneling requer plano Pro+; HTTP + Access é a alternativa no Free
- [GitHub Container Registry (GHCR)](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

### 10.3 Actions Utilizadas
- [actions/checkout](https://github.com/actions/checkout) — Check out repository
- [actions/setup-java](https://github.com/actions/setup-java) — Setup JDK
- [actions/cache](https://github.com/actions/cache) — Cache Gradle dependencies
- [docker/build-push-action](https://github.com/docker/build-push-action) — Build & push Docker image
- [tailscale/github-action](https://github.com/tailscale/github-action) — Tailscale mesh network setup

### 10.4 Referências de Segurança Citadas no `SECURITY_ASSESSMENT.md`
- Sysdig: [Self-hosted runners como backdoors (Shai-Hulud)](https://www.sysdig.com/blog/how-threat-actors-are-using-self-hosted-github-actions-runners-as-backdoors)
- StepSecurity: [CVE-2025-32955 — Harden-Runner bypass via Docker](https://www.stepsecurity.io/blog/evolving-harden-runners-disable-sudo-policy-for-improved-runner-security)
- Arctiq: [Top 10 GitHub Actions Security Pitfalls](https://arctiq.com/blog/top-10-github-actions-security-pitfalls-the-ultimate-guide-to-bulletproof-workflows)
- GitHub Security Lab: [`pull_request_target` injection](https://securitylab.github.com/research/github-actions-preventing-pwn-requests/)
- Aikido: [Complete GitHub Actions Security Checklist](https://www.aikido.dev/blog/checklist-github-actions)

---

## Adendo v1.15 — Usuário Dedicado para CI/CD via Tailscale SSH

**Data:** 23/06/2026
**Motivo:** Separação de responsabilidades entre acesso manual ao servidor e CI/CD automatizado via Tailscale SSH.

### Problema

Atualmente, o usuário `workshop` é usado para **tudo**: acesso manual ao servidor (debugging, manutenção) e CI/CD (deploy via Tailscale SSH). Isso apresenta os seguintes riscos:

| Risco | Descrição |
|-------|-----------|
| Auditabilidade | Logs do `journalctl` misturam ações manuais e automatizadas — impossível saber se um comando foi executado por um humano ou pelo pipeline |
| Blast radius | Se o `TS_AUTH_KEY` vazar, o atacante tem acesso como `workshop` — o mesmo usuário que dá acesso manual ao servidor |
| Controle de acesso | Não é possível restringir comandos do CI/CD sem afetar o acesso manual (e vice-versa) |

### Solução

Criar um usuário Linux dedicado `ci-deploy` no servidor, usado **exclusivamente** pelo CI/CD. O usuário `workshop` continua disponível para acesso manual.

| Aspecto | Antes | Depois |
|---------|-------|--------|
| CI/CD usa | `workshop` | `ci-deploy` |
| Acesso manual usa | `workshop` | `workshop` |
| Audit logs | Misturados | Separados |
| Blast radius | `workshop` comprometido = acesso total | `ci-deploy` comprometido = apenas CI/CD |

### Tarefas

| # | Tarefa | Comando/ação | Dependência |
|---|--------|--------------|-------------|
| 15.1 | Criar usuário `ci-deploy` no servidor | `sudo useradd -m -s /bin/bash ci-deploy` | — |
| 15.2 | Adicionar `ci-deploy` ao grupo `workshop` | `sudo usermod -aG workshop ci-deploy` | 15.1 |
| 15.3 | Configurar `authorized_keys` para `ci-deploy` | Adicionar chave pública do deploy em `~ci-deploy/.ssh/authorized_keys` com `command="/usr/local/bin/deploy-wrapper.sh"` e flags `no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty` (mesmo template da Fase 0.3) | 15.1 |
| 15.4 | Ajustar ACL Tailscale | `users: ["workshop"]` → `users: ["ci-deploy"]` na regra SSH do Tailscale Admin Console | 15.2 |
| 15.5 | Atualizar `PROD_SSH_USER` no GitHub | `gh secret set PROD_SSH_USER --body "ci-deploy"` | 15.4 |
| 15.6 | Testar com workflow de teste | Executar `test-tailscale-ssh.yml` — SSH e SCP devem funcionar como `ci-deploy` | 15.5 |
| 15.7 | Testar acesso manual | `ssh workshop@TS_TAILSCALE_IP` — deve continuar funcionando como `workshop` | 15.4 |

### Comandos de referência

```bash
# No servidor:
sudo useradd -m -s /bin/bash ci-deploy
sudo usermod -aG workshop ci-deploy

# Criar authorized_keys para ci-deploy:
sudo mkdir -p /home/ci-deploy/.ssh
sudo cp /home/workshop/.ssh/authorized_keys /home/ci-deploy/.authorized_keys
# (ou adicionar nova chave pública do deploy)
sudo chown -R ci-deploy:ci-deploy /home/ci-deploy/.ssh
sudo chmod 700 /home/ci-deploy/.ssh
sudo chmod 600 /home/ci-deploy/.authorized_keys

# No Tailscale Admin Console (https://login.tailscale.com/admin/acls):
# Na regra SSH, trocar "users": ["workshop"] por "users": ["ci-deploy"]

# No GitHub:
gh secret set PROD_SSH_USER --body "ci-deploy"
```

### Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| CI/CD não funciona se `ci-deploy` não estiver configurado corretamente | Testar com `test-tailscale-ssh.yml` antes de merge |
| `ci-deploy` não tem permissões para comandos que o deploy precisa | Grupo `workshop` herda permissões; `deploy-wrapper.sh` controla whitelist |
| Se Tailscale cair, CI/CD não funciona (usando `ci-deploy`) | `workshop` mantido para acesso manual via Tailscale SSH; acesso físico ao servidor como contingência final |
| Auth key do Tailscale precisa ser renovada a cada 90 dias | Documentado na seção 12 do artigo; criar calendar reminder |

### Validação

- [ ] Usuário `ci-deploy` criado e no grupo `workshop`
- [ ] ACL Tailscale atualizada: `users: ["ci-deploy"]`
- [ ] `PROD_SSH_USER` atualizado no GitHub
- [ ] Workflow de teste passa (SSH + SCP como `ci-deploy`)
- [ ] Acesso manual como `workshop` continua funcionando
- [ ] Audit logs mostram `ci-deploy` para ações do CI/CD

---

## 11. Aprovações

| Papel | Nome | Data | Status |
|-------|------|------|--------|
| Mantenedor do projeto | — | — | ⏳ Pendente |
| Revisor de segurança | — | — | ⏳ Pendente |

---

## 12. Histórico de Revisões

| Versão | Data | Autor | Mudança |
|--------|------|-------|---------|
| 1.0 | 16/06/2026 | Eng. | Criação inicial do PRD |
| 1.1 | 16/06/2026 | Eng. | Expansão da seção 1.3 (estado atual detalhado do repo); reescrita da seção 5 (14 proteções em 3 frentes); atualização da seção 6 (+7 critérios de segurança AS9-AS15); atualização do cronograma (Fase 0.5 paralela) |
| 1.2 | 16/06/2026 | Eng. | Adição de 5 novas proteções (SECURITY.md, CodeQL, CI workflow, permissions blocks, webhook, 2º admin); seção 5 expandida para 19 proteções em 3 frentes; seção 6 com AS16-AS20; cronograma com Fase 0.6 |
| 1.3 | 17/06/2026 | Eng. | Reescrita das seções 3 e 4 para refletir servidor atrás de NAT (sem IP público, rede doméstica) + Cloudflare Tunnel como meio de transporte SSH entre runner hosted e servidor. Adicionada Fase 0.7 (instalação nativa do `cloudflared-ssh` com systemd) e ajustes nas seções 6.3, 7 e 9. Tunnels HTTP (gateway) permanecem em Docker no `docker-compose-gateway.yml`; tunnel SSH é exceção (binário nativo) por criticidade de deploy. |
| 1.4 | 17/06/2026 | Eng. | Refinamento da Fase 0.7: instalação do `cloudflared` via APT oficial Cloudflare (repo `noble main` para Ubuntu 24.04, caminho `/usr/bin/cloudflared`); criação de grupo `cloudflared` dedicado para permissões do credentials file (`User=nobody` + `Group=cloudflared` + `chmod 640`); adição da tarefa 0.7.12 (validação de boot após reboot). Unit systemd ajustada com diretivas de hardening (`NoNewPrivileges`, `ProtectSystem=strict`, `WatchdogSec=60`). |
| 1.5 | 19/06/2026 | Eng. | Revisão de fluxo com mantenedor. Mudanças: (1) Triggers revisados — `release/*` é a branch de release, tag `v*` em `release/*` dispara prod/gateway/observability; QA 100% manual via `workflow_dispatch`; `release/1.2.0` mantida como branch de trabalho manual. (2) Fase 0.7 — `cloudflared` provisionado em `~/.local/bin/` na máquina de dev (opção A); validação de boot substituída por `systemctl kill` + verificação de restart (sem reboot); credentials file permanece apenas no servidor (sem `CF_TUNNEL_TOKEN_SSH`). (3) Fase 1 — gate humana do env `production` aplicada já na Fase 1 (não na Fase 5); GHCR com tags `<git-tag>`, `sha-<7>`, `latest`; rollback via tag `:backup` no servidor + GHCR `latest`; sem `./gradlew test` no CI release por enquanto. (4) Fase 2 — `cloudflared-ssh-qa.service` separado; GHCR com tag `qa-<sha-7>` (sem `latest`); chave Ed25519 separada da prod; sem rollback; sem pgBackRest. (5) Fase 3 — imagem `workshop_rest_api-caddy-gateway` no GHCR (subpacote do owner); env `production` com required reviewers; reusa `PROD_SSH_KEY`; sem rollback. (6) Fase 4 — `paths:` filter removido; mesma gate humana do gateway; reusa `PROD_SSH_KEY`; validação simplificada (Promtail + labels em Loki como gate; ingestão como warning); `workflow_dispatch` default `ref: main`; `contents: read` apenas (sem `packages: write`); auditoria de bind de portas do `docker-compose-observability.yml` confirmou que nenhuma porta é publicada no host. (7) Fase 5 — item 5.0 novo: forçar runners hosted em PRs via Repository rulesets antes da remoção; item 5.3 opção B (remover tudo, sem backup). (8) Fase 6 — Trivy em modo fail desde o início (exit-code 1); scope filesystem; `shred` em cópias temporárias após auditoria; `concurrency:` apenas em prod; `timeout-minutes:` ajustados (build 20, deploy 45 com meta de reduzir, verify 30, cleanup 10). |
| 1.6 | 20/06/2026 | Eng. | Adição das camadas de teste L1 e L2. (1) Fase 0.6 reforçada com menção explícita à camada L1 (`ci.yml` valida `./gradlew test` em PRs e push em `main`). (2) Nova Fase 0.6.1 — workflow `build-image.yml` (camada L2) que isola build de imagem e push para GHCR, com smoke test efêmero (health check 200) e validação de JDK 21. (3) Nova seção 5.2.7 com conteúdo YAML completo do `build-image.yml` (actions pinadas por SHA, cache GHA, smoke test efêmero). (4) 4 novos critérios de aceitação (AC8-AC11) cobrindo L1 e L2. (5) Linha de risco na seção 7.1 reforçada com L2 como mitigação. (6) Cronograma atualizado: Fase 0.6.1 adicionada (15 min em paralelo no D+0); tempo total ativo ajustado para ~7.75h. |
| 1.7 | 20/06/2026 | Eng. | **(1) Correção factual:** domínio real é `eletroluk.com` (não `unspoken-tech.org`) — verificado em `infra/caddy/Caddyfile-gateway` e via DNS (NS do Cloudflare: `konnor.ns.cloudflare.com`, `nola.ns.cloudflare.com`; subdomínios `api.eletroluk.com`, `api-qa.eletroluk.com`, `grafana.eletroluk.com` já em produção). Substituição aplicada em 18+ ocorrências. **(2) Migração de Tunnel TCP → Tunnel HTTP + Cloudflare Zero Trust Free:** o projeto roda no Free tier (não tem Pro+), portanto o transporte SSH original (que exigiria Pro+) foi substituído por **Cloudflare Tunnel HTTP + Cloudflare Zero Trust** com autenticação por **Service Token**. Diff: Fase 0.7 inteira reescrita (Bloco A com 2 tunnels, Bloco B com 2 systemd services, Bloco C com 2 Self-hosted Applications + 1 Service Token compartilhado); diagrama da arquitetura (3.1) atualizado para refletir transporte HTTPS; tabela de secrets (3.4) cresceu de 8 para **10** (adição de `CF_ACCESS_CLIENT_ID` e `CF_ACCESS_CLIENT_SECRET`); workflows passam a incluir step de `cloudflared access ssh --listener localhost:2222` (prod) / `localhost:2223` (QA) antes do `appleboy/ssh-action`. **(3) Hardening adicional:** mitigação `command="..."` em `authorized_keys` (via `/usr/local/bin/deploy-wrapper.sh`) **promovida de "opcional" para obrigatória** na Fase 0.3 — mitiga o risco residual de vazar `QA_SSH_KEY` dar shell no host de prod. **(4) Inventário de secrets órfãos** adicionado na Fase 0.5 e remoção programada na Fase 5.5 (4 secrets atuais: `SSH_PRIVATE_KEY`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSPHRASE`). **(5) Concurrency** adicionada também no `deploy-qa.yml` (Fase 2). **(6) Rollback** via GHCR `:latest` corrigido conceitualmente em AC5 — agora é via tag `:backup` local no servidor (GHCR `:latest` é sobrescrita e perderia referência). **(7) Trivy** em endurecimento gradual: inicia em modo relatório (`--exit-code 0`) e endurece para `--exit-code 1` após baseline. **(8) AC4** corrigido: loop `90 × 5s = 7,5 min`, meta `≤ 5 min` (corrige inconsistência numérica de v1.6). **(9) Métrica de custo** corrigida: "ilimitado" → "dentro da quota gratuita mensal de jun/2026" (atualiza referência desatualizada). **(10) Pinning de `cloudflared` no runner** documentado em 3.6 (versão + SHA256). **(11) CodeQL** com SHAs distintos por sub-action (corrige bug de v1.6 que usava o mesmo SHA para 3 sub-actions). **(12) Dependabot** com `package-ecosystem: "docker"` adicional. **(13) `attestations: write`** adicionado como follow-up da Fase 6 (SLSA build provenance). **(14) DNSSEC** em `eletroluk.com` adicionado como follow-up da Fase 6 (atualmente `delegationSigned: false`). **(15) Pre-requisitos B (NEW)** adicionados ao cronograma: 15 min paralelos para confirmar CF Free + Zero Trust org + tunnels Docker existentes. **(16) Cronograma** atualizado para v1.7: ~8,25h ativas (+30 min na Fase 0.7; −30 min na Fase 2 pelo reaproveitamento do tunnel QA). |
| 1.8 | 21/06/2026 | Eng. | **(1) Implementação da Fase 0.7 concluída:** tunnel SSH `workshop-ssh` (ID: `d5bda623-2bb9-4a66-8f47-83f8b36dd7ee`) criado e funcionando. Serviço `cloudflared-ssh.service` ativo com 4 conexões QUIC ao Edge. **(2) Correção crítica no ingress:** mudança de `service: http_status:200` para `service: ssh://localhost:22` no `config-ssh.yml` — resolveu o erro `websocket: bad handshake`. **(3) Bug cloudflared 2026.6.0:** versão 2026.6.0 ignora Service Tokens para SSH (issue #1673); downgrade para 2026.5.1. **(4) Aplicação Infrastructure incompatível:** requer IP (servidor atrás de NAT); decisão: manter Self-hosted. **(5) Smoke test OK:** `ssh -p 2222 workshop@localhost` retorna `SSH via Cloudflare Access OK`. **(6) PRD atualizado:** seção 0 (registro de progresso), tarefas 0.7.3a/0.7.3b, template `config-ssh.yml`, e esta linha de histórico. |
| 1.9 | 22/06/2026 | Eng. | **Fase 0.3 (produção) concluída:** chaves SSH Ed25519 geradas (`deploy_key_prod`, `deploy_key_qa`); `deploy_key_prod.pub` configurada no `~/.ssh/authorized_keys` do servidor com `command="/usr/local/bin/deploy-wrapper.sh"` e flags `no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty`; `deploy-wrapper.sh` criado e executando; conexão SSH via Cloudflare Access validada (`ssh -i ~/.ssh/deploy_key_prod -p 2222 workshop@localhost 'echo OK'` retorna `OK`). Pendência menor: permissão de `/var/log/deploy-wrapper.log`. Chave QA (`deploy_key_qa`) adiada. |
| 1.10 | 22/06/2026 | Eng. | **Fase 0.11 (produção) concluída:** 9 de 10 secrets adicionados via `gh secret set`. Produção completa: `PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_USER`, `PROD_SSH_KEY`, `CF_ACCESS_CLIENT_ID`, `CF_ACCESS_CLIENT_SECRET`. QA parcial: `QA_SSH_HOST`, `QA_SSH_PORT`, `QA_SSH_USER` adicionados; `QA_SSH_KEY` pendente (chave não gerada). |
| 1.11 | 22/06/2026 | Eng. | **Fase 0.13 (produção) concluída:** validação de restart do `cloudflared-ssh.service` via `systemctl kill` — `Restart=always` funcionou, restart counter=222, reconexão ao Edge em 6 segundos, SSH pós-restart validado (`OK`). Fases 0.7, 0.3, 0.11 e 0.13 (produção) todas concluídas. Próximo marco: Fase 1 (migração do `deploy.yml`). |
| 1.12 | 22/06/2026 | Eng. | **Auditoria de SHAs (seção 3.6):** 3 dos 6 SHAs de actions listados divergiam dos valores reais publicados no GitHub em jun/2026. SHAs corrigidos: `actions/setup-java` v4.1.0 (`c5195...` → `9704b...`), `docker/build-push-action` v6.0.0 (`05658...` → `c382f...`), `docker/login-action` v3.2.0 (`5cd0f...` → `0d4c9...`). Adicionado SHA de `appleboy/ssh-action` v1.0.3 (`029f5...`) — antes era apenas "pinar via tag". Validação via `curl https://api.github.com/repos/<owner>/<repo>/git/refs/tags/<tag>`. Adicionadas tarefas 0.6.1 (script `scripts/audit-action-shas.sh`) e 0.6.2 (`.github/dependabot.yml`) para automação de auditorias futuras. Adendo 3.6.1 documenta 3 métodos de encontrar/validar SHAs. |
| 1.13 | 22/06/2026 | Eng. | **Migração do transporte CI/CD de Cloudflare Access SSH para Tailscale.** Problema: Service Tokens do Cloudflare Zero Trust não encaminham o username de forma consistente do runner para o servidor; erros intermitentes (`username is empty`, `websocket: bad handshake`, `error in libcrypto`) impediram deploys confiáveis. Solução: `tailscale/github-action@v4` cria nós efêmeros na rede mesh via WireGuard, conectando ao servidor sem port mapping. Mudanças: (1) Seção 0.14 — novo registro de progresso documentando o problema e a solução. (2) Seção 1.1 — parágrafo adicionado sobre a mudança de transporte. (3) Seção 3.1 — diagrama de arquitetura reescrito com Tailscale (WireGuard) no lugar de Cloudflare Access SSH (HTTPS). (4) Seção 3.2 — tabela de decisões técnicas atualizada: conectividade → Tailscale, auth → OIDC + WireGuard + SSH key; adicionada linha do Tailscale GitHub Action. (5) Seção 3.4 — secrets remapeados: removidos `CF_ACCESS_CLIENT_ID/SECRET`, adicionados `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `TS_TAILSCALE_IP`; total de novos reduzido de 10 para 7. (6) Seção 0.7 — nota adicionada indicando que o tunnel SSH continua para acesso manual mas CI/CD usa Tailscale. (7) Seção 4 — Fase 1 atualizada: step de `tailscale/github-action@v4` substitui `cloudflared access ssh --listener`; workflows deploy-gateway e deploy-qa e deploy-observability atualizados. (8) Seção 7 — novo risco "Limite do plano Tailscale Personal" com mitigação; risco "Service Token vaza" removido/relevância reduzida. (9) Cloudflare tunnel SSH (`cloudflared-ssh.service`) mantido como contingência e para acesso manual — não revertido. |
| 1.14 | 23/06/2026 | Eng. | **Implementação da Fase 1 — `deploy.yml` reescrito com Tailscale.** Workflow `deploy.yml` migrado de Cloudflare Access SSH para Tailscale. Mudanças: (1) Removidos env vars `CLOUDFLARED_VERSION`, `CLOUDFLARED_SHA256`, `SSH_HOST`. (2) Job `deploy`: step "Setup cloudflared + SSH" substituído por `tailscale/github-action@v4` (SHA `306e68a...` v4.1.2) + step "Setup SSH key" simplificado (sem ProxyCommand). (3) Jobs `verify` e `cleanup`: mesmo padrão — Tailscale action + SSH key setup. (4) `SSH_TARGET` muda de `workshop@ssh.eletroluk.com` para `workshop@${{ secrets.TS_TAILSCALE_IP }}`. (5) Adicionado `-o StrictHostKeyChecking=no` nos comandos SSH/SCP. (6) Secrets confirmados: `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `TS_TAILSCALE_IP` criados via `gh secret set`. (7) Plano Tailscale Personal validado: 6 users, 1.000 ephemeral min/mês (suficiente para ~200 deploys). (8) ACL Tailscale configurado: `autogroup:member → autogroup:self` (network + SSH). (9) Tag `tag:ci` configurada com owner `autogroup:member`. |
| 1.15 | 23/06/2026 | Eng. | **Adendo v1.15 — Usuário dedicado para CI/CD via Tailscale SSH.** (1) Justificativa: `workshop` é usado para acesso manual e CI/CD, misturando audit logs e blast radius. (2) Solução: criar usuário `ci-deploy` dedicado ao CI/CD, mantendo `workshop` para acesso manual. (3) Tarefas: criar `ci-deploy` no servidor, adicionar ao grupo `workshop`, ajustar ACL Tailscale (`users: ["ci-deploy"]`), atualizar `PROD_SSH_USER` no GitHub, testar com `test-tailscale-ssh.yml`. (4) Risco: se Tailscale cair, CI/CD não funciona — mitigado mantendo `workshop` + Cloudflare tunnel para acesso manual. (5) Checklist de validação adicionado no adendo. |
| 1.16 | 23/06/2026 | Eng. | **Remoção do cloudflared-ssh do servidor.** (1) `cloudflared-ssh.service` removido — tunnel SSH nativo não é mais necessário (CI/CD e acesso manual usam Tailscale). (2) Fase 0.7 marcada como "descontinuada" no PRD. (3) Containers Docker HTTP (`cloudflared-prod`, `cloudflared-qa`) permanecem ativos como proxy reverso para endpoints web. (4) Secrets `CF_ACCESS_CLIENT_ID` e `CF_ACCESS_CLIENT_SECRET` removidos (Service Token obsoleto). (5) Diagrama de arquitetura atualizado — bloco Cloudflare tunnel SSH removido. (6) Riscos do cloudflared-ssh removidos; novo risco "Tailscale não reconecta após reboot" adicionado. (7) `deploy.yml` — trigger corrigido (apenas tags, sem `branches`); step "Deploy via SSH" separado em 3 steps (Prepare, Upload, Execute). (8) Tailscale resiliente a reboot: módulo `tun` configurado em `/etc/modules-load.d/tun.conf`; `tailscaled` habilitado no systemd. |
| 1.17 | 23/06/2026 | Eng. | **Tailscale SSH habilitado — chaves SSH e deploy-wrapper.sh removidos.** (1) `tailscale up --ssh` habilitado no servidor — autenticação SSH via identidade Tailscale (OIDC), não via `authorized_keys`. (2) `deploy-wrapper.sh` removido do servidor — sem `command=` no `authorized_keys`, o wrapper nunca é invocado. (3) Linhas das chaves públicas (`deploy_key_prod.pub`, `deploy_key_qa.pub`) removidas do `authorized_keys`. (4) Secrets `PROD_SSH_KEY`, `QA_SSH_KEY`, `PROD_SSH_HOST`, `PROD_SSH_PORT`, `QA_SSH_HOST`, `QA_SSH_PORT` marcados como **obsoletos** — podem ser removidos do GitHub. (5) Apenas `PROD_SSH_USER` e `QA_SSH_USER` continuam necessários. (6) Fase 0.3 marcada como "descontinuada" no PRD. (7) Fase 0.11 revisada — 6 de 8 secrets SSH obsoletos. (8) Segurança do CI/CD agora depende 100% do Tailscale ACL (sem segunda camada de autenticação SSH). (9) Configuração atual do servidor atualizada (seção 0). (10) Próximos passos atualizados com lista de pendências. |
| 1.18 | 23/06/2026 | Eng. | **Deploy.yml validado end-to-end com GHCR pull+retag, concurrency e timeouts.** (1) **GHCR login no servidor** via `export GH_USER/GH_TOKEN` no SSH — Padrão 1 da indústria (não requer PAT separado; `GITHUB_TOKEN` de curta duração). (2) **GHCR pull por SHA tag** — imagem puxada por `sha-<full-commit>` (tag imutável) + retag para nome local do compose (Opção B — não altera `docker-compose-production.yml`). (3) **`packages: read`** adicionado ao deploy job — necessário para `docker pull` via `GITHUB_TOKEN`. (4) **`workflow_dispatch` ref** — checkout e image tag usam `github.event.inputs.ref || github.ref`. (5) **Concurrency** — `group: production, cancel-in-progress: false` (deploys em fila). (6) **Timeouts** — build: 20min, deploy: 45min, verify: 30min, cleanup: 10min. (7) **Teste end-to-end** — tag `v1.3.0-rc-test` validada com sucesso: build (1m45s), deploy (1m4s), verify (19s), cleanup (16s). (8) **appleboy/ssh-action removido** — deploy usa `ssh`/`scp` direto (sem dependência de terceiros). (9) **tailscale/github-action** adicionado à tabela de SHAs (seção 3.6). (10) Seções 3.2, 3.3.1, 3.4, 3.5, 3.6 atualizadas. (11) Próximos passos: itens 3 e 4 marcados como concluídos. |
| 1.19 | 23/06/2026 | Eng. | **Docker layer cache (GHA) e correção de script injection.** (1) **`docker/setup-buildx-action@v3.11.0`** adicionado ao `deploy.yml` — habilita BuildKit necessário para cache de layers; SHA pinnado (`18ce135...`). (2) **Cache GHA** — `cache-from: type=gha` + `cache-to: type=gha,mode=max` no `docker/build-push-action` — reutiliza layers entre builds; `mode=max` salva todas as layers incluindo intermediate stages; limite de 10 GB por repo na GitHub Actions cache. (3) **Script injection corrigido** — 3 steps do `deploy.yml` tinham expressões `${{ }}` com valores user-controllable diretamente em `run:` blocks (vetor de ataque: injetar shell via ref malicioso). Todas movidas para `env:` blocks: "Prepare image tag" (`github.event.inputs.ref`), "Show build info" (`github.ref_name`, `github.sha`, `github.actor`, `github.event_name`), "Cleanup Summary" (mesmos campos). (4) Seções 3.2, 3.3.1, 3.6, 7.1 atualizadas. (5) Cache validado com sucesso via `workflow_dispatch` (2 execuções consecutivas, layers `CACHED` na 2ª execução). |
| 1.20 | 23/06/2026 | Eng. | **Harden-Runner (StepSecurity) em modo audit.** (1) **`step-security/harden-runner@v2.12.1`** adicionado como primeiro step em todos os 4 jobs do `deploy.yml` (build, deploy, verify, cleanup) — SHA pinnado (`002fdce...`). (2) **`egress-policy: audit`** — monitora tráfego de rede (egress), arquivos escritos e processos executados sem bloquear nada. (3) **Objetivo:** criar baseline do comportamento normal do workflow para futura migração para `block` mode com allowlist de domínios. (4) Dashboard StepSecurity mostrará domínios acessados (GHCR, GitHub API, Tailscale controlplane, Docker Hub) após 1-2 deploys. (5) Seções 3.3.1 e 3.6 atualizadas. |
