package com.tproject.workshop.service;

import com.tproject.workshop.dto.color.ColorResponseDto;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.repository.ColorRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ColorService {
    private final ColorRepository colorRepository;
    private final ObservabilityEventRecorder eventRecorder;

    public List<ColorResponseDto> getColorsDto() {
        return colorRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Color createOrReturnExistentColor(String color) {
        var colorFound = colorRepository.findByColorIgnoreCase(color);
        if (colorFound.isPresent()) {
            return colorFound.get();
        }

        var colorName = color.toLowerCase().trim().replaceAll("\\s+", " ");
        try {
            var newColor = new Color();
            newColor.setColor(colorName);
            Color saved = colorRepository.save(newColor);
            eventRecorder.record(ObservabilityEventCatalog.CATALOG_ENTRY_CREATED, Map.of("catalog.kind", "color"));
            return saved;
        } catch (DataIntegrityViolationException e) {
            return findColorOnCreate(colorName, color);
        }
    }

    private Color findColorOnCreate(String colorName, String originalColor) {
        return colorRepository.findByColorIgnoreCase(colorName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar cor: " + originalColor));
    }

    private ColorResponseDto toDto(Color color) {
        return new ColorResponseDto(
                color.getIdColor(),
                color.getColor()
        );
    }
}
