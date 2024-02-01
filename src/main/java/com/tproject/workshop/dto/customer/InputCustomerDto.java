package com.tproject.workshop.dto.customer;

import com.tproject.workshop.model.Customer;
import lombok.Data;

@Data
public class InputCustomerDto {
    private String name;
    private String  cpf;
    private char gender;
    private String mail;
    private String phone;
    private String cellPhone;


//TODO: apply conversion model
    public Customer toCustomerModel(){
        Customer inputCustomer = new Customer();
        inputCustomer.setName(name);
        inputCustomer.setCpf(cpf);
        inputCustomer.setGender(gender);
        inputCustomer.setMail(mail);
        inputCustomer.setPhone(phone);
        inputCustomer.setCellPhone(cellPhone);

        return inputCustomer;
    }
}
