package com.tproject.workshop.repository.jdbc.impl;

import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import com.tproject.workshop.repository.jdbc.ModelRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ModelRepositoryJdbcImpl implements ModelRepositoryJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Page<ModelResponseDto> searchModels(ModelSearchParam params) {
        MapSqlParameterSource sqlParams = new MapSqlParameterSource()
                .addValue("TYPE_ID", params.typeId(), Types.INTEGER)
                .addValue("BRAND_ID", params.brandId(), Types.INTEGER)
                .addValue("QUERY", params.query(), Types.VARCHAR)
                .addValue("PAGE_SIZE", params.size(), Types.INTEGER)
                .addValue("OFFSET", params.page() * params.size(), Types.INTEGER);

        List<ModelResponseDto> models = jdbcTemplate.query(
                UtilsSql.getQuery("model/searchModels"),
                sqlParams,
                getModelResponseDtoMapper()
        );

        Long total = jdbcTemplate.queryForObject(
                UtilsSql.getQuery("model/searchModels.count"), sqlParams, Long.class);

        return new PageImpl<>(models,
                PageRequest.of(params.page(), params.size()),
                total != null ? total : 0);
    }

    private RowMapper<ModelResponseDto> getModelResponseDtoMapper() {
        return (rs, rowNum) -> new ModelResponseDto(
                rs.getInt("id_model"),
                rs.getString("model")
        );
    }
}
