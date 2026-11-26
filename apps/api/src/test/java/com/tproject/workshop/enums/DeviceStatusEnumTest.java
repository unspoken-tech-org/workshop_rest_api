package com.tproject.workshop.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.tproject.workshop.dto.device.DeviceStatusInputRecord;
import com.tproject.workshop.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeviceStatusEnumTest {

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"PRONTO", "APROVADO", "NAO_APROVADO", "ENTREGUE", "DESCARTADO"})
    void shouldResetUrgencyAndRevisionForTerminalAndCompletedStatuses(DeviceStatusEnum status) {
        assertThat(status.resetsUrgencyAndRevision()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"NOVO", "EM_ANDAMENTO", "AGUARDANDO"})
    void shouldPreserveUrgencyAndRevisionForOpenWorkStatuses(DeviceStatusEnum status) {
        assertThat(status.resetsUrgencyAndRevision()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"ENTREGUE", "DESCARTADO"})
    void shouldMarkDeliveredAndDiscardedAsClosed(DeviceStatusEnum status) {
        assertThat(status.isClosed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"NOVO", "EM_ANDAMENTO", "AGUARDANDO", "APROVADO", "NAO_APROVADO", "PRONTO"})
    void shouldNotMarkOpenOrReadyStatusesAsClosed(DeviceStatusEnum status) {
        assertThat(status.isClosed()).isFalse();
    }

    @Test
    void shouldParseTrimmedAndCaseInsensitiveStatusNames() {
        assertThat(DeviceStatusEnum.fromString("  em_andamento "))
                .isEqualTo(DeviceStatusEnum.EM_ANDAMENTO);
        assertThat(DeviceStatusEnum.fromString("Pronto")).isEqualTo(DeviceStatusEnum.PRONTO);
    }

    @Test
    void shouldRejectNullBlankOrUnknownStatuses() {
        assertThatThrownBy(() -> DeviceStatusEnum.fromString(null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeviceStatusEnum.fromString("   "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeviceStatusEnum.fromString("INVALIDO"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldSerializeAndDeserializeStatusViaJackson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String json =
                mapper.writeValueAsString(new DeviceStatusInputRecord(DeviceStatusEnum.PRONTO));
        assertThat(json).isEqualTo("{\"deviceStatus\":\"PRONTO\"}");

        DeviceStatusInputRecord deserialized =
                mapper.readValue(
                        "{\"deviceStatus\":\"  em_andamento \"}", DeviceStatusInputRecord.class);
        assertThat(deserialized.deviceStatus()).isEqualTo(DeviceStatusEnum.EM_ANDAMENTO);

        assertThatThrownBy(() ->
                        mapper.readValue(
                                "{\"deviceStatus\":\"INVALIDO\"}", DeviceStatusInputRecord.class))
                .isInstanceOf(ValueInstantiationException.class)
                .hasCauseInstanceOf(BadRequestException.class);
    }
}
