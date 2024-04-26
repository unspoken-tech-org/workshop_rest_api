package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.SellController;
import com.tproject.workshop.dto.customer.InputSellDto;
import com.tproject.workshop.model.Sale;
import com.tproject.workshop.service.SellService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SellControllerImpl implements SellController {
    private final SellService sellService;

    @Override
    public List<Sale> list(){
        return sellService.findAllSell();
    }
    @Override
    public Sale findById(int idSell){
        return sellService.findById(idSell);
    }
    @Override
    public Sale create(InputSellDto inputSellDto){
        return sellService.addSell(inputSellDto);
    }
}
