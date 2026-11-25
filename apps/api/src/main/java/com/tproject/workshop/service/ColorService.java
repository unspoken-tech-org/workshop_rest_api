package com.tproject.workshop.service;

import com.tproject.workshop.dto.color.ColorResponseDto;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.WorkshopException;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.repository.ColorRepository;
import com.tproject.workshop.utils.CatalogName;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColorService {
    private final ColorRepository colorRepository;
    private final ObservabilityEventRecorder eventRecorder;

    public List<ColorResponseDto> getColorsDto() {
        return colorRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public Color createOrReturnExistentColor(String color) {
        return CatalogName.findOrCreate(
                color,
                colorRepository::findByColorIgnoreCase,
                colorRepository::insertIgnore,
                () ->
                        eventRecorder.record(
                                ObservabilityEventCatalog.CATALOG_ENTRY_CREATED,
                                Map.of("catalog.kind", "color")),
                () -> new WorkshopException("Erro ao criar ou buscar cor: " + color));
    }

    private ColorResponseDto toDto(Color color) {
        return new ColorResponseDto(color.getIdColor(), color.getColor());
    }
}
