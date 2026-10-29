package com.acme.claims.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Date helpers used across the application.
 *
 * <p>NOTE: the formatters below are shared static {@link SimpleDateFormat}
 * instances. They are used from request threads and from the escalation
 * timer thread.</p>
 */
public final class DateUtils {

    public static final String DATE_PATTERN = "dd/MM/yyyy";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
    public static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(DATETIME_PATTERN);

    private static final long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

    static {
        DATE_FORMAT.setLenient(false);
    }

    private DateUtils() {
    }

    public static Date parseDate(String value) throws ParseException {
        if (value == null || value.trim().length() == 0) {
            throw new ParseException("Date value is empty", 0);
        }
        return DATE_FORMAT.parse(value.trim());
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return DATETIME_FORMAT.format(date);
    }

    /**
     * Whole days between two dates (end - start).
     */
    public static int daysBetween(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        return (int) (diff / MILLIS_PER_DAY);
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    public static Date daysAgo(int days) {
        return addDays(new Date(), -days);
    }

    public static boolean isInFuture(Date date) {
        return date != null && date.after(new Date());
    }

    public static Date startOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static int calculateAge(Date dateOfBirth) {
        Calendar dob = Calendar.getInstance();
        dob.setTime(dateOfBirth);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
}
