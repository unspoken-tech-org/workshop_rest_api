package com.tproject.workshop.service;

import com.tproject.workshop.dto.customer.InputCellphoneDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Cellphone;
import com.tproject.workshop.repository.CellphoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CellphoneService {
    private final CellphoneRepository cellphoneRepository;

    public List<Cellphone> findAllCellphones(){
        return cellphoneRepository.findAll();
    }
    public Cellphone findById(int idCellphone){
        return cellphoneRepository.findById(idCellphone)
                .orElseThrow(() -> new NotFoundException("Nao existe telefone com esse id" + idCellphone));
    }
    public Cellphone addCellphone(InputCellphoneDto inputCellphoneDto){
        return cellphoneRepository.save(inputCellphoneDto.toCellphoneModel());
    }
}
