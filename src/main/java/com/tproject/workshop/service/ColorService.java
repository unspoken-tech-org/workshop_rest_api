package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.ColorInputDtoRecord;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.repository.ColorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ColorService {
    private final ColorRepository colorRepository;

    public List<Color> getColors() {
        return colorRepository.findAll();
    }

    public Color save(ColorInputDtoRecord colorInput) {
        var colorFound = colorRepository.findByColorIgnoreCase(colorInput.color());
        if (colorFound.isPresent()) {
            return colorFound.get();
        }
        var newColor = new Color();
        newColor.setColor(colorInput.color().toLowerCase());
        return colorRepository.save(newColor);
    }
}
