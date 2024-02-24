package com.tproject.workshop.dto.cellphone;

import com.tproject.workshop.model.Cellphone;
import lombok.Data;

@Data
public class InputCellphoneDto {
    private String number;
    private boolean whatsapp;
    private String type;

    public Cellphone toCellphoneModel(){
        Cellphone inputCellphone = new Cellphone();
        inputCellphone.setNumber(number);
        inputCellphone.setWhatsapp(whatsapp);
        inputCellphone.setType(type);

        return inputCellphone;
    }
}