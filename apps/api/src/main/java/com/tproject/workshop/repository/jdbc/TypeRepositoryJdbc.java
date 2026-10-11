package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.type.TypeResponseDto;
import com.tproject.workshop.dto.type.TypeSearchParam;
import org.springframework.data.domain.Page;

public interface TypeRepositoryJdbc {
    Page<TypeResponseDto> searchTypes(TypeSearchParam params);
}
