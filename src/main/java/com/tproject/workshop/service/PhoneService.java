package com.tproject.workshop.service;

import com.tproject.workshop.repository.PhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PhoneService {
    private final PhoneRepository cellphoneRepository;


}
