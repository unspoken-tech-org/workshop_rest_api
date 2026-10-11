package com.tproject.workshop.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class UtilsString {

    public static String capitalizeEachWord(String str) {
        return Arrays.stream(str.split(" ")).map(StringUtils::capitalize).reduce((a, b) -> a + " " + b).orElse("");
    }

    public static String normalizeString(String value) {
        return value.toLowerCase()
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úùû]", "u")
                .replaceAll("[ç]", "c");
    }

    public static String onlyDigits(String value) {
        if (value == null) {
            return null;
        }

        return value.replaceAll("\\D", "");
    }

    /**
     * Formats a phone number to the Brazilian standard.
     * Example output:
     * - Mobile: (11) 91234-5678
     * - Landline: (11) 1234-5678
     * Accepts only digits, ignores other characters.
     *
     * @param phoneNumber String containing the phone number (with or without mask)
     * @return Formatted string or the original value if formatting is not possible
     */
    public static String formatPhoneNumberBR(String phoneNumber) {
        if (phoneNumber == null) return null;
        String digits = onlyDigits(phoneNumber);
        if (digits.length() == 11) {
            // Mobile: (XX) 9XXXX-XXXX
            return String.format("(%s) %s-%s",
                    digits.substring(0, 2),
                    digits.substring(2, 7),
                    digits.substring(7, 11));
        } else if (digits.length() == 10) {
            // Landline: (XX) XXXX-XXXX
            return String.format("(%s) %s-%s",
                    digits.substring(0, 2),
                    digits.substring(2, 6),
                    digits.substring(6, 10));
        } else {
            // Não é possível formatar, retorna original
            return phoneNumber;
        }
    }


    public static String formatCpf(String cpf) {
        if (cpf == null) return null;
        String digits = onlyDigits(cpf);
        if (digits.length() == 11) {
            return String.format("%s.%s.%s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6, 9),
                    digits.substring(9, 11)
            );
        } else {
            return cpf;
        }
    }


}
