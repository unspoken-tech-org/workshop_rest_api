package com.tproject.workshop.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class UtilsString {

    public static String capitalizeEachWord(String str) {
        return Arrays.stream(str.split(" ")).map(StringUtils::capitalize).reduce((a, b) -> a + " " + b).orElse("");
    }

    public static String normalizeString(String paymentType) {
        return paymentType.toLowerCase()
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úùû]", "u")
                .replaceAll("[ç]", "c");
    }


}
