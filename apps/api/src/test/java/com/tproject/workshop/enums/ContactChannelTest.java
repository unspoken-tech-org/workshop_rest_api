package com.tproject.workshop.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ContactChannelTest {

    @Test
    void shouldNormalizeAccentsCaseAndWhitespace() {
        assertThat(ContactChannel.fromString("Ligação")).isEqualTo(ContactChannel.LIGACAO);
        assertThat(ContactChannel.fromString("  MENSAGEM ")).isEqualTo(ContactChannel.MENSAGEM);
        assertThat(ContactChannel.fromString("Pessoalmente"))
                .isEqualTo(ContactChannel.PESSOALMENTE);
        assertThat(ContactChannel.fromString("WHATSAPP")).isEqualTo(ContactChannel.WHATSAPP);
    }

    @Test
    void shouldExposePhoneRequirementAndObservabilityValues() {
        assertThat(ContactChannel.LIGACAO.requiresPhone()).isTrue();
        assertThat(ContactChannel.MENSAGEM.requiresPhone()).isTrue();
        assertThat(ContactChannel.PESSOALMENTE.requiresPhone()).isFalse();
        assertThat(ContactChannel.WHATSAPP.requiresPhone()).isFalse();

        assertThat(ContactChannel.LIGACAO.observabilityValue()).isEqualTo("ligacao");
        assertThat(ContactChannel.MENSAGEM.observabilityValue()).isEqualTo("mensagem");
        assertThat(ContactChannel.PESSOALMENTE.observabilityValue()).isEqualTo("other");
        assertThat(ContactChannel.WHATSAPP.observabilityValue()).isEqualTo("other");
    }

    @Test
    void shouldRejectInvalidChannelAndAllowNullableEmpty() {
        assertThat(ContactChannel.fromNullable(null)).isEmpty();
        assertThat(ContactChannel.fromNullable("   ")).isEmpty();
        assertThatThrownBy(() -> ContactChannel.fromString("email"))
                .isInstanceOf(BadRequestException.class);
    }
}
