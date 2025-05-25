package com.mihaiciobotaru.eshop.converter;

import com.mihaiciobotaru.eshop.models.CartItem;

import jakarta.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CartItemConverter implements AttributeConverter<List<CartItem>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(CartItemConverter.class);

    @Override
    public String convertToDatabaseColumn(List<CartItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing List<CartItem> to JSON: {}", e.getMessage());
            throw new RuntimeException("Error serializing List<CartItem> to JSON", e);
        }
    }

    @Override
    public List<CartItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<CartItem>>() {});
        } catch (IOException e) {
            logger.error("Error deserializing JSON to List<CartItem>: {}", e.getMessage());
            throw new RuntimeException("Error deserializing JSON to List<CartItem>", e);
        }
    }
    
}
