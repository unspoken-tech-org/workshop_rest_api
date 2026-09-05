package com.tproject.workshop.service;

import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Model createOrReturnExistentModel(String model) {
        var modelName = model.toLowerCase().trim().replaceAll("\\s+", " ");
        var modelFound = modelRepository.findByModelIgnoreCase(modelName);

        if (modelFound.isPresent()) {
            return modelFound.get();
        }

        try {
            var newModel = new Model();
            newModel.setModel(modelName);

            return modelRepository.save(newModel);
        } catch (DataIntegrityViolationException e) {
            return findModelOnCreate(modelName, model);
        }
    }

    private Model findModelOnCreate(String modelName, String originalModel) {
        return modelRepository.findByModelIgnoreCase(modelName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar modelo: " + originalModel));
    }

    @Transactional(readOnly = true)
    public Page<ModelResponseDto> searchModels(ModelSearchParam params) {
        return modelRepository.searchModels(params);
    }

    @Transactional
    public ModelResponseDto createModel(String modelName) {
        Model model = createOrReturnExistentModel(modelName);
        return new ModelResponseDto(model.getIdModel(), model.getModel());
    }

}
