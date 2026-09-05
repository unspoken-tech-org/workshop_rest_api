package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.TypeController;
import com.tproject.workshop.dto.type.CreateTypeRequest;
import com.tproject.workshop.dto.type.TypeResponseDto;
import com.tproject.workshop.dto.type.TypeSearchParam;
import com.tproject.workshop.service.TypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/type")
public class TypeControllerImpl implements TypeController {
    private final TypeService typeService;

    @Override
    public List<TypeResponseDto> findAll(String name) {
        return typeService.findAllDto(name);
    }

    @Override
    public Page<TypeResponseDto> search(@Valid TypeSearchParam params) {
        return typeService.searchTypes(params);
    }

    @Override
    public TypeResponseDto create(CreateTypeRequest request) {
        return typeService.createType(request.type());
    }
}

