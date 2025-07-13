package com.tproject.workshop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.model.CustomerContact;

public interface CustomerContactController {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  CustomerContact save(@RequestBody CustomerContactInputDto contact);

}
