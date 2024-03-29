package com.tproject.workshop.service;

import com.tproject.workshop.dto.cellphone.InputCellphoneDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.CellphoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CellphoneService {
    private final CellphoneRepository cellphoneRepository;

    public List<Phone> findAllCellphones(){
        return cellphoneRepository.findAll();
    }
    public Phone findById(int idCellphone){
        return cellphoneRepository.findById(idCellphone)
                .orElseThrow(() -> new NotFoundException("Nao existe telefone com esse id" + idCellphone));
    }
    public Phone addCellphone(InputCellphoneDto inputCellphoneDto){
        return cellphoneRepository.save(inputCellphoneDto.toCellphoneModel());
    }
}
