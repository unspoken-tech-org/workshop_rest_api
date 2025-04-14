package com.tproject.workshop.controller;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.model.CustomerContact;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface CustomerContactController {

  @PostMapping
  CustomerContact save(@RequestBody CustomerContactInputDto contact);

}
