package com.tproject.workshop.service;

import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Technician;
import com.tproject.workshop.repository.TechnicianRepository;
import com.tproject.workshop.utils.CatalogName;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public List<TechnicianResponseDto> findAll() {
        return technicianRepository.findAll().stream().map(this::toDto).toList();
    }

    public TechnicianResponseDto findByIdDto(int id) {
        return toDto(findById(id));
    }

    public Technician findById(int id) {
        return technicianRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Técnico com id %d não encontrado", id)));
    }

    @Transactional
    public Technician save(Technician technician) {
        if (technician == null || technician.getName() == null || technician.getName().isBlank()) {
            throw new BadRequestException("O nome do técnico é obrigatório");
        }
        String number = Optional.ofNullable(technician.getNumber()).orElse("");
        return CatalogName.findOrCreate(
                technician.getName(),
                technicianRepository::findByNameIgnoreCase,
                normalized -> technicianRepository.insertIgnore(normalized, number),
                null,
                () ->
                        new NotFoundException(
                                "Erro ao criar ou buscar técnico: " + technician.getName()));
    }

    private TechnicianResponseDto toDto(Technician technician) {
        return new TechnicianResponseDto(
                technician.getId(), technician.getName(), technician.getNumber());
    }
}
