package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.repository.ColorRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ColorService {
    private final ColorRepository colorRepository;

    public List<Color> getColors() {
        return colorRepository.findAll();
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
            return colorRepository.save(newColor);
        } catch (DataIntegrityViolationException e) {
            return findColorOnCreate(colorName, color);
        }
    }

    private Color findColorOnCreate(String colorName, String originalColor) {
        return colorRepository.findByColorIgnoreCase(colorName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar cor: " + originalColor));
    }
}
