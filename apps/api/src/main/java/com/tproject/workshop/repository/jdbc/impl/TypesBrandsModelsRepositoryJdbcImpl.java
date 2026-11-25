package com.tproject.workshop.repository.jdbc.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import com.tproject.workshop.exception.WorkshopException;
import com.tproject.workshop.repository.jdbc.TypesBrandsModelsRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TypesBrandsModelsRepositoryJdbcImpl implements TypesBrandsModelsRepositoryJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<TypeRecord> getTypesBrandsModels() {
        try {
            String jsonResult =
                    jdbcTemplate.queryForObject(
                            UtilsSql.getQuery("brands-models-types/listBrandsModelsTypes"),
                            Map.of(),
                            String.class);

            if (jsonResult == null || jsonResult.isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(jsonResult, new TypeReference<List<TypeRecord>>() {});
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        } catch (Exception e) {
            throw new WorkshopException("Falha ao processar o resultado JSON do banco de dados", e);
        }
    }
}
