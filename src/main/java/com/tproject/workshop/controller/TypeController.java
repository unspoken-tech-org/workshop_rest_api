package com.tproject.workshop.controller;

import com.tproject.workshop.model.Type;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface TypeController {

  @GetMapping
  List<Type> findAll(@RequestParam(required = false) String name);
}
