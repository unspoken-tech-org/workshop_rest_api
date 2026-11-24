# Docker: `default-address-pools` e `live-restore` nos hosts de QA e PRD

**Status:** preparado, NÃO aplicado. Aplicar só na janela da próxima release.
**Arquivos:** `platform/host/docker/daemon.json` (referência) e
`scripts/host/apply-docker-daemon-config.sh` (aplicação idempotente).

## Por que

Os dois hosts usam o pool padrão do Docker (`172.17.0.0/16` até `172.31.0.0/16`,
máscara /16, 15 faixas) e as 15 já estão ocupadas (verificado só com leitura em
24/11/2026):

| Host | Faixas 172.x em uso | LAN / outras |
|---|---|---|
| PRD (`workshop-local`) | 172.17 a 172.31, todas (172.20 e 172.21 órfãs, em remoção) | `eno1` 192.168.3.0/24 (gateway 192.168.3.1), `tailscale0` 100.64.0.0/10 |
| QA (`100.79.194.54`) | 172.17 a 172.31, todas (inclusive 172.31, usada por `workshop-app`) | `enp0s6` 10.0.0.0/24 (gateway 10.0.0.1), `tailscale0` |

Quando o pool padrão acaba, o Docker cai para `192.168.0.0/20`, que no PRD
sobrepõe a LAN `192.168.3.0/24`: a rota do bridge novo roubaria o tráfego da LAN
(SSH local, gateway). Na QA o fallback também seria problemático.

## O que muda

```json
"default-address-pools": [{ "base": "10.200.0.0/16", "size": 24 }],
"live-restore": true
```

- Pool `10.200.0.0/16` em fatias /24: 256 redes novas, 253 IPs úteis cada.
- **Não colide:** PRD só tem 127/8, 192.168.3.0/24, 100.x e 172.17-31; QA só tem
  127/8, 10.0.0.0/24 (`enp0s6`, a `10.200` fica fora dela), 100.x, 169.254/16 e
  172.17-31. Nenhum dos dois tem interface, rota ou rede Docker em `10.200.0.0/16`.
  Reconfira na janela (pré-checagem 3).
- Chaves atuais (`storage-driver`, `log-driver`, `log-opts`) preservadas.
- Sem `bip`: `docker0` (172.17.0.0/16) não é usada por nenhum stack; mexer nela
  não ganha nada e exigiria mais uma troca no restart.
- `live-restore: true`: em restarts futuros do daemon os containers continuam
  rodando. Limitações: não funciona com Swarm (`Swarm: inactive` nos dois hosts,
  ok); durante o restart o daemon não gerencia redes nem healthchecks; a troca de
  versão major do Docker ainda pode exigir parar containers. A chave é recarregável
  por SIGHUP, mas `default-address-pools` não, então a mesma restart cobre as duas.
  **O restart que ativa o live-restore ainda derruba os containers**, pois o
  daemon em execução está com `Live Restore Enabled: false`.

### Impacto

- `systemctl restart docker` derruba **todos** os containers do host
  (API, banco, gateway, observabilidade) até subirem de novo (`restart:` das
  stacks). Há indisponibilidade; faça na janela.
- Redes existentes **mantêm** as subnets atuais; só redes novas (sem subnet
  explícita) usam o pool `10.200.x.0/24`.
- **Risco no gateway (atenção):** `platform/gateway/caddy/Caddyfile-gateway` e
  `Caddyfile-gateway-qa` confiam como proxy só `172.16.0.0/12` (rede
  `workshop-ingress`, onde fica o cloudflared) e dizem que `10.0.0.0/8` NÃO é
  confiável. Se a `workshop-ingress` for **removida e recriada** depois da mudança
  (`docker compose down`, `docker network rm`, mudança de config da rede), ela
  nasce em `10.200.x.0/24`, fora do `trusted_proxies`: o Caddy deixa de aceitar o
  `Cf-Connecting-Ip`, o `client_ip` vira o do cloudflared e o rate limit passa a
  tratar todo mundo como um só cliente. O restart do Docker por si só não recria
  redes. Antes de aplicar, decidir e mergear uma das opções: (a) fixar `ipam` da
  `workshop-ingress` em uma /24 dentro de `172.16.0.0/12` nos compose do gateway
  (PRD e QA; recria a rede, fazer na janela do gateway), ou (b) acrescentar
  `10.200.0.0/16` ao `trusted_proxies` conscientemente. Até lá, não rode
  cleanup/deploy do gateway que derrube a rede entre o restart e a decisão.
- Fora o gateway, nenhum outro IP 172.x fixo (allowlist, `pg_hba.conf`) foi
  encontrado em `platform/` e `apps/`.

## Pré-checagens (cada host, QA primeiro)

1. Janela combinada, backup do banco recente (pgBackRest em PRD) e release
   pronta para deploy logo depois.
2. `cat /etc/docker/daemon.json` igual ao esperado (só as 3 chaves atuais).
3. Sem colisão: `ip -4 addr; ip route; docker network ls -q | xargs docker network inspect -f '{{.Name}} {{range .IPAM.Config}}{{.Subnet}} {{end}}' | grep -E ' 10\.'` não pode mostrar `10.200`.
4. `docker info | grep -iE 'swarm|live restore'` mostra `Swarm: inactive`.
5. Anotar `docker ps --format '{{.Names}} {{.Status}}'` (linha de base).
6. `jq --version` e `dockerd --version` presentes (ambos presentes nos dois hosts; Docker 29.9.3).
7. Decisão sobre a `workshop-ingress` (item de risco do gateway acima) tomada.

## Passos

No host, com o repositório na `main` (ou o checkout do tag da release):

```bash
# 1. dry-run (padrão): mostra o diff e valida, sem gravar
scripts/host/apply-docker-daemon-config.sh

# 2. gravar (faz backup /etc/docker/daemon.json.bak-AAAAMMDD-HHMMSS)
sudo scripts/host/apply-docker-daemon-config.sh --apply

# 3. reiniciar (DERRUBA os containers)
sudo systemctl restart docker
```

Ordem: **QA, verificar, e só então PRD**. Em PRD, avise antes e acompanhe o
Grafana/Healthchecks. O script não reinicia nada sozinho.

## Verificação pós-restart

1. `docker info | grep -iE 'live restore'` -> `Live Restore Enabled: true`.
2. `docker ps --format '{{.Names}} {{.Status}}'`: todos os stacks `Up`/`healthy`,
   igual à linha de base. Containers sem `restart:` não voltam sozinhos: suba pelo
   deploy do stack correspondente.
3. Rodar os `verify.sh` dos stacks do host (`.github/scripts/<stack>/verify.sh`),
   ou o deploy da release, que já os executa.
4. Rede de teste:
   ```bash
   docker network create zz-pool-test
   docker network inspect zz-pool-test -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}'  # 10.200.0.0/24
   docker network rm zz-pool-test
   ```
5. Redes antigas continuam com as subnets de antes.

## Rollback

```bash
sudo scripts/host/apply-docker-daemon-config.sh --rollback          # backup mais recente
sudo scripts/host/apply-docker-daemon-config.sh --rollback /etc/docker/daemon.json.bak-AAAAMMDD-HHMMSS
sudo systemctl restart docker
```

Redes criadas durante o intervalo em `10.200.x` continuam funcionando enquanto
existirem. Se o Docker não subir: `journalctl -u docker -n 50`, restaurar o backup
manualmente (`cp -p`) e reiniciar.

## Alternativa sem restart: subnet explícita nos compose

Em cada rede nova de um compose, fixar a subnet evita o pool do daemon:

```yaml
networks:
  minha-rede:
    ipam:
      config:
        - subnet: 10.201.10.0/24
```

Vale como mitigação para redes novas antes da janela, mas não substitui o pool: é
preciso manter um registro de faixas por stack para não repetir subnets, e uma
rede esquecida sem `ipam` volta ao fallback `192.168.0.0/20`. Hoje nenhum compose
do repositório declara `ipam` (todas as redes herdam o pool). Recomendação: **não**
alterar os compose existentes agora (recriaria as redes e derrubaria os serviços
fora da janela); aplicar o pool e, nas redes **novas**, usar subnet explícita só
quando houver motivo específico. Faixa reservada para isso: `10.201.0.0/16`.

## Referências

- Docker: [daemon configuration](https://docs.docker.com/reference/cli/dockerd/), [live restore](https://docs.docker.com/engine/containers/live-restore/).
