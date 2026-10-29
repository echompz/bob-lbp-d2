package com.acme.claims.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class MoneyUtils {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(value);
    }

    public static double percentage(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return 0d;
        }
        Double ratio = new Double(part.doubleValue() / whole.doubleValue());
        return ratio.doubleValue() * 100;
    }
}
