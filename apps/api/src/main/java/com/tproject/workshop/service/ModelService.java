package com.tproject.workshop.service;

import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.repository.ModelRepository;
import com.tproject.workshop.utils.CatalogName;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ModelService {

    private final ModelRepository modelRepository;
    private final ObservabilityEventRecorder eventRecorder;

    public List<Model> findAll() {
        return modelRepository.findAll();
    }

    public Model findById(int id) {
        return modelRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Modelo com id %d não encontrada", id)));
    }

    @Transactional
    public Model createOrReturnExistentModel(String model) {
        return CatalogName.findOrCreate(
                model,
                modelRepository::findByModelIgnoreCase,
                modelRepository::insertIgnore,
                () ->
                        eventRecorder.record(
                                ObservabilityEventCatalog.CATALOG_ENTRY_CREATED,
                                Map.of("catalog.kind", "model")),
                () -> new NotFoundException("Erro ao criar ou buscar modelo: " + model));
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
