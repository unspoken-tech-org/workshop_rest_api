package com.tproject.workshop.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.service.BrandService;
import com.tproject.workshop.service.ModelService;
import com.tproject.workshop.service.TypeService;
import com.tproject.workshop.utils.CatalogName;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

class CatalogNameKeyIT extends AbstractIntegrationTest {

    @Autowired private TypeService typeService;
    @Autowired private BrandService brandService;
    @Autowired private ModelService modelService;

    @Test
    void shouldRejectDuplicateNameKey_whenInsertingSpellingVariantDirectly() {
        jdbcTemplate.update("INSERT INTO types (type) VALUES ('micro-ondas')");
        jdbcTemplate.update("INSERT INTO brands (brand) VALUES ('Brastemp')");
        jdbcTemplate.update("INSERT INTO models (model) VALUES ('Ação 100')");

        assertThatThrownBy(
                        () -> jdbcTemplate.update("INSERT INTO types (type) VALUES ('Microondas')"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() ->
                        jdbcTemplate.update("INSERT INTO brands (brand) VALUES ('BRASTEMP ')"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(
                        () -> jdbcTemplate.update("INSERT INTO models (model) VALUES ('acao-100')"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldReturnExistingType_whenSpellingVariesInCaseSpaceHyphenOrAccent() {
        Integer id = typeService.createOrReturnExistentType("Forno Elétrico").getIdType();

        for (String variant :
                List.of(
                        "forno eletrico",
                        "  FORNO   ELÉTRICO ",
                        "forno-elétrico",
                        "fornoeletrico")) {
            assertThat(typeService.createOrReturnExistentType(variant).getIdType())
                    .as(variant)
                    .isEqualTo(id);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM types", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldReturnExistingBrandAndModel_whenSpellingVaries() {
        Integer brandId = brandService.createOrReturnExistentBrand("Electrolux").getIdBrand();
        Integer modelId =
                modelService.createOrReturnExistentModel("Micro-Ondas MB 38").getIdModel();

        assertThat(brandService.createOrReturnExistentBrand("  ELECTROLUX ").getIdBrand())
                .isEqualTo(brandId);
        assertThat(modelService.createOrReturnExistentModel("micro ondas mb38").getIdModel())
                .isEqualTo(modelId);
        assertThat(modelService.createOrReturnExistentModel("MICROONDAS MB-38").getIdModel())
                .isEqualTo(modelId);
    }

    @Test
    void shouldCreateNewEntry_whenKeyIsDifferent() {
        Integer first = typeService.createOrReturnExistentType("Ventilador de Mesa").getIdType();
        Integer second = typeService.createOrReturnExistentType("Ventilador de Teto").getIdType();

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void shouldComputeSameKeyInJavaAndDatabase() {
        List<String> samples =
                List.of(
                        "Micro-ondas",
                        "  Lava   Louças  ",
                        "Forno ELÉTRICO",
                        "Ação 100",
                        "Fußball",
                        "Æther Øl Đa Łódź Œuvre",
                        "Ñandú Çedilha Ümlaut",
                        "AirFryer 3,5L (preta)",
                        "100%/ok");
        for (String sample : samples) {
            String dbKey =
                    jdbcTemplate.queryForObject(
                            "SELECT regexp_replace(lower(public.immutable_unaccent(?)), '[^a-z0-9]', '', 'g')",
                            String.class,
                            sample);
            assertThat(CatalogName.nameKey(sample)).as(sample).isEqualTo(dbKey);
        }
    }
}
