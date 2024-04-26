package com.tproject.workshop.dto.customer;

import com.tproject.workshop.model.Sale;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InputSellDto {
    private BigDecimal total;
    private BigDecimal paidTotal;
    private String sellType;
    private String cellPhone;
    private int idCustomer;

    public Sale toSellModel(){
        Sale inputSale = new Sale();
        inputSale.setSellType(sellType);
        inputSale.setTotal(total);
        inputSale.setPaidTotal(paidTotal);


        return inputSale;
    }
}
