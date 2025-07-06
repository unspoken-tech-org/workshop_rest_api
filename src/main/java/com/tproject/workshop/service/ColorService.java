package com.tproject.workshop.service;

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

    public Color createOrReturnExistentColor(String color) {
        var colorFound = colorRepository.findByColorIgnoreCase(color);
        if (colorFound.isPresent()) {
            return colorFound.get();
        }
        var newColor = new Color();
        newColor.setColor(color.toLowerCase());
        return colorRepository.save(newColor);
    }
}
