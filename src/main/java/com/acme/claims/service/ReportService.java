package com.acme.claims.service;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.report.ClaimReportLine;
import com.acme.claims.report.ClaimSummaryReport;
import com.acme.claims.report.StatusTotal;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.util.DateUtils;
import com.acme.claims.util.MoneyUtils;
import com.acme.claims.util.TempFileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ClaimRepository claimRepository;

    public ClaimSummaryReport buildClaimSummary() {
        List<Claim> claims = new ArrayList<Claim>(claimRepository.findAll());
        return buildClaimSummary(claims);
    }

    public ClaimSummaryReport buildClaimSummary(List<Claim> claims) {
        Collections.sort(claims, new Comparator<Claim>() {
            public int compare(Claim a, Claim b) {
                return a.getClaimNumber().compareTo(b.getClaimNumber());
            }
        });

        ClaimSummaryReport report = new ClaimSummaryReport();
        report.setGeneratedAt(DateUtils.formatDateTime(new Date()));
        report.setTotalClaims(claims.size());

        BigDecimal totalClaimed = BigDecimal.ZERO;
        BigDecimal totalApproved = BigDecimal.ZERO;
        for (Claim claim : claims) {
            totalClaimed = totalClaimed.add(claim.getClaimAmount());
            if (claim.getApprovedAmount() != null) {
                totalApproved = totalApproved.add(claim.getApprovedAmount());
            }
            report.getClaims().add(toLine(claim));
        }
        report.setTotalClaimed(MoneyUtils.round(totalClaimed));
        report.setTotalApproved(MoneyUtils.round(totalApproved));

        ClaimStatus[] statuses = ClaimStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            long count = 0;
            BigDecimal amount = BigDecimal.ZERO;
            for (Claim claim : claims) {
                if (claim.getStatus() == statuses[i]) {
                    count++;
                    amount = amount.add(claim.getClaimAmount());
                }
            }
            report.getStatusTotals().add(new StatusTotal(statuses[i].name(), new Long(count), MoneyUtils.round(amount)));
        }
        return report;
    }

    public String toXml(ClaimSummaryReport report) {
        try {
            JAXBContext context = JAXBContext.newInstance(ClaimSummaryReport.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(report, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to generate XML report", e);
        }
    }

    public String toCsv(ClaimSummaryReport report) {
        StringBuffer csv = new StringBuffer();
        csv.append("claimNumber,policyNumber,policyType,customerName,status,claimAmount,approvedAmount,fraudScore,incidentDate\n");
        for (ClaimReportLine line : report.getClaims()) {
            csv.append(line.getClaimNumber()).append(',');
            csv.append(line.getPolicyNumber()).append(',');
            csv.append(line.getPolicyType()).append(',');
            csv.append('"').append(line.getCustomerName()).append('"').append(',');
            csv.append(line.getStatus()).append(',');
            csv.append(line.getClaimAmount()).append(',');
            csv.append(line.getApprovedAmount() != null ? line.getApprovedAmount().toPlainString() : "").append(',');
            csv.append(line.getFraudScore() != null ? line.getFraudScore().toString() : "").append(',');
            csv.append(DateUtils.formatDate(line.getIncidentDate())).append('\n');
        }
        return csv.toString();
    }

    /**
     * Writes the XML report to a temporary file for pickup by the nightly finance batch.
     *
     * @return absolute path of the written file
     */
    public String exportXmlToFile() throws IOException {
        TempFileHandle handle = new TempFileHandle("claim-summary-", ".xml");
        FileWriter writer = new FileWriter(handle.getFile());
        writer.write(toXml(buildClaimSummary()));
        writer.close();
        return handle.getPath();
    }

    private ClaimReportLine toLine(Claim claim) {
        ClaimReportLine line = new ClaimReportLine();
        line.setClaimNumber(claim.getClaimNumber());
        line.setPolicyNumber(claim.getPolicy().getPolicyNumber());
        line.setPolicyType(claim.getPolicy().getPolicyType().name());
        line.setCustomerName(claim.getPolicy().getCustomer().getFullName());
        line.setStatus(claim.getStatus().name());
        line.setClaimAmount(claim.getClaimAmount());
        line.setApprovedAmount(claim.getApprovedAmount());
        line.setFraudScore(claim.getFraudScore());
        line.setIncidentDate(claim.getIncidentDate());
        return line;
    }
}
