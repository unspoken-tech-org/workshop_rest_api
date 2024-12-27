package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.BrandController;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.service.BrandService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/brand")
public class BrandControllerImpl implements BrandController {

  final BrandService brandService;

  @Override
  public List<Brand> list(String name) {
    return brandService.findAll(name);
  }
}
