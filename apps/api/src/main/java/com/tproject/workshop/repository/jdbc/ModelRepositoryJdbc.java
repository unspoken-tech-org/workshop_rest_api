package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import org.springframework.data.domain.Page;

public interface ModelRepositoryJdbc {
    Page<ModelResponseDto> searchModels(ModelSearchParam params);
}
