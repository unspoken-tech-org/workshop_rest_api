package com.tproject.workshop.repository.jdbc.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.repository.jdbc.CustomerRepositoryJdbc;
import com.tproject.workshop.utils.UtilsSql;
import com.tproject.workshop.utils.UtilsString;
import com.tproject.workshop.utils.mapper.JsonResultSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryJdbcImpl implements CustomerRepositoryJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public Optional<CustomerOutputDto> findCustomerById(int id) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CUSTOMER_ID", id, Types.INTEGER);


        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    UtilsSql.getQuery("customer/getCustomer"),
                    params,
                    getCustomerOutputDtoMapper()
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<CustomerListOutputDto> searchCustomers(CustomerFilterDto filters) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID", filters.id(), Types.INTEGER)
                .addValue("CPF", UtilsString.onlyDigits(filters.cpf()), Types.VARCHAR)
                .addValue("PHONE", filters.phone(), Types.VARCHAR)
                .addValue("SEARCH_NAME", filters.searchName(), Types.VARCHAR)
                .addValue("SEARCH_EMAIL", filters.searchEmail(), Types.VARCHAR)
                .addValue("ORDER_BY_FIELD", filters.ordenation().orderByField(), Types.VARCHAR)
                .addValue("ORDER_BY_DIRECTION", filters.ordenation().orderByDirection().toString(), Types.VARCHAR)
                .addValue("PAGE_SIZE", filters.size(), Types.INTEGER)
                .addValue("OFFSET", filters.page() * filters.size(), Types.INTEGER);

        List<CustomerListOutputDto> customers = jdbcTemplate.query(
                UtilsSql.getQuery("customer/searchCustomers"),
                params,
                getCustomerListOutputDtoMapper()
        );

        Long total = jdbcTemplate.queryForObject(
                UtilsSql.getQuery("customer/searchCustomers.count"), params, Long.class);

        return new PageImpl<>(customers,
                PageRequest.of(filters.page(), filters.size()),
                total != null ? total : 0);
    }


    private RowMapper<CustomerListOutputDto> getCustomerListOutputDtoMapper() {
        return (rs, rowNum) -> {
            CustomerListOutputDto dto = new CustomerListOutputDto();
            dto.setId(rs.getInt("id"));
            dto.setName(rs.getString("name"));
            dto.setCpf(rs.getString("cpf"));
            dto.setEmail(rs.getString("email"));
            dto.setGender(rs.getString("gender"));
            dto.setInsertDate(rs.getTimestamp("insert_date").toLocalDateTime());
            dto.setMainPhone(rs.getString("main_phone"));
            return dto;
        };
    }

    private RowMapper<CustomerOutputDto> getCustomerOutputDtoMapper() {
        return (rs, rowNum) -> {
            CustomerOutputDto dto = new CustomerOutputDto();

            dto.setCustomerId(rs.getInt("id"));
            dto.setName(rs.getString("name"));
            dto.setCpf(rs.getString("cpf"));
            dto.setGender(rs.getString("gender"));
            dto.setEmail(rs.getString("email"));
            dto.setInsertDate(rs.getString("insert_date"));

            dto.setPhones(
                    JsonResultSetMapper.readJsonList(rs, "phones", new TypeReference<>() {
                    }, objectMapper)
            );

            dto.setCustomerDevices(
                    JsonResultSetMapper.readJsonList(rs, "customer_devices", new TypeReference<>() {
                    }, objectMapper)
            );

            return dto;
        };
    }
}
