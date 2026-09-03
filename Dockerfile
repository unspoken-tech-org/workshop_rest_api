# Stage 1: Build da aplicação
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copia arquivos do Gradle Wrapper e dependências para cacheamento de camadas
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/

# Garante permissão de execução no wrapper e pré-baixa dependências
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copia apenas o código fonte principal
COPY src/main src/main

# Compila o JAR executável da aplicação
RUN ./gradlew bootJar --no-daemon -x test

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
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Define a execução como usuário não-root
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
