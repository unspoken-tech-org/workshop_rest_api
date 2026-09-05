# Stage 1: Build da aplicação
FROM eclipse-temurin:21-jdk-alpine AS builder
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

# Stage 2: Runtime seguro e enxuto
FROM eclipse-temurin:21-jre-alpine
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

# Define a execução como usuário não-root
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
