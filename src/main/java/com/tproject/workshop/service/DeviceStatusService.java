package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.DeviceStatus;
import com.tproject.workshop.repository.DeviceStatusRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DeviceStatusService {

    private final DeviceStatusRepository deviceStatusRepository;

    public List<DeviceStatus> findAll(){
        return deviceStatusRepository.findAll();
    }

    public DeviceStatus findById(int id){
        return deviceStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Status do dispositivo com id %d não encontrado", id)));
    }

    public DeviceStatus save(DeviceStatus deviceStatus){
        var deviceStatusFound = findByStatus(deviceStatus.getStatus());
        if(deviceStatusFound == null){
            var deviceStatusName = deviceStatus.getStatus();
            deviceStatus.setStatus(UtilsString.capitalizeEachWord(deviceStatusName));

            return deviceStatusRepository.save(deviceStatus);
        }
        return deviceStatus;
    }

    public DeviceStatus findByStatus(String status){
        return deviceStatusRepository.findByStatusIgnoreCase(status).orElseThrow(() ->
            new NotFoundException(String.format("Status do dispositivo com nome %s não encontrado", status))
        );
    }
}
