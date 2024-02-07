package com.tproject.workshop.dto.customer;

import com.tproject.workshop.model.Cellphone;
import lombok.Data;

@Data
public class InputCellphoneDto {
    private int number;
    private int whatsapp;
    private char type;

    public Cellphone toCellphoneModel(){
        Cellphone inputCellphone = new Cellphone();
        inputCellphone.setNumber(number);
        inputCellphone.setWhatsapp(whatsapp);
        inputCellphone.setType(type);

        return inputCellphone;
    }
}