package com.tproject.workshop.utils;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.beans.BeanUtils;

public class MapUtils {

  public static String[] getNullPropertyNames(Object source) {
    return Arrays.stream(BeanUtils.getPropertyDescriptors(source.getClass()))
        .map(PropertyDescriptor::getName)
        .filter(name -> {
          try {
            return Objects.requireNonNull(BeanUtils.getPropertyDescriptor(source.getClass(), name))
                .getReadMethod()
                .invoke(source) == null;
          } catch (Exception e) {
            return false;
          }
        })
        .toArray(String[]::new);
  }

}
