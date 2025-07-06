package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.ColorsController;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.service.ColorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/colors")
@AllArgsConstructor
public class ColorsControllerImpl implements ColorsController {

    private final ColorService colorService;

    @Override
    public List<Color> getColors() {
        return colorService.getColors();
    }
}
