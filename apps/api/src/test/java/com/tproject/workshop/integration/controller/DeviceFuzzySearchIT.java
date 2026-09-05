package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql",
        "/test-scripts/DeviceFuzzySearchIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class DeviceFuzzySearchIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/device";

    @DisplayName("Search devices")
    @MethodSource("searchDevicesArguments")
    @ParameterizedTest(name = "{displayName} : {0} {2}")
    public void searchDevices(int index, Map<String, Object> params, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .body(params)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        if (List.of(18, 19).contains(index)) {
            super.validateResponseIgnoreAttributes(index, response, List.of("departureDate"));
        } else {
            super.validateResponse(index, response);
        }
    }

    private static Stream<Arguments> searchDevicesArguments() {
        return Stream.of(
                // 1: query "micro-ondas" → todos devices micro-ondas (acentuados e não)
                Arguments.of(1, Map.of("searchQuery", "micro-ondas"), "search micro-ondas"),

                // 2: query "Micro-ondas" (case mix) → mesmo que #1
                Arguments.of(2, Map.of("searchQuery", "Micro-ondas"), "search Micro-ondas case mix"),

                // 3: query "microondas" (sem hífen) → mesmo que #1 (fuzzy encontra todos)
                Arguments.of(3, Map.of("searchQuery", "microondas"), "search microondas sem hífen"),

                // 4: query "micro-ondas de embutir" → device 101 apenas
                Arguments.of(4, Map.of("searchQuery", "micro-ondas de embutir"), "search micro-ondas de embutir"),

                // 5: query "microond" (parcial) → mesmo que #1
                Arguments.of(5, Map.of("searchQuery", "microond"), "search microond parcial"),

                // 6: query "xyzzy" (inexistente) → página vazia
                Arguments.of(6, Map.of("searchQuery", "xyzzy"), "search xyzzy inexistente"),

                // 7: sem query → retorna todos os devices (55 total)
                Arguments.of(7, Map.of(), "search sem query"),

                // 8: query "samsung" → todos Samsung+Sansung: 9 devices
                Arguments.of(8, Map.of("searchQuery", "samsung"), "search samsung"),

                // 9: query "sansung" (typo) → mesmo conjunto que #8 (threshold 0.45)
                Arguments.of(9, Map.of("searchQuery", "sansung"), "search sansung typo"),

                // 10: query "britania" (sem acento) → Britânia+Britania: 8 devices
                Arguments.of(10, Map.of("searchQuery", "britania"), "search britania sem acento"),

                // 11: query "britânia" (com acento) → mesmo que #10
                Arguments.of(11, Map.of("searchQuery", "britânia"), "search britânia com acento"),

                // 12: query "ventilador" → todos ventiladores: 12 devices
                Arguments.of(12, Map.of("searchQuery", "ventilador"), "search ventilador"),

                // 13: query "joão" → devices de todos os Joãos: 25 devices
                Arguments.of(13, Map.of("searchQuery", "joão"), "search joão"),

                // 14: query "maria aparecida" → devices da Maria Aparecida: 14 devices
                Arguments.of(14, Map.of("searchQuery", "maria aparecida"), "search maria aparecida"),

                // 15: query "aparecida" (palavra do nome) → mesmo que #14
                Arguments.of(15, Map.of("searchQuery", "aparecida"), "search aparecida"),

                // 16: query "samsung" + status NOVO → devices Samsung+Sansung com status NOVO
                Arguments.of(16, Map.of("searchQuery", "samsung", "status", List.of("NOVO")), "search samsung with status NOVO"),

                // 17: query "britânia" + urgency true → devices Britânia/Britania com urgência
                Arguments.of(17, Map.of("searchQuery", "britânia", "urgency", true), "search britânia with urgency"),

                // 18: sem query + status ENTREGUE → device 102 apenas (departureDate auto-set)
                Arguments.of(18, Map.of("status", List.of("ENTREGUE")), "search status ENTREGUE"),

                // 19: sem query + status DESCARTADO → device 105 apenas (departureDate auto-set)
                Arguments.of(19, Map.of("status", List.of("DESCARTADO")), "search status DESCARTADO"),

                // =========================================================
                // NOVOS: multi-word fuzzy search (20-33)
                // =========================================================

                // 20: query "joão samsung" → devices do João que são Samsung
                Arguments.of(20, Map.of("searchQuery", "joão samsung"), "search joão samsung"),

                // 21: query "maria ventilador" → devices da Maria que são Ventilador
                Arguments.of(21, Map.of("searchQuery", "maria ventilador"), "search maria ventilador"),

                // 22: query "joão micro-ondas" → devices do João que são Micro-ondas
                Arguments.of(22, Map.of("searchQuery", "joão micro-ondas"), "search joão micro-ondas"),

                // 23: query "joão micro-ondas britânia" → João + Micro-ondas + Britânia
                Arguments.of(23, Map.of("searchQuery", "joão micro-ondas britânia"), "search joão micro-ondas britânia"),

                // 24: query "maria britânia ventilador" → Maria + Britânia + Ventilador
                Arguments.of(24, Map.of("searchQuery", "maria britânia ventilador"), "search maria britânia ventilador"),

                // 25: query "silva samsung" → Silva + Samsung (fuzzy em ambos)
                Arguments.of(25, Map.of("searchQuery", "silva samsung"), "search silva samsung"),

                // 26: query "aparecida ventilador de mesa" → Maria Aparecida + Ventilador de mesa
                Arguments.of(26, Map.of("searchQuery", "aparecida ventilador de mesa"), "search aparecida ventilador de mesa"),

                // 27: query "pedro arno" → Pedro + Arno
                Arguments.of(27, Map.of("searchQuery", "pedro arno"), "search pedro arno"),

                // 28: query "pedro liquidificador" → Pedro + Liquidificador
                Arguments.of(28, Map.of("searchQuery", "pedro liquidificador"), "search pedro liquidificador"),

                // 29: query "oliveira wallita" → Pedro Oliveira + Wallita
                Arguments.of(29, Map.of("searchQuery", "oliveira wallita"), "search oliveira wallita"),

                // 30: query "joão" (regression) → mesmo que #13 (25 devices)
                Arguments.of(30, Map.of("searchQuery", "joão"), "search joão (regression)"),

                // 31: query "samsung" (regression) → mesmo que #8
                Arguments.of(31, Map.of("searchQuery", "samsung"), "search samsung (regression)"),

                // 32: query "ventilador samsung" → ventilador E samsung
                Arguments.of(32, Map.of("searchQuery", "ventilador samsung"), "search ventilador samsung"),

                // 33: query "micro-ondas samsung joão" → João + Micro-ondas + Samsung
                Arguments.of(33, Map.of("searchQuery", "micro-ondas samsung joão"), "search micro-ondas samsung joão"),

                // =========================================================
                // NOMES COMPOSTOS: 3 Joãos com nomes diferentes (34-42)
                // =========================================================

                // 34: query "joão pedro da silva" → nome completo 4 palavras
                Arguments.of(34, Map.of("searchQuery", "joão pedro da silva"), "search joão pedro da silva (full name)"),

                // 35: query "joão pedro" → primeiro + segundo nome
                Arguments.of(35, Map.of("searchQuery", "joão pedro"), "search joão pedro (first + middle)"),

                // 36: query "joão paulo ferreira" → nome completo 3 palavras
                Arguments.of(36, Map.of("searchQuery", "joão paulo ferreira"), "search joão paulo ferreira (full name)"),

                // 37: query "joão victor almeida santos" → nome completo 4 palavras
                Arguments.of(37, Map.of("searchQuery", "joão victor almeida santos"), "search joão victor almeida santos (full name)"),

                // 38: query "joão victor" → primeiro + segundo nome
                Arguments.of(38, Map.of("searchQuery", "joão victor"), "search joão victor (first + middle)"),

                // 39: query "joao pedro da silva" → sem acento (unaccent normaliza)
                Arguments.of(39, Map.of("searchQuery", "joao pedro da silva"), "search joao pedro da silva (no accent)"),

                // 40: query "jao pedro" → typo "joão" → "jao"
                Arguments.of(40, Map.of("searchQuery", "jao pedro"), "search jao pedro (typo joão)"),

                // 41: query "joão paullo" → typo "paulo" → "paullo"
                Arguments.of(41, Map.of("searchQuery", "joão paullo"), "search joão paullo (typo paulo)"),

                // 42: query "joão pedro samsung" → nome + brand (interseção)
                Arguments.of(42, Map.of("searchQuery", "joão pedro samsung"), "search joão pedro samsung (name + brand)"),

                // =========================================================
                // INTERSEÇÃO COM FILTER: campos em comum (43-67)
                // =========================================================

                // --- deviceId ---
                // 43: deviceId=100 → 1 device específico
                Arguments.of(43, Map.of("deviceId", 100), "filter by deviceId"),

                // 44: deviceId=9999 → vazio (inexistente)
                Arguments.of(44, Map.of("deviceId", 9999), "filter by non-existent deviceId"),

                // --- customerCpf ---
                // 45: customerCpf="11111111111" → João (customer 10)
                Arguments.of(45, Map.of("customerCpf", "11111111111"), "filter by customer CPF"),

                // --- customerPhone ---
                // 46: customerPhone="11999" → João (phone 11999887766, parcial)
                Arguments.of(46, Map.of("customerPhone", "11999"), "filter by customer phone partial"),

                // --- status individual ---
                // 47: status=[NOVO]
                Arguments.of(47, Map.of("status", List.of("NOVO")), "filter status NOVO"),

                // 48: status=[EM_ANDAMENTO]
                Arguments.of(48, Map.of("status", List.of("EM_ANDAMENTO")), "filter status EM_ANDAMENTO"),

                // 49: status=[AGUARDANDO]
                Arguments.of(49, Map.of("status", List.of("AGUARDANDO")), "filter status AGUARDANDO"),

                // 50: status=[PRONTO]
                Arguments.of(50, Map.of("status", List.of("PRONTO")), "filter status PRONTO"),

                // --- múltiplos status ---
                // 51: status=[NOVO,EM_ANDAMENTO]
                Arguments.of(51, Map.of("status", List.of("NOVO", "EM_ANDAMENTO")), "filter status NOVO+EM_ANDAMENTO"),

                // --- status inválido ---
                // 52: status=[INVALID_STATUS] → vazio
                Arguments.of(52, Map.of("status", List.of("INVALID_STATUS")), "filter invalid status"),

                // --- urgency ---
                // 53: urgency=false
                Arguments.of(53, Map.of("urgency", false), "filter urgency=false"),

                // --- revision ---
                // 54: revision=true
                Arguments.of(54, Map.of("revision", true), "filter revision=true"),

                // 55: revision=false
                Arguments.of(55, Map.of("revision", false), "filter revision=false"),

                // --- combinação urgency+revision ---
                // 56: urgency=true,revision=true
                Arguments.of(56, Map.of("urgency", true, "revision", true), "filter urgency=true+revision=true"),

                // --- date range ---
                // 57: initialEntryDate=2024-06-01 → devices a partir de jun/2024
                Arguments.of(57, Map.of("initialEntryDate", "2024-06-01"), "filter initialEntryDate"),

                // 58: finalEntryDate=2024-12-31 → devices até dez/2024
                Arguments.of(58, Map.of("finalEntryDate", "2024-12-31"), "filter finalEntryDate"),

                // 59: range jun-ago 2024
                Arguments.of(59, Map.of("initialEntryDate", "2024-06-01", "finalEntryDate", "2024-08-31"), "filter date range Q2-2024"),

                // 60: range out-dez 2024
                Arguments.of(60, Map.of("initialEntryDate", "2024-10-01", "finalEntryDate", "2024-12-31"), "filter date range Q4-2024"),

                // --- paginação ---
                // 61: page=0,size=10
                Arguments.of(61, Map.of("page", 0, "size", 10), "pagination page 0 size 10"),

                // 62: page=1,size=10
                Arguments.of(62, Map.of("page", 1, "size", 10), "pagination page 1 size 10"),

                // 63: page=0,size=3
                Arguments.of(63, Map.of("page", 0, "size", 3), "pagination page 0 size 3"),

                // 64: page=100,size=5 → vazio
                Arguments.of(64, Map.of("page", 100, "size", 5), "pagination beyond data"),

                // 65: page=0,size=100 → todos
                Arguments.of(65, Map.of("page", 0, "size", 100), "pagination large size"),

                // --- date edge cases ---
                // 66: initialEntryDate futura → vazio
                Arguments.of(66, Map.of("initialEntryDate", "2030-01-01"), "filter future initialEntryDate"),

                // 67: finalEntryDate passada → vazio
                Arguments.of(67, Map.of("finalEntryDate", "2000-01-01"), "filter past finalEntryDate")
        );
    }
}
