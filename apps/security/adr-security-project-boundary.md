# ADR: limite do projeto de segurança

*Status:* aceito  
*Data:* 2026-09-05

## Contexto

O monorepo passa a declarar dois projetos Gradle: `apps:api`, que entrega o
serviço Spring Boot e seu JAR executável, e `apps:security`, reservado para a
extração futura dos componentes de autenticação e autorização. A segurança
continua funcionando dentro da API nesta etapa; criar o projeto não deve
alterar o comportamento ou o artefato publicado.

## Decisão

`apps/security` será um projeto Java independente, com coordenadas herdadas do
projeto raiz (`com.tproject:1.3.1`) e sem dependência da implementação da API.
O módulo é deliberadamente um placeholder compilável: novos contratos de
segurança poderão ser adicionados nele sem mover código de produção nesta
reorganização.

`apps/api` permanece como o único projeto Spring Boot e mantém os componentes
de segurança atuais, as dependências existentes, os testes e o artefato
`workshop-1.3.1.jar`. O projeto raiz apenas agrega as tarefas de build, testes,
testes de integração e geração do Boot JAR.

## Consequências

- Builds e testes podem ser executados por projeto ou pela raiz do monorepo.
- A fronteira fica explícita sem introduzir uma dependência circular entre API
  e segurança.
- A extração posterior precisará definir contratos (tokens, chaves, claims e
  configuração) e testes de integração antes de mover as classes atuais.
- Até essa extração, mudanças de autenticação continuam sendo feitas em
  `apps/api/src` e não há um segundo serviço executável.

## Fora do escopo desta decisão

Não são alterados fluxos de autenticação, banco de dados, Docker Compose,
infraestrutura ou workflows. Também não é criado um artefato executável para
`apps/security` nesta fase.
