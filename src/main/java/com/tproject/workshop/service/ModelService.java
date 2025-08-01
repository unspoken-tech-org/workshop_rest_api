package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ModelService {

    private final ModelRepository modelRepository;

    public List<Model> findAll() {
        return modelRepository.findAll();
    }

    public Model findById(int id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Modelo com id %d não encontrada", id)));
    }

    public Model createOrReturnExistentModel(String model) {
        var modelFound = modelRepository.findByModelIgnoreCase(model);
        if (modelFound.isPresent()) {
            return modelFound.get();
        }
        var modelName = model.toLowerCase();
        var newModel = new Model();
        newModel.setModel(modelName);

        return modelRepository.save(newModel);
    }

}
