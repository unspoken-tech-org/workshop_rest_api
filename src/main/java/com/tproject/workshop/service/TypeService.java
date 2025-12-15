package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeService {
    private final TypeRepository typeRepository;

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
        var typeFound = typeRepository.findByTypeIgnoreCase(type);

        if (typeFound.isPresent()) {
            return typeFound.get();
        }

        var typeName = type.toLowerCase().trim().replaceAll("\\s+", " ");
        try {
            var newType = new Type();
            newType.setType(typeName);

            return typeRepository.save(newType);
        } catch (DataIntegrityViolationException e) {
            return findTypeOnCreate(typeName, type);
        }
    }

    private Type findTypeOnCreate(String typeName, String originalType) {
        return typeRepository.findByTypeIgnoreCase(typeName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar tipo: " + originalType));
    }

}
