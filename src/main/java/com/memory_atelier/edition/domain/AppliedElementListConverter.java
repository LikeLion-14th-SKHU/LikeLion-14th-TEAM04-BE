package com.memory_atelier.edition.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** {@link AppliedElement} 목록을 JSON 한 컬럼에 담는다(조인 테이블 대신). */
@Converter
public class AppliedElementListConverter implements AttributeConverter<List<AppliedElement>, String> {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final TypeReference<List<AppliedElement>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<AppliedElement> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("재창조 요소 목록을 JSON으로 변환하지 못했습니다.", e);
        }
    }

    @Override
    public List<AppliedElement> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON을 재창조 요소 목록으로 변환하지 못했습니다.", e);
        }
    }
}
