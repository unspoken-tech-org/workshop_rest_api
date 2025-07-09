package com.tproject.workshop.service;

import com.tproject.workshop.dto.customer.InputSellDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Sale;
import com.tproject.workshop.repository.SellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellService {
    private final SellRepository sellRepository;

    public List<Sale> findAllSell() {
        return sellRepository.findAll();
    }

    public Sale findById(int idSell) {
        return sellRepository.findById(idSell).orElseThrow(() -> new NotFoundException("Não existe venda com id " + idSell));
    }

    public Sale addSell(InputSellDto inputSellDto) {
        return sellRepository.save(inputSellDto.toSellModel());
    }
}
