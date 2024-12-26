package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.TypeController;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.service.TypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/type")
public class TypeControllerImpl implements TypeController {
    private final TypeService typeService;

    @Override
    public List<Type> findAll(String name) {
        return typeService.findAll(name);
    }

}
