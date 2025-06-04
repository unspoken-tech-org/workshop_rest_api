package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhoneService {
    private final PhoneRepository phoneRepository;

    public Phone findById(int id) {
        return phoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Não foi encontrado um telefone com o id %d", id)));
    }
}
