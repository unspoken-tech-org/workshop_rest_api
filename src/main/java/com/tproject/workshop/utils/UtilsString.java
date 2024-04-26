package com.tproject.workshop.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class UtilsString {

    public  static String capitalizeEachWord(String str){
        return Arrays.stream(str.split(" ")).map(StringUtils::capitalize).reduce((a, b) -> a + " " + b).orElse("");
    }
}
