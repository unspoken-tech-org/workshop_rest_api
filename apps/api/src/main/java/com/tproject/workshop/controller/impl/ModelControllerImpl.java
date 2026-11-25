package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.ModelController;
import com.tproject.workshop.dto.PageResponse;
import com.tproject.workshop.dto.model.CreateModelRequest;
import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import com.tproject.workshop.service.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/model")
public class ModelControllerImpl implements ModelController {

    private final ModelService modelService;

    @Override
    public PageResponse<ModelResponseDto> search(@Valid ModelSearchParam params) {
        return PageResponse.from(modelService.searchModels(params));
    }

    @Override
    public ModelResponseDto create(CreateModelRequest request) {
        return modelService.createModel(request.model());
    }
}
