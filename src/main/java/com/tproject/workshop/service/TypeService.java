package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.TypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TypeService {
    private final TypeRepository typeRepository;

    public Type findById(int id){
        return typeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Tipo com id %d não encontrado", id)));
    }

    public List<Type> findAll(String name){
        if(name == null || name.trim().isEmpty()){
        return typeRepository.findAll();
        }

        return typeRepository.findByTypeContainingIgnoreCase(name);
    }


}
