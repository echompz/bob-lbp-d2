package com.acme.claims.report;

import com.acme.claims.util.DateUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Date;

/**
 * JAXB adapter that renders dates as dd/MM/yyyy.
 */
public class DateAdapter extends XmlAdapter<String, Date> {

    @Override
    public Date unmarshal(String value) throws Exception {
        return value == null ? null : DateUtils.parseDate(value);
    }

    @Override
    public String marshal(Date value) throws Exception {
        return value == null ? null : DateUtils.formatDate(value);
    }
}
