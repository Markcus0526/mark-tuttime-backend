package com.tuttime.common.util;

import java.math.BigDecimal;

public class NumberUtil {

    public static BigDecimal stringToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // Handle cases where the string isn't a valid number
            return BigDecimal.ZERO;
        }
    }

}
