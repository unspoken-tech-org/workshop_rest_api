# Observabilidade local

Laboratório local, sem credenciais reais ou integrações externas, que simula o
Collector de borda da produção: recebe OTLP, raspa a API com identidade de
leitura mínima e encaminha tudo por OTLP a Loki, Prometheus e Tempo.

```text
API (container) ──scrape autenticado──► Collector ──OTLP──► Prometheus 3
API (Java Agent) ──OTLP traces───────► Collector ──OTLP──► Tempo 3
containers com label ─► docker-socket-proxy ◄─ Alloy ──OTLP──► Collector ──OTLP──► Loki
```

## Pré-requisito: segredos locais

Os valores ficam em `.secrets/` (ignorado pelo Git) e são gerados uma única
vez, sem serem impressos: senha de scrape e seu hash bcrypt (a API guarda só o
hash), senha do banco sintético e a API key sintética. Para rotacionar, apague
o arquivo correspondente (senha e hash juntos) e rode de novo:

```bash
platform/observability/local/scripts/init-secrets.sh
```

O perfil `api` também monta o OpenTelemetry Java Agent, baixado uma única vez
para `.javaagent/` (ignorado pelo Git) com versão e checksum SHA-256 fixados no
script; um arquivo com checksum divergente é recusado:

```bash
platform/observability/local/scripts/fetch-javaagent.sh
```

## Subir e parar

Na raiz de `workshop_rest_api`. O perfil `api` constrói e sobe a API em
container (perfil Spring `qa`, logs JSON iguais a QA/PRD, label de coleta do
Alloy) sobre um PostgreSQL sintético em `tmpfs`; o seed cria a API key
sintética depois que o Flyway da API criou o schema:

```bash
docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml --profile api up -d --build --wait
docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml --profile api run --rm workshop-api-seed
docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml --profile api down
```

O banco sintético não tem volume: cada `down` (ou recriação do container)
descarta todos os dados. Nunca restaure backup ou cópia de produção nele; a
jornada sentinela da Fase 0.5 usa somente esta base (R23). A API fica em
`http://127.0.0.1:8080` (`API_LOCAL_PORT` muda a porta); a key fica em
`.secrets/synthetic-api-key` e se vincula ao primeiro `boundDeviceId` usado.

Com o agent, a API exporta somente traces (OTLP/HTTP para o Collector);
métricas continuam pelo scrape e logs pelo stdout JSON coletado pelo Alloy. A
configuração comum fica em `apps/api/otel/javaagent.properties`; o Compose
informa só endpoint e atributos de recurso do ambiente.

Sem `--profile api` sobe só a stack de observabilidade. Para raspar uma API
rodando no host (`bootRun`), use `API_SCRAPE_TARGET=host.docker.internal:8080`
e exporte o conteúdo de `.secrets/api-scrape-password-hash` como
`OBSERVABILITY_SCRAPE_PASSWORD_HASH`; o firewall do host pode bloquear esse
caminho (R21), por isso o perfil `api` é o padrão.

As interfaces web ficam somente no loopback: Grafana em `http://127.0.0.1:3000`,
Prometheus em `http://127.0.0.1:9090`, Loki em `http://127.0.0.1:3100`, Tempo em
`http://127.0.0.1:3200` e Alertmanager em `http://127.0.0.1:9093`. O Grafana é
somente leitura/anônimo por padrão para não armazenar segredo no repositório.

O dashboard `Workshop API` (`grafana/provisioning/dashboards`) é provisionado
como código: alterações pela interface não são salvas. Ele mostra RED da API,
JVM/Hikari, logs WARN/ERROR, traces e a fila do Collector, com annotation de
cada início da API. O Viewer anônimo não acessa o Explore, então o trace de um
log ou da tabela abre no próprio dashboard (variável `trace_id`).

Instrumentações no host podem enviar OTLP para `127.0.0.1:4317` (gRPC) ou
`127.0.0.1:4318` (HTTP). O Collector remove credenciais, headers HTTP, URLs e
caminhos concretos, query strings e statements SQL; preserva `http.route` e
`service.instance.id` (que não vira label de métrica nem índice do Loki). É
defesa em profundidade: a API continua sendo a barreira principal.

## Coleta de logs

O Alloy não monta o `docker.sock`: acessa a API Docker pelo
`docker-socket-proxy`, somente leitura (containers e redes). Só containers com
o label abaixo são coletados (allowlist); o nome do serviço vem do segundo
label ou do nome do container:

```bash
docker run --label com.workshop.observability.logs=true \
           --label com.workshop.observability.service=meu-servico ...
```

A máscara do Alloy cobre `chave=valor`, `Chave: valor`, JSON e `Bearer <token>`.

## Healthchecks

Os healthchecks verificam prontidão real, não só o arquivo de configuração.
Collector e Loki usam imagens sem cliente HTTP; `otel-collector-probe` e
`loki-probe` fazem a verificação (`/` da extensão `health_check` e `/ready`) e
são as dependências dos demais serviços. Healthcheck verde não substitui o
canário: cada pipeline deve ser comprovado por consulta.

## Fila persistente

Cada exporter do Collector tem fila em disco (`file_storage`, volume
`otel_collector_queue`) com limite em bytes (`COLLECTOR_QUEUE_BYTES`, 256 MiB
por padrão) e retry sem prazo: backend fora do ar não perde o que já foi aceito,
e a fila sobrevive a reinícios do Collector. Cheia, a fila recusa dados novos e
a recusa chega a quem enviou (agent, Alloy), sem bloquear a API. Como o retry
não expira, a queda aparece como fila que não esvazia
(`OTelCollectorQueueBacklog`), não como falha de envio. Recusado pelo
Collector, o Alloy descarta o log em definitivo (`AlloyLogsDropped`). Para
ensaiar a fila cheia, suba o Collector com um limite pequeno:

```bash
COLLECTOR_QUEUE_BYTES=65536 docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml --profile api up -d --no-deps --force-recreate otel-collector
```

## Validar sem subir a stack

```bash
docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml config --quiet
docker run --rm -e API_SCRAPE_USERNAME=u -e API_SCRAPE_TARGET=h:1 -e COLLECTOR_QUEUE_BYTES=1 \
  --tmpfs /var/lib/otelcol/file_storage:uid=10001 \
  -v "$PWD/platform/observability/local/collector/config.yaml:/etc/otelcol-contrib/config.yaml:ro" \
  otel/opentelemetry-collector-contrib:0.161.0 \
  validate --config=file:/etc/otelcol-contrib/config.yaml
docker run --rm \
  -v "$PWD/platform/observability/local/alloy/config.alloy:/etc/alloy/config.alloy:ro" \
  grafana/alloy:v1.20.1 validate /etc/alloy/config.alloy
docker run --rm \
  -v "$PWD/platform/observability/local/loki/config.yaml:/etc/loki/config.yaml:ro" \
  grafana/loki:3.7.8 -config.file=/etc/loki/config.yaml -verify-config
docker run --rm --entrypoint promtool -w /etc/prometheus \
  -v "$PWD/platform/observability/local/prometheus:/etc/prometheus:ro" \
  prom/prometheus:v3.13.4 test rules tests/observability_test.yml
docker run --rm --entrypoint amtool \
  -v "$PWD/platform/observability/local/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" \
  prom/alertmanager:v0.34.1 check-config /etc/alertmanager/alertmanager.yml
```

Tempo 3 e Prometheus 3 usam volumes novos (`tempo3_data`, `prometheus3_data`):
não há downgrade suportado do formato de dados. Os volumes 2.x antigos ficam
preservados para retorno até decisão explícita de remoção.

Não há receiver externo no Alertmanager. Antes de qualquer uso fora da máquina
local, adicione autenticação/TLS, retenção e uma integração de notificações em
um ambiente de implantação próprio; não coloque tokens neste diretório.
