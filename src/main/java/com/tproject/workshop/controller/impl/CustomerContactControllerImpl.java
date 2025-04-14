package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.CustomerContactController;
import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.service.CustomerContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/customer-contact")
@RequiredArgsConstructor
public class CustomerContactControllerImpl implements CustomerContactController {

  private final CustomerContactService customerContactService;

  @Override
  public CustomerContact save(CustomerContactInputDto contact) {
    return customerContactService.save(contact);
  }
}
