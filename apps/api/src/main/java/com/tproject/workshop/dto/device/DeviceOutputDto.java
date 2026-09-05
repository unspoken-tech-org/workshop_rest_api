package com.tproject.workshop.dto.device;

import com.tproject.workshop.dto.cellphone.CellPhoneOutputDeviceDto;
import com.tproject.workshop.dto.contact.CustomerContactOutputDto;
import com.tproject.workshop.dto.payment.PaymentDeviceOutputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@FieldNameConstants(asEnum = true)
@AllArgsConstructor
@NoArgsConstructor
public class DeviceOutputDto {
    private int deviceId;
    private int customerId;
    private String customerName;
    private String deviceStatus;
    private String brandName;
    private String modelName;
    private String typeName;
    private Integer technicianId;
    private String technicianName;
    private String problem;
    private String observation;
    private String budget;
    private BigDecimal budgetFee;
    private BigDecimal serviceValue;
    private List<String> deviceColors;
    private boolean hasUrgency;
    private boolean revision;
    private LocalDateTime entryDate;
    private LocalDateTime departureDate;
    private LocalDateTime lastUpdate;
    private List<CellPhoneOutputDeviceDto> customerPhones;
    private List<CustomerContactOutputDto> customerContacts;
    private List<MinifiedDeviceTableOutputDto> otherDevices;
    private List<PaymentDeviceOutputDto> payments;
}
