package com.tproject.workshop.repository.jdbc.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import com.tproject.workshop.utils.UtilsString;
import com.tproject.workshop.utils.mapper.JsonResultSetMapper;
import lombok.RequiredArgsConstructor;
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryJdbcImpl implements DeviceRepositoryJdbc {


    public static final String DEVICE_ID = "DEVICE_ID";
    public static final String CUSTOMER_PHONE = "CUSTOMER_PHONE";
    public static final String CUSTOMER_CPF = "CUSTOMER_CPF";
    public static final String DEVICE_TYPES = "DEVICE_TYPES";
    public static final String DEVICE_BRANDS = "DEVICE_BRANDS";
    public static final String STATUS = "STATUS";
    public static final String INITIAL_ENTRY_DATE = "INITIAL_ENTRY_DATE";
    public static final String FINAL_ENTRY_DATE = "FINAL_ENTRY_DATE";
    public static final String CUSTOMER_NAME = "CUSTOMER_NAME";
    public static final String HAS_URGENCY = "HAS_URGENCY";
    public static final String HAS_REVISION = "HAS_REVISION";
    public static final String ORDER_BY_FIELD = "ORDER_BY_FIELD";
    public static final String ORDER_BY_DIRECTION = "ORDER_BY_DIRECTION";
    public static final String PAGE_SIZE = "PAGE_SIZE";
    public static final String OFFSET = "OFFSET";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Page<DeviceTableDto> listTable(DeviceQueryParam deviceParams) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(DEVICE_ID, deviceParams.getDeviceId(), Types.INTEGER)
                .addValue(CUSTOMER_NAME, deviceParams.getCustomerName(), Types.VARCHAR)
                .addValue(CUSTOMER_PHONE, deviceParams.getCustomerPhone(), Types.VARCHAR)
                .addValue(CUSTOMER_CPF, UtilsString.onlyDigits(deviceParams.getCustomerCpf()), Types.VARCHAR)
                .addValue(DEVICE_TYPES, UtilsSql.toLiteralArray(deviceParams.getDeviceTypes()))
                .addValue(DEVICE_BRANDS, UtilsSql.toLiteralArray(deviceParams.getDeviceBrands()))
                .addValue(STATUS, UtilsSql.toLiteralArray(deviceParams.getStatus()))
                .addValue(INITIAL_ENTRY_DATE, deviceParams.getInitialEntryDate(), Types.VARCHAR)
                .addValue(FINAL_ENTRY_DATE, deviceParams.getFinalEntryDate(), Types.VARCHAR)
                .addValue(HAS_URGENCY, deviceParams.getUrgency(), Types.BOOLEAN)
                .addValue(HAS_REVISION, deviceParams.getRevision(), Types.BOOLEAN)
                .addValue(ORDER_BY_FIELD, deviceParams.getOrdenation().orderByField(), Types.VARCHAR)
                .addValue(ORDER_BY_DIRECTION, deviceParams.getOrdenation().orderByDirection().toString(), Types.VARCHAR)
                .addValue(PAGE_SIZE, deviceParams.getSize())
                .addValue(OFFSET, deviceParams.getPage());

        List<DeviceTableDto> devices = jdbcTemplate
                .query(
                        UtilsSql.getQuery("device/listTable"),
                        params,
                        JdbcTemplateMapperFactory
                                .newInstance()
                                .addKeys(DeviceTableDto.Fields.deviceId.name(),
                                        DeviceTableDto.Fields.customerId.name(),
                                        DeviceTableDto.Fields.type.name(),
                                        DeviceTableDto.Fields.brand.name(),
                                        DeviceTableDto.Fields.model.name(),
                                        DeviceTableDto.Fields.customerName.name(),
                                        DeviceTableDto.Fields.entryDate.name(),
                                        DeviceTableDto.Fields.departureDate.name(),
                                        DeviceTableDto.Fields.status.name(),
                                        DeviceTableDto.Fields.observation.name(),
                                        DeviceTableDto.Fields.problem.name(),
                                        DeviceTableDto.Fields.hasUrgency.name(),
                                        DeviceTableDto.Fields.hasUrgency.name(),
                                        DeviceTableDto.Fields.hasRevision.name()
                                )
                                .newResultSetExtractor(DeviceTableDto.class)
                );

        Long total = jdbcTemplate.queryForObject(UtilsSql.getQuery("device/listTable.count"), params, Long.class);

        return new PageImpl<>(devices, PageRequest.of(deviceParams.getPage(), deviceParams.getSize()), total != null ? total : 0);
    }

    @Override
    public Optional<DeviceOutputDto> findByDeviceId(int deviceId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(DEVICE_ID, deviceId, Types.INTEGER);

        try {
            DeviceOutputDto device = jdbcTemplate.queryForObject(
                    UtilsSql.getQuery("device/getDevice"),
                    params,
                    getDeviceOutputDtoMapper()
            );
            return Optional.ofNullable(device);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private RowMapper<DeviceOutputDto> getDeviceOutputDtoMapper() {
        return (rs, rowNum) -> {
            DeviceOutputDto dto = new DeviceOutputDto();

            dto.setDeviceId(rs.getInt("device_id"));
            dto.setCustomerId(rs.getInt("customer_id"));
            dto.setCustomerName(rs.getString("customer_name"));
            dto.setDeviceStatus(rs.getString("device_status"));
            dto.setBrandName(rs.getString("brand_name"));
            dto.setModelName(rs.getString("model_name"));
            dto.setTypeName(rs.getString("type_name"));
            dto.setTechnicianId((Integer) rs.getObject("technician_id"));
            dto.setTechnicianName(rs.getString("technician_name"));
            dto.setProblem(rs.getString("problem"));
            dto.setObservation(rs.getString("observation"));
            dto.setBudget(rs.getString("budget"));
            dto.setHasUrgency(rs.getBoolean("has_urgency"));
            dto.setRevision(rs.getBoolean("is_revision"));
            dto.setEntryDate(rs.getTimestamp("entry_date"));
            dto.setLaborValue(rs.getBigDecimal("labor_value"));
            dto.setServiceValue(rs.getBigDecimal("service_value"));
            dto.setLaborValueCollected(rs.getBoolean("labor_value_collected"));
            dto.setDepartureDate(
                    rs.getTimestamp("departure_date") != null ? rs.getTimestamp("departure_date")
                            : null);
            dto.setLastUpdate(rs.getTimestamp("last_update"));

            Array colorsArray = rs.getArray("device_colors");
            dto.setDeviceColors(
                    colorsArray != null ? Arrays.asList((String[]) colorsArray.getArray())
                            : Collections.emptyList());

            dto.setCustomerContacts(
                    JsonResultSetMapper.readJsonList(rs, "customer_contacts", new TypeReference<>() {
                    }, objectMapper)
            );

            dto.setCustomerPhones(
                    JsonResultSetMapper.readJsonList(rs, "customer_phones", new TypeReference<>() {
                    }, objectMapper)
            );

            dto.setOtherDevices(
                    JsonResultSetMapper.readJsonList(rs, "other_devices", new TypeReference<>() {
                    }, objectMapper)
            );

            dto.setPayments(
                    JsonResultSetMapper.readJsonList(rs, "payments", new TypeReference<>() {
                    }, objectMapper)
            );

            return dto;
        };
    }

}
