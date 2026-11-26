# CONVENTIONS.md

Convenções obrigatórias para código novo e alterado em `workshop_rest_api` (`apps/api`).
Regras marcadas com **(ArchUnit)** são verificadas automaticamente em `./gradlew check`.
Violações pré-existentes no código legado estão congeladas via `FreezingArchRule` em
`apps/api/src/test/resources/archunit_store/` — **não copie o padrão legado**.

---

## 1. Arquitetura e Camadas (Código de Produção)

| Camada / Aspecto | Regra atual (código novo) | O que NÃO copiar (legado) |
| --- | --- | --- |
| **Controllers** | Interface `*Controller` em `controller/` com `@RequestMapping` e `@Valid`; implementação `*ControllerImpl` em `controller/impl/` apenas delega para services **(ArchUnit)**. | Colocar `@RequestMapping` ou `@Valid` diretamente na `*ControllerImpl`, ou injetar repositories em controllers **(ArchUnit)**. |
| **DTOs** | `record` imutável em `dto/<dominio>/` terminado em `Request`, `Response` ou `Query`, com `static from(Entity)` quando aplicável **(ArchUnit)**. | Classes mutáveis com sufixos legados (`*InputDto*`, `*OutputDto*`, `*Record` no nome). |
| **Entidades JPA** | Classes `@Entity` em `model/` com `@Getter`/`@Setter` explícitos (ou métodos de domínio), **nunca** `@Data` **(ArchUnit)**. | `@Data` em entidades JPA (gera `equals`/`hashCode`/`toString` problemáticos com proxies Hibernate). |
| **Injeção de dependência** | Beans Spring (`@Service`, `@RestController`, `@Component`) usam `@RequiredArgsConstructor` e campos `private final`, nunca `@AllArgsConstructor` **(ArchUnit)**. | `@AllArgsConstructor` ou `@Autowired` em campos. |
| **Pacotes** | Pacote de utilitários é `utils/` (plural); nomes de pacotes em lowercase sem camelCase ou snake_case **(ArchUnit)**. | Pacote `util/` (singular), `dto/deviceStatistics/`, `dto/typesBrandsModels/`, `utils/filter_utils/`. |
| **Exceções** | Exceções de domínio estendem a base comum (`DomainException` ou subclasse mapeada) e são tratadas em `GlobalExceptionHandler`. | Exceções soltas estendendo `RuntimeException` diretamente sem mapeamento central. |
| **Queries complexas** | Repositórios JDBC em `repository/jdbc/impl/` lendo SQL externo em `apps/api/src/main/resources/db/query/`. | SQL inline complexo em strings Java ou JPQL pesado para relatórios. |
| **Segurança** | `config.security` não depende de `controller` **(ArchUnit)**. | Acoplamento reverso de filtros de segurança em controllers. |

### ADRs curtos (por que decidimos assim)
- **Records em `dto/`:** eliminam boilerplate, garantem imutabilidade na borda HTTP e impedem mutação acidental entre controller e service.
- **Sem `@Data` em `@Entity`:** `@EqualsAndHashCode` sobre todos os campos quebra `Set`s e lazy loading do Hibernate; `@ToString` pode disparar queries N+1.
- **`FreezingArchRule` (ratchet):** permite ativar regras novas hoje sem bloquear entregas em refatorações gigantes; ao tocar num legado, a baseline encolhe automaticamente.

---

## 2. Convenções Obrigatórias de Testes (`apps/api/src/test`)

1. **Sufixos e tags:**
   - Testes unitários terminam em `*Test.java` e **não** usam `@SpringBootTest` **(ArchUnit)**.
   - Testes de integração terminam em `*IT.java` e estendem `AbstractIntegrationTest` (que carrega `@Tag("integration")`).
   - Classes de suporte (`support/`, `fixtures/`) não terminam em `Test` nem `IT`.
2. **Visibilidade (JUnit 5):**
   - Classes e métodos de teste (`@Test`, `@ParameterizedTest`) são **package-private** (`class FooTest`, `void should...()`), nunca `public` **(ArchUnit)**.
3. **Nomenclatura:**
   - `should<Comportamento>_when<Condicao>()` (ou `@Nested` por operação com `should<Comportamento>()`).
4. **Assertions:**
   - **AssertJ** (`assertThat`, `assertThatThrownBy`, `assertThatCode`) como padrão único em Java.
   - Em RestAssured, validar status e campos de contrato diretamente (`body(...)` ou desserializando o DTO e asserindo com AssertJ).
5. **Fim de novos snapshots JSON:**
   - **Proibido** criar ou editar arquivos em `apps/api/src/test/resources/jsons/`. Os snapshots existentes ficam congelados apenas para os ITs legados até serem aposentados por domínio (exclusão é permitida quando um domínio migra).
6. **HTTP Status:**
   - Usar `org.springframework.http.HttpStatus` (`.value()`) ou inteiros literais explícitos — **nunca** importar `org.apache.http.HttpStatus` **(ArchUnit)**.
7. **Tempo e mocks determinísticos:**
   - Usar `TestClock.FIXED` (`Clock.fixed(...)`) em testes unitários, **nunca** `@Spy Clock.systemDefaultZone()` nem `Instant.now()` solto em assertions **(ArchUnit)**.
   - Usar Mockito estrito (padrão do `MockitoExtension`): **proibido** `@MockitoSettings(strictness = Strictness.LENIENT)` **(ArchUnit)**.
   - Em ITs, `Instant.now()` é aceito apenas para preparar dados ou calcular prazos de espera, nunca em assertions exatas contra o horário do servidor.
8. **Isolamento sem ordem e sem IDs fixos:**
   - Proibido `@TestMethodOrder` / `@Order` em testes novos.
   - Cada teste prepara o mínimo de dados que precisa via builders em `support/fixtures/` ou helper explícito, deixando o banco gerar os IDs e usando o ID retornado nas assertions (sem depender de `resetTablesSequence.sql`).
9. **Cache de contexto Spring preservado:**
   - Nunca adicionar `@DirtiesContext`, `@MockitoBean` ou `@TestPropertySource` divergente em subclasses de `AbstractIntegrationTest`, para manter 1 único contexto Spring na suíte inteira.
