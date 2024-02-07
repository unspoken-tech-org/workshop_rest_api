package com.tproject.workshop.controller;

import com.tproject.workshop.dto.customer.InputSellDto;
import com.tproject.workshop.model.Sell;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface SellController {

    @GetMapping
    List<Sell> list();

    @GetMapping("/{id}")
    Sell findById(@PathVariable("id") int idSell);

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Sell create(@RequestBody @NotNull InputSellDto inputSellDto);

}
