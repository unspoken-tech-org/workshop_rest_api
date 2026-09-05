package com.tproject.workshop.repository.jdbc.impl;

import com.tproject.workshop.dto.type.TypeResponseDto;
import com.tproject.workshop.dto.type.TypeSearchParam;
import com.tproject.workshop.repository.jdbc.TypeRepositoryJdbc;
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
public class TypeRepositoryJdbcImpl implements TypeRepositoryJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Page<TypeResponseDto> searchTypes(TypeSearchParam params) {
        MapSqlParameterSource sqlParams = new MapSqlParameterSource()
                .addValue("QUERY", params.query(), Types.VARCHAR)
                .addValue("PAGE_SIZE", params.size(), Types.INTEGER)
                .addValue("OFFSET", params.page() * params.size(), Types.INTEGER);

        List<TypeResponseDto> types = jdbcTemplate.query(
                UtilsSql.getQuery("type/searchTypes"),
                sqlParams,
                getTypeResponseDtoMapper()
        );

        Long total = jdbcTemplate.queryForObject(
                UtilsSql.getQuery("type/searchTypes.count"), sqlParams, Long.class);

        return new PageImpl<>(types,
                PageRequest.of(params.page(), params.size()),
                total != null ? total : 0);
    }

    private RowMapper<TypeResponseDto> getTypeResponseDtoMapper() {
        return (rs, rowNum) -> new TypeResponseDto(
                rs.getInt("id_type"),
                rs.getString("type")
        );
    }
}
