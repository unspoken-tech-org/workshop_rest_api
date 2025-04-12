package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Technician;
import com.tproject.workshop.repository.TechnicianRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public List<Technician> findAll(){
        return technicianRepository.findAll();
    }

    public Technician findById(int id){
        return technicianRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Técnico com id %d não encontrado", id)));
    }

    public Technician save(Technician technician){
        var technicianFound = findByName(technician.getName());
        if(technicianFound == null){
            var technicianName = UtilsString.capitalizeEachWord(technician.getName());
            technician.setName(technicianName);
            return technicianRepository.save(technician);
        }
        return technicianFound;
    }

    private Technician findByName(String name){
        return technicianRepository.findByNameIgnoreCase(name);
    }
}
