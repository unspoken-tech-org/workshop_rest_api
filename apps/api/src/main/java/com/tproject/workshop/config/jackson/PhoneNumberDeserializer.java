package com.tproject.workshop.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.tproject.workshop.utils.UtilsString;

import java.io.IOException;

/**
 * Deserializer to remove special characters from phone number
 */
public class PhoneNumberDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String phoneNumber = p.getText();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        return UtilsString.onlyDigits(phoneNumber);
    }
} 