package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneService {
    private final PhoneRepository cellphoneRepository;

    public Phone findByNumber(String number) {
        return cellphoneRepository
                .findByNumber(number)
                .orElseThrow(() -> new NotFoundException("O número informado não está cadastrado"));
    }
}
