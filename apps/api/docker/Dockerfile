# Stage 1: Build da aplicação
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Este Dockerfile é usado com o contexto da raiz do monorepo
# (docker build -f apps/api/docker/Dockerfile .).
# Copia o wrapper e os dois projetos Gradle para manter o build configurado.
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY apps/api/build.gradle apps/api/build.gradle
COPY apps/security/build.gradle apps/security/build.gradle

# Garante permissão de execução no wrapper e pré-baixa dependências
RUN chmod +x gradlew && ./gradlew :apps:api:dependencies --no-daemon || true

# Copia apenas o código fonte principal da API
COPY apps/api/src/main apps/api/src/main

# Compila o JAR executável da aplicação e separa o Boot JAR do artefato
# `-plain`, sem acoplar a imagem a uma versão específica.
RUN ./gradlew :apps:api:bootJar --no-daemon -x test && \
    boot_jar="$(find apps/api/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" && \
    test -n "$boot_jar" && cp "$boot_jar" /app/workshop.jar

# Stage 2: OpenTelemetry Java Agent, baixado do Maven Central com checksum fixado.
# Usa wget/sha256sum do busybox para funcionar com o builder legado e com BuildKit.
FROM eclipse-temurin:25-jre-alpine AS otel-agent
RUN mkdir -p /otel && \
    wget -q -O /otel/opentelemetry-javaagent.jar \
        https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.32.0/opentelemetry-javaagent-2.32.0.jar && \
    echo "f787eb6c7f3d18e69a431e108a15278d25ee37f83d68b678f621e063f3988f82  /otel/opentelemetry-javaagent.jar" | sha256sum -c - && \
    chmod 644 /otel/opentelemetry-javaagent.jar

# Stage 3: Runtime seguro e enxuto
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Adiciona tzdata para suporte correto a fuso horário e ca-certificates
RUN apk --no-cache add tzdata ca-certificates

# Cria usuário e grupo não-privilegiados para execução da aplicação
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Garante a existência dos diretórios esperados pelos volumes de chaves
RUN mkdir -p /app/config/keys /app/config/qa_keys && \
    chown -R appuser:appgroup /app

# Copia o JAR do estágio de build com permissão do appuser
COPY --from=builder --chown=appuser:appgroup /app/workshop.jar app.jar

# Java Agent e configuração ficam na imagem, mas só são ativados pelo compose
# (-javaagent em JAVA_TOOL_OPTIONS); o ENTRYPOINT não muda.
COPY --from=otel-agent /otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY apps/api/otel/javaagent.properties /otel/javaagent.properties

# Versão do build vai para service.version na cópia da imagem (nunca no arquivo do repo).
# Só [A-Za-z0-9._-] para não quebrar o formato key=value do .properties.
ARG SERVICE_VERSION=unknown
RUN if ! printf '%s' "${SERVICE_VERSION}" | grep -Eq '^[A-Za-z0-9._-]+$'; then \
        echo "SERVICE_VERSION inválido: use somente letras, dígitos, '.', '_' e '-'" >&2; \
        exit 1; \
    fi && \
    sed -i "s|^otel.resource.attributes=service.namespace=workshop\$|otel.resource.attributes=service.namespace=workshop,service.version=${SERVICE_VERSION}|" /otel/javaagent.properties && \
    grep -Fqx "otel.resource.attributes=service.namespace=workshop,service.version=${SERVICE_VERSION}" /otel/javaagent.properties

# Define a execução como usuário não-root
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
