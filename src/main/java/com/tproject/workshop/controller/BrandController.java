package com.tproject.workshop.controller;

import com.tproject.workshop.model.Brand;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface BrandController {

  @GetMapping
  List<Brand> list(@RequestParam(required = false) String name);

}
