package com.tproject.workshop.dto.customer;

import com.tproject.workshop.model.Sell;
import lombok.Data;

@Data
public class InputSellDto {
    private float total;
    private float paidTotal;
    private String sellType;
    private String cellPhone;
    private int idCustomer;

    public Sell toSellModel(){
        Sell inputSell = new Sell();
        inputSell.setSellType(sellType);
        inputSell.setTotal(total);
        inputSell.setPaidTotal(paidTotal);


        return inputSell;
    }
}
