package com.tproject.workshop.dto.cellphone;

import com.tproject.workshop.model.Phone;
import lombok.Data;

@Data
public class InputCellphoneDto {
    private String number;
    private boolean whatsapp;
    private String type;

    public Phone toCellphoneModel(){
        Phone inputPhone = new Phone();
        inputPhone.setNumber(number);
        inputPhone.setWhatsapp(whatsapp);
        inputPhone.setType(type);

        return inputPhone;
    }
}