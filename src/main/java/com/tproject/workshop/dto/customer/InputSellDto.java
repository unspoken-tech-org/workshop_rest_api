package com.tproject.workshop.dto.customer;

import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.Sell;

public class InputSellDto {
    private String name;
    private float total;
    private float totalPay;
    private String selltype;
    private String cellPhone;
    private int idCustomer;

    public Sell toSellModel(){
        Sell inputSell = new Sell();
        inputSell.setName(name);
        inputSell.setSellType(selltype);
        inputSell.setTotal(total);
        inputSell.setTotalPay(totalPay);


        return inputSell;
    }
}
