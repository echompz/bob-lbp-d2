package com.acme.claims.report;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
public class StatusTotal {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private long count;

    @XmlAttribute
    private BigDecimal amount;

    public StatusTotal() {
    }

    public StatusTotal(String name, long count, BigDecimal amount) {
        this.name = name;
        this.count = count;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
