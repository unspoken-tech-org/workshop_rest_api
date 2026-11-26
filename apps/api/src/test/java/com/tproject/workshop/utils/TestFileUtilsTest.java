package com.tproject.workshop.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TestFileUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "DeviceControllerIT, device-controller-i-t",
        "DeviceFuzzySearchIT, device-fuzzy-search-i-t",
        "ApiKeyControllerIT, api-key-controller-i-t",
        "CustomerContactControllerIT, customer-contact-controller-i-t"
    })
    void shouldPreserveLegacyLowerHyphenConversion(String input, String expected) {
        assertThat(TestFileUtils.toLowerHyphen(input)).isEqualTo(expected);
    }
}
