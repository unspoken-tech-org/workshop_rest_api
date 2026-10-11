package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.TechnicianController;
import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.service.TechnicianService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/technician")
public class TechnicianControllerImpl implements TechnicianController {

  private final TechnicianService technicianService;

  @Override
  public TechnicianResponseDto find(Integer id) {
    return technicianService.findByIdDto(id);
  }

  @Override
  public List<TechnicianResponseDto> list() {
    return technicianService.findAll();
  }
}
