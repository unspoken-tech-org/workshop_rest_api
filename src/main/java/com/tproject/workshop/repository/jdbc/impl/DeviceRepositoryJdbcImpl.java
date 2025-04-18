package com.tproject.workshop.repository.jdbc.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import com.tproject.workshop.utils.mapper.JsonResultSetMapper;
import java.sql.Array;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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


    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Device saveDevice(Device device) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(DeviceInputDto.Fields.customerId.name(), device.getCustomer().getIdCustomer(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.deviceStatusId.name(), device.getDeviceStatus().getId(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.brandId.name(), device.getBrandsModelsTypes().getIdBrand().getIdBrand(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.modelId.name(), device.getBrandsModelsTypes().getIdModel().getModel(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.typeId.name(), device.getBrandsModelsTypes().getIdType().getIdType(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.technicianId.name(), device.getTechnician().getId(), Types.INTEGER)
                .addValue(DeviceInputDto.Fields.problem.name(), device.getProblem(), Types.VARCHAR)
                .addValue(DeviceInputDto.Fields.observation.name(), device.getObservation(), Types.VARCHAR)
                .addValue(DeviceInputDto.Fields.hasUrgency.name(), device.isHasUrgency(), Types.BOOLEAN)
                ;
        return null;
    }

    @Override
    public List<DeviceTableDto> listTable(DeviceQueryParam deviceParams) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(DEVICE_ID, deviceParams.getDeviceId(), Types.INTEGER)
                .addValue(CUSTOMER_NAME, deviceParams.getCustomerName(), Types.VARCHAR)
                .addValue(CUSTOMER_PHONE, deviceParams.getCustomerPhone(), Types.VARCHAR)
                .addValue(CUSTOMER_CPF, deviceParams.getCustomerCpf(), Types.VARCHAR)
                .addValue(DEVICE_TYPES,UtilsSql.toLiteralArray(deviceParams.getDeviceTypes()))
                .addValue(DEVICE_BRANDS,UtilsSql.toLiteralArray(deviceParams.getDeviceBrands()))
                .addValue(STATUS, UtilsSql.toLiteralArray(deviceParams.getStatus()))
                .addValue(INITIAL_ENTRY_DATE, deviceParams.getInitialEntryDate(), Types.VARCHAR)
                .addValue(FINAL_ENTRY_DATE, deviceParams.getFinalEntryDate() ,  Types.VARCHAR)
                ;

       return jdbcTemplate
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
                                                DeviceTableDto.Fields.hasUrgency.name()
                                        )
                                        .newResultSetExtractor(DeviceTableDto.class)
                        );
    }

    @Override
    public DeviceOutputDto findByDeviceId(int deviceId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue(DEVICE_ID, deviceId, Types.INTEGER);

        return jdbcTemplate.queryForObject(
            UtilsSql.getQuery("device/getDevice"),
            params,
            getDeviceOutputDtoMapper()
        );
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
            dto.setTechnicianId(rs.getInt("technician_id"));
            dto.setTechnicianName(rs.getString("technician_name"));
            dto.setProblem(rs.getString("problem"));
            dto.setObservation(rs.getString("observation"));
            dto.setBudget(rs.getString("budget"));
            dto.setHasUrgency(rs.getBoolean("has_urgency"));
            dto.setRevision(rs.getBoolean("is_revision"));
            dto.setEntryDate(rs.getTimestamp("entry_date"));
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

            return dto;
        };
    }

}
