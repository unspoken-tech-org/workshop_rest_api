package com.tproject.workshop.dto.customer;

import com.tproject.workshop.dto.cellphone.CellPhoneOutputDeviceDto;
import com.tproject.workshop.dto.device.MinifiedDeviceTableOutputDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@FieldNameConstants(asEnum = true)
@NoArgsConstructor
public class CustomerOutputDto {

    private Integer customerId;
    private String name;
    private String cpf;
    private String gender;
    private String email;
    private String insertDate;
    private List<CellPhoneOutputDeviceDto> phones;
    private List<MinifiedDeviceTableOutputDto> customerDevices;


}
