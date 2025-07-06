package com.tproject.workshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

import com.tproject.workshop.model.Color;

public interface ColorsController {

    @GetMapping
    public List<Color> getColors();

    
} 