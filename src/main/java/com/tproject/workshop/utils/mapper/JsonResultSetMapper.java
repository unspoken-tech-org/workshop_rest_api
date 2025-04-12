package com.tproject.workshop.utils.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class JsonResultSetMapper {
  public static <T> List<T> readJsonList(ResultSet rs, String columnName, TypeReference<List<T>> typeRef, ObjectMapper objectMapper) {
    try {
      String json = rs.getString(columnName);
      if (json != null && !json.isBlank()) {
        return objectMapper.readValue(json, typeRef);
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON column: " + columnName, e);
    }
    return Collections.emptyList();
  }
}
