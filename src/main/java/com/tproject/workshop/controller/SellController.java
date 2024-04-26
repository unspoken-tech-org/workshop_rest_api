package com.tproject.workshop.controller;

import com.tproject.workshop.dto.customer.InputSellDto;
import com.tproject.workshop.model.Sale;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface SellController {

    @GetMapping
    List<Sale> list();

    @GetMapping("/{id}")
    Sale findById(@PathVariable("id") int idSell);

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Sale create(@RequestBody @NotNull InputSellDto inputSellDto);

}
