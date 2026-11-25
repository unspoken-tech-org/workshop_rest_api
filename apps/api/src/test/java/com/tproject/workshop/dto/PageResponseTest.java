package com.tproject.workshop.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.validation.PageBounds;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldConvertSpringPagePreservingContractFields() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.number()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.last()).isFalse();
    }

    @Test
    void shouldApplyUnifiedDefaultPageAndSize() {
        BrandSearchParam param = new BrandSearchParam("  ", null, null);
        DeviceQueryParam query = new DeviceQueryParam();

        assertThat(param.query()).isNull();
        assertThat(param.page()).isEqualTo(PageBounds.DEFAULT_PAGE);
        assertThat(param.size()).isEqualTo(PageBounds.DEFAULT_SIZE);
        assertThat(query.getPage()).isEqualTo(PageBounds.DEFAULT_PAGE);
        assertThat(query.getSize()).isEqualTo(PageBounds.DEFAULT_SIZE);
    }

    @Test
    void shouldValidatePageBoundsWithConsistentPtBrMessages() {
        BrandSearchParam invalid = new BrandSearchParam(null, -1, 101);

        Set<ConstraintViolation<BrandSearchParam>> violations = validator.validate(invalid);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "O número da página deve ser maior ou igual a zero",
                        "O tamanho da página deve ser no máximo 100");
    }
}
