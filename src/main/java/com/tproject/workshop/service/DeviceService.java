package com.tproject.workshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper mapper;

    public List<DeviceTableDto> list(DeviceQueryParam params){
        return deviceRepository.listTable(params);
    }
}
