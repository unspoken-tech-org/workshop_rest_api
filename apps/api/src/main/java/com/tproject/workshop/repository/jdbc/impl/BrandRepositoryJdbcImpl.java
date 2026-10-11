package com.tproject.workshop.repository.jdbc.impl;

import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.repository.jdbc.BrandRepositoryJdbc;
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
public class BrandRepositoryJdbcImpl implements BrandRepositoryJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Page<BrandResponseDto> searchBrands(BrandSearchParam params) {
        MapSqlParameterSource sqlParams = new MapSqlParameterSource()
                .addValue("QUERY", params.query(), Types.VARCHAR)
                .addValue("PAGE_SIZE", params.size(), Types.INTEGER)
                .addValue("OFFSET", params.page() * params.size(), Types.INTEGER);

        List<BrandResponseDto> brands = jdbcTemplate.query(
                UtilsSql.getQuery("brand/searchBrands"),
                sqlParams,
                getBrandResponseDtoMapper()
        );

        Long total = jdbcTemplate.queryForObject(
                UtilsSql.getQuery("brand/searchBrands.count"), sqlParams, Long.class);

        return new PageImpl<>(brands,
                PageRequest.of(params.page(), params.size()),
                total != null ? total : 0);
    }

    private RowMapper<BrandResponseDto> getBrandResponseDtoMapper() {
        return (rs, rowNum) -> new BrandResponseDto(
                rs.getInt("id_brand"),
                rs.getString("brand")
        );
    }
}
