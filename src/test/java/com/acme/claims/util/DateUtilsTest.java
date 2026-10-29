package com.acme.claims.util;

import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DateUtilsTest {

    @Test
    public void parsesAndFormatsDdMmYyyy() throws ParseException {
        Date date = DateUtils.parseDate("15/03/2024");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("15/03/2024", DateUtils.formatDate(date));
    }

    @Test(expected = ParseException.class)
    public void rejectsImpossibleDates() throws ParseException {
        DateUtils.parseDate("31/02/2024");
    }

    @Test(expected = ParseException.class)
    public void rejectsIsoFormat() throws ParseException {
        DateUtils.parseDate("2024-03-15");
    }

    @Test
    public void formatsNullAsNull() {
        assertNull(DateUtils.formatDate(null));
        assertNull(DateUtils.formatDateTime(null));
    }

    @Test
    public void daysBetweenCountsWholeDays() throws ParseException {
        assertEquals(10, DateUtils.daysBetween(DateUtils.parseDate("01/01/2024"), DateUtils.parseDate("11/01/2024")));
        assertEquals(366, DateUtils.daysBetween(DateUtils.parseDate("01/01/2024"), DateUtils.parseDate("01/01/2025")));
    }

    @Test
    public void addDaysAndFutureCheck() {
        Date tomorrow = DateUtils.addDays(new Date(), 1);
        assertTrue(DateUtils.isInFuture(tomorrow));
        assertFalse(DateUtils.isInFuture(DateUtils.daysAgo(1)));
    }

    @Test
    public void calculatesAgeBeforeAndAfterBirthday() {
        Calendar dob = Calendar.getInstance();
        dob.add(Calendar.YEAR, -30);
        dob.add(Calendar.DAY_OF_MONTH, -1);
        assertEquals(30, DateUtils.calculateAge(dob.getTime()));

        dob = Calendar.getInstance();
        dob.add(Calendar.YEAR, -30);
        dob.add(Calendar.DAY_OF_MONTH, 1);
        assertEquals(29, DateUtils.calculateAge(dob.getTime()));
    }
}
