package com.tproject.workshop.service;

import com.tproject.workshop.dto.type.TypeResponseDto;
import com.tproject.workshop.dto.type.TypeSearchParam;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TypeService {
    private final TypeRepository typeRepository;
    private final ObservabilityEventRecorder eventRecorder;


    public List<TypeResponseDto> findAllDto(String name) {
        return findAll(name).stream()
                .map(this::toDto)
                .toList();
    }

    public Type findById(int id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Tipo com id %d não encontrado", id)));
    }

    public List<Type> findAll(String name) {
        if (name == null || name.trim().isEmpty()) {
            return typeRepository.findAll();
        }

        return typeRepository.findByTypeContainingIgnoreCase(name);
    }


    @Transactional
    public Type createOrReturnExistentType(String type) {
        var typeName = type.toLowerCase().trim().replaceAll("\\s+", " ");
        var typeFound = typeRepository.findByTypeIgnoreCase(typeName);

        if (typeFound.isPresent()) {
            return typeFound.get();
        }

        try {
            var newType = new Type();
            newType.setType(typeName);

            Type saved = typeRepository.save(newType);
            eventRecorder.record(ObservabilityEventCatalog.CATALOG_ENTRY_CREATED, Map.of("catalog.kind", "type"));
            return saved;
        } catch (DataIntegrityViolationException e) {
            return findTypeOnCreate(typeName, type);
        }
    }

    private Type findTypeOnCreate(String typeName, String originalType) {
        return typeRepository.findByTypeIgnoreCase(typeName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar tipo: " + originalType));
    }
    
    private TypeResponseDto toDto(Type type) {
        return new TypeResponseDto(
                type.getIdType(),
                type.getType()
        );
    }

    @Transactional(readOnly = true)
    public Page<TypeResponseDto> searchTypes(TypeSearchParam params) {
        return typeRepository.searchTypes(params);
    }

    @Transactional
    public TypeResponseDto createType(String typeName) {
        Type type = createOrReturnExistentType(typeName);
        return new TypeResponseDto(type.getIdType(), type.getType());
    }

}
