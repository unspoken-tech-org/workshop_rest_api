#!/bin/bash
# =============================================================================
# Script de Release - Cria tag e atualiza versão no build.gradle
# =============================================================================
# Uso:
#   ./scripts/release.sh [major|minor|patch|hotfix] [mensagem]
#
# Exemplos:
#   ./scripts/release.sh patch                    # v1.0.2 -> v1.0.3
#   ./scripts/release.sh minor                    # v1.0.3 -> v1.1.0
#   ./scripts/release.sh major                    # v1.1.0 -> v2.0.0
#   ./scripts/release.sh patch "Fix login bug"   # Com mensagem customizada
#   ./scripts/release.sh hotfix                   # v1.0.3 -> v1.0.4-hotfix
# =============================================================================

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Arquivo que contém a versão
BUILD_FILE="build.gradle"

# Função para mostrar uso
show_usage() {
    echo -e "${BLUE}Uso:${NC} $0 [major|minor|patch|hotfix] [mensagem]"
    echo ""
    echo -e "${BLUE}Tipos de release:${NC}"
    echo "  major   - Incrementa versão major (1.0.0 -> 2.0.0)"
    echo "  minor   - Incrementa versão minor (1.0.0 -> 1.1.0)"
    echo "  patch   - Incrementa versão patch (1.0.0 -> 1.0.1)"
    echo "  hotfix  - Incrementa patch com sufixo -hotfix (1.0.0 -> 1.0.1-hotfix)"
    echo ""
    echo -e "${BLUE}Exemplos:${NC}"
    echo "  $0 patch"
    echo "  $0 minor \"Nova feature de exportação\""
    echo "  $0 hotfix \"Correção urgente de login\""
    exit 1
}

# Função para log colorido
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar argumentos
if [ -z "$1" ]; then
    log_error "Tipo de release não especificado!"
    show_usage
fi

RELEASE_TYPE="$1"
CUSTOM_MESSAGE="$2"

# Validar tipo de release
if [[ ! "$RELEASE_TYPE" =~ ^(major|minor|patch|hotfix)$ ]]; then
    log_error "Tipo de release inválido: $RELEASE_TYPE"
    show_usage
fi

# Verificar se estamos na branch main
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ] && [ "$CURRENT_BRANCH" != "master" ]; then
    log_warning "Você está na branch '$CURRENT_BRANCH', não na 'main'."
    read -p "Deseja continuar mesmo assim? (s/N): " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Ss]$ ]]; then
        log_info "Release cancelado."
        exit 0
    fi
fi

# Verificar se há mudanças não commitadas
if ! git diff-index --quiet HEAD --; then
    log_error "Existem mudanças não commitadas. Commit ou stash antes de criar uma release."
    git status --short
    exit 1
fi

# Atualizar repositório
log_info "Atualizando repositório..."
git fetch --tags
git pull --rebase

# Verificar se build.gradle existe
if [ ! -f "$BUILD_FILE" ]; then
    log_error "Arquivo $BUILD_FILE não encontrado!"
    exit 1
fi

# Extrair versão atual do build.gradle
CURRENT_VERSION=$(grep -E "^version\s*=\s*['\"]" "$BUILD_FILE" | sed -E "s/version\s*=\s*['\"]([^'\"]+)['\"].*/\1/")

if [ -z "$CURRENT_VERSION" ]; then
    log_error "Não foi possível extrair a versão do $BUILD_FILE"
    exit 1
fi

log_info "Versão atual: $CURRENT_VERSION"

# Separar versão em partes (remover sufixo se houver)
BASE_VERSION=$(echo "$CURRENT_VERSION" | sed -E 's/-.*//')
IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE_VERSION"

# Calcular nova versão
case "$RELEASE_TYPE" in
    major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_VERSION="${NEW_MAJOR}.0.0"
        ;;
    minor)
        NEW_MINOR=$((MINOR + 1))
        NEW_VERSION="${MAJOR}.${NEW_MINOR}.0"
        ;;
    patch)
        NEW_PATCH=$((PATCH + 1))
        NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"
        ;;
    hotfix)
        NEW_PATCH=$((PATCH + 1))
        NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}-hotfix"
        ;;
esac

NEW_TAG="v${NEW_VERSION}"

log_info "Nova versão: $NEW_VERSION"
log_info "Nova tag: $NEW_TAG"

# Verificar se tag já existe
if git rev-parse "$NEW_TAG" >/dev/null 2>&1; then
    log_error "Tag $NEW_TAG já existe!"
    exit 1
fi

# Confirmar com usuário
echo ""
echo -e "${YELLOW}=== RESUMO DA RELEASE ===${NC}"
echo -e "  Versão atual:  ${RED}$CURRENT_VERSION${NC}"
echo -e "  Nova versão:   ${GREEN}$NEW_VERSION${NC}"
echo -e "  Tag:           ${GREEN}$NEW_TAG${NC}"
echo -e "  Tipo:          $RELEASE_TYPE"
echo ""
read -p "Confirmar release? (s/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Ss]$ ]]; then
    log_info "Release cancelado."
    exit 0
fi

# Atualizar versão no build.gradle
log_info "Atualizando versão no $BUILD_FILE..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/version = '$CURRENT_VERSION'/version = '$NEW_VERSION'/" "$BUILD_FILE"
    sed -i '' "s/version = \"$CURRENT_VERSION\"/version = \"$NEW_VERSION\"/" "$BUILD_FILE"
else
    # Linux/Git Bash
    sed -i "s/version = '$CURRENT_VERSION'/version = '$NEW_VERSION'/" "$BUILD_FILE"
    sed -i "s/version = \"$CURRENT_VERSION\"/version = \"$NEW_VERSION\"/" "$BUILD_FILE"
fi

# Verificar se a alteração foi feita
NEW_VERSION_CHECK=$(grep -E "^version\s*=\s*['\"]" "$BUILD_FILE" | sed -E "s/version\s*=\s*['\"]([^'\"]+)['\"].*/\1/")
if [ "$NEW_VERSION_CHECK" != "$NEW_VERSION" ]; then
    log_error "Falha ao atualizar versão no $BUILD_FILE"
    git checkout "$BUILD_FILE"
    exit 1
fi
log_success "Versão atualizada no $BUILD_FILE"

# Criar commit
COMMIT_MESSAGE="chore(release): bump version to $NEW_VERSION"
if [ -n "$CUSTOM_MESSAGE" ]; then
    COMMIT_MESSAGE="$COMMIT_MESSAGE - $CUSTOM_MESSAGE"
fi

log_info "Criando commit..."
git add "$BUILD_FILE"
git commit -m "$COMMIT_MESSAGE"
log_success "Commit criado"

# Criar tag
TAG_MESSAGE="Release $NEW_VERSION"
if [ -n "$CUSTOM_MESSAGE" ]; then
    TAG_MESSAGE="$TAG_MESSAGE: $CUSTOM_MESSAGE"
fi

log_info "Criando tag $NEW_TAG..."
git tag -a "$NEW_TAG" -m "$TAG_MESSAGE"
log_success "Tag criada"

# Push
log_info "Enviando para o repositório remoto..."
git push origin "$CURRENT_BRANCH"
git push origin "$NEW_TAG"
log_success "Push concluído"

# Resumo final
echo ""
echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}  RELEASE CRIADA COM SUCESSO!${NC}"
echo -e "${GREEN}=============================================${NC}"
echo ""
echo -e "  Tag:     ${GREEN}$NEW_TAG${NC}"
echo -e "  Versão:  ${GREEN}$NEW_VERSION${NC}"
echo -e "  Commit:  $(git rev-parse --short HEAD)"
echo ""
echo -e "  O deploy será iniciado automaticamente."
echo -e "  Acompanhe em: ${BLUE}https://github.com/<owner>/<repo>/actions${NC}"
echo ""
echo -e "${GREEN}=============================================${NC}"

