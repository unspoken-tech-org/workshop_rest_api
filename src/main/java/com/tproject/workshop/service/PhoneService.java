package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PhoneService {
    private final PhoneRepository cellphoneRepository;

    public Phone findByNumber(String number) {
        return cellphoneRepository.findByNumber(number)
                .orElseThrow(() -> new NotFoundException(String.format("O numero %s não está cadastrado", number)));
    }

}
