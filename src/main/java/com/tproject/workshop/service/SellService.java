package com.tproject.workshop.service;

import com.tproject.workshop.dto.customer.InputSellDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Sell;
import com.tproject.workshop.repository.SellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellService {
    private final SellRepository sellRepository;

    public List<Sell> findAllSell(){
        return sellRepository.findAll();
    }

    public Sell findById(int idSell){
        return sellRepository.findById(idSell).orElseThrow(() -> new NotFoundException("Não existe venda com id " + idSell));
    }

    public Sell addSell(InputSellDto inputSellDto){
        return sellRepository.save(inputSellDto.toSellModel());
    }
}
