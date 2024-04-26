package com.tproject.workshop.repository.jdbc.impl;

import com.tproject.workshop.config.ArraySqlValue;
import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import lombok.RequiredArgsConstructor;
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryJdbcImpl implements DeviceRepositoryJdbc {


    public static final String DEVICE_IDS = "DEVICE_IDS";
    public static final String CUSTOMER_ID = "CUSTOMER_ID";
    public static final String BRAND_ID = "BRAND_ID";
    public static final String MODEL_ID = "MODEL_ID";
    public static final String TYPE_ID = "TYPE_ID";
    public static final String STATUS_ID = "STATUS_ID";
    public static final String ENTRY_DATE = "ENTRY_DATE";
    public static final String DEPARTURE_DATE = "DEPARTURE_DATE";
    public static final String CUSTOMER_NAME = "CUSTOMER_NAME";


    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Device saveDevice(Device device) {
//        MapSqlParameterSource params = new MapSqlParameterSource()
//                .addValue(DeviceInputDto.Fields.customerId.name(), device.getCustomer().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.deviceStatusId.name(), device.getDeviceStatus().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.brandId.name(), device.getBrandsModelsTypes().getBrandsModels().getBrand().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.modelId.name(), device.getBrandsModelsTypes().getBrandsModels().getModel().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.typeId.name(), device.getBrandsModelsTypes().getType().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.technicianId.name(), device.getTechnician().getId(), Types.INTEGER)
//                .addValue(DeviceInputDto.Fields.problem.name(), device.getProblem(), Types.VARCHAR)
//                .addValue(DeviceInputDto.Fields.observation.name(), device.getObservation(), Types.VARCHAR)
//                .addValue(DeviceInputDto.Fields.hasUrgency.name(), device.isHasUrgency(), Types.BOOLEAN)
                ;
        return null;
    }

    @Override
    public List<DeviceTableDto> listTable(DeviceQueryParam deviceParams) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(DEVICE_IDS,
                        ArraySqlValue.create(Optional.ofNullable(deviceParams.getDeviceIds())
                                .map(ids -> ids.toArray(new Integer[0])).orElse(new Integer[]{})), Types.ARRAY)
                .addValue(BRAND_ID, deviceParams.getBrandId(), Types.INTEGER)
                .addValue(MODEL_ID, deviceParams.getModelId(), Types.INTEGER)
                .addValue(TYPE_ID, deviceParams.getTypeId(), Types.INTEGER)
                .addValue(STATUS_ID, deviceParams.getStatusId(), Types.INTEGER)
                .addValue(ENTRY_DATE, deviceParams.getEntryDate(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue(DEPARTURE_DATE, deviceParams.getDepartureDate(),  Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue(CUSTOMER_ID, deviceParams.getCustomerId(), Types.INTEGER)
                .addValue(CUSTOMER_NAME, deviceParams.getCustomerName(), Types.VARCHAR)
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
}
