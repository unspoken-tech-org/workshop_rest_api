package com.tproject.workshop.controller;

import com.tproject.workshop.model.Technician;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface TechnicianController {

    @GetMapping("/{id}")
    Technician find(@PathVariable("id") Integer id);

    @GetMapping("/list")
    List<Technician> list();

}
