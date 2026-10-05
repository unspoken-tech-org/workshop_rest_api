# Borda Cloudflare de observabilidade

Esta configuração controla somente o Tunnel remoto `observability-qa`, o
registro DNS do Grafana e o Cloudflare Access do Grafana. Ela não gerencia a VM,
o Compose ou o Caddy; esses componentes precisam consumir os contratos abaixo.

## Contratos de origem

- `grafana-obs.eletroluk.com` (nome temporário, R8) é CNAME para o hostname
  `<tunnel-id>.cfargotunnel.com` do Tunnel. Não há rota pública de telemetria:
  o OTLP chega à QA só pela rede privada, e o Flutter usará a entrada HTTPS da
  API de produção.
- O hostname chega ao Caddy dedicado da VM de QA por
  `http://caddy:8080` por padrão (serviço `caddy` de `platform/observability/control-plane/compose.yml`). Sobrescreva
  `caddy_origin_service` se o nome do serviço no Compose for diferente. O
  `cloudflared` e esse Caddy devem compartilhar uma rede privada; não abra o
  serviço diretamente na Internet.
- Grafana aceita todos os caminhos do próprio hostname; qualquer outro hostname
  recebe `404` no Tunnel.
- A lista `grafana_access_emails` é obrigatória e não tem valor padrão. A
  política é uma allowlist explícita; não há regra `everyone`. O Access deve
  continuar sendo a única porta pública administrativa do Grafana.

## Identificadores e credencial da API

`cloudflare_account_id`, `cloudflare_zone_id` e `grafana_access_emails` são
entradas obrigatórias fornecidas pelo ambiente confiável. Nenhum ID real,
e-mail ou segredo é versionado.

O segredo de CI deve se chamar `OBS_CF_API_TOKEN`. Antes de executar Terraform,
o ambiente confiável deve apenas mapeá-lo para a variável Terraform, sem
imprimir o valor:

```sh
export TF_VAR_obs_cf_api_token="$OBS_CF_API_TOKEN"
```

Use um token dedicado, nunca uma Global API Key nem um token do gateway de QA ou
produção. O escopo mínimo é:

- `Cloudflare Tunnel: Edit` no account que hospeda o Tunnel;
- `Access: Apps and Policies: Edit` no mesmo account;
- `DNS: Edit` somente para a zona `eletroluk.com`.

O provider v5 usa `cloudflare_dns_record`,
`cloudflare_zero_trust_tunnel_cloudflared*` e os recursos
`cloudflare_zero_trust_access_*`; não use os nomes legados do provider v4.

## Backend e state

O state fica num bucket R2 privado. O bloco `backend "s3"` guarda só os
ajustes fixos do R2 (`region = "auto"`, `use_path_style`, `skip_*`) e o lock
por `use_lockfile` (Terraform >= 1.10); `bucket`, `key` e o endpoint
`https://<account-id>.r2.cloudflarestorage.com` vêm dos segredos
`OBS_TF_STATE_*` no workflow. O R2 não versiona objetos: o workflow copia o
state para `snapshots/` no mesmo bucket antes de cada `apply`. O backend não
pode ser público.

O provider trata a configuração de ingress do Tunnel como update-only: ela não
é removida por `terraform destroy`. A aposentadoria do Tunnel exige uma limpeza
manual aprovada e registrada.

Não versione `backend` config, `.terraform/`, state, `*.tfvars`, planos ou
certificados. Para a checagem local sem credencial use:

```sh
terraform init -backend=false
terraform fmt -check
terraform validate
```

## Token do Tunnel após o apply

O Tunnel é criado remotamente sem `tunnel_secret` no código. O data source
`cloudflare_zero_trust_tunnel_cloudflared_token` produz a saída sensível
`tunnel_token` depois que o recurso existe. O deploy do plano de controle lê
essa saída do state com uma credencial R2 somente leitura
(`OBS_TF_STATE_RO_*`), mascara o valor e o grava apenas no `.env` remoto usado
como `CLOUDFLARE_TUNNEL_TOKEN`; ele não vira segredo do GitHub nem aparece em
logs ou artefatos. Como o token aparece no state, o backend privado é
obrigatório.
