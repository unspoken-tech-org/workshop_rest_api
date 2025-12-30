package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Technician;
import com.tproject.workshop.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public List<Technician> findAll() {
        return technicianRepository.findAll();
    }

    public Technician findById(int id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Técnico com id %d não encontrado", id)));
    }

    @Transactional
    public Technician save(Technician technician) {
        var technicianFound = technicianRepository.findByNameIgnoreCase(technician.getName());
        if (technicianFound.isPresent()) {
            return technicianFound.get();
        }
        var technicianName = technician.getName().toLowerCase();
        technician.setName(technicianName);
        return technicianRepository.save(technician);
    }

}
