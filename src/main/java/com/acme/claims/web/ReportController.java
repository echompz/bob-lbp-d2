package com.acme.claims.web;

import com.acme.claims.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@Api(tags = "Reports", description = "Portfolio reports for finance and audit (MANAGER / AUDITOR)")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @ApiOperation("Claims summary as XML (finance system feed)")
    @RequestMapping(value = "/claims-summary/xml", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public String claimsSummaryXml() {
        return reportService.toXml(reportService.buildClaimSummary());
    }

    @ApiOperation("Claims summary as CSV")
    @RequestMapping(value = "/claims-summary/csv", method = RequestMethod.GET)
    public ResponseEntity<String> claimsSummaryCsv() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claims-summary.csv\"");
        return new ResponseEntity<String>(reportService.toCsv(reportService.buildClaimSummary()), headers, HttpStatus.OK);
    }

    @ApiOperation("Write the XML summary to the server's temp directory for batch pickup")
    @RequestMapping(value = "/claims-summary/export", method = RequestMethod.POST)
    public Map<String, String> exportClaimsSummary() throws IOException {
        Map<String, String> result = new HashMap<String, String>();
        result.put("path", reportService.exportXmlToFile());
        return result;
    }
}
