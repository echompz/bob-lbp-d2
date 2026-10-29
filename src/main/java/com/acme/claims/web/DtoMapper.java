package com.acme.claims.web;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimDocument;
import com.acme.claims.model.ClaimNote;
import com.acme.claims.model.Customer;
import com.acme.claims.model.Policy;
import com.acme.claims.util.DateUtils;
import com.acme.claims.web.dto.ClaimResponse;
import com.acme.claims.web.dto.CustomerResponse;
import com.acme.claims.web.dto.DocumentResponse;
import com.acme.claims.web.dto.NoteResponse;
import com.acme.claims.web.dto.PolicyResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hand-written entity to DTO mapping.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static ClaimResponse toResponse(Claim claim) {
        ClaimResponse r = new ClaimResponse();
        r.setId(claim.getId());
        r.setClaimNumber(claim.getClaimNumber());
        if (claim.getPolicy() != null) {
            r.setPolicyNumber(claim.getPolicy().getPolicyNumber());
            r.setPolicyType(claim.getPolicy().getPolicyType().name());
            if (claim.getPolicy().getCustomer() != null) {
                r.setCustomerName(claim.getPolicy().getCustomer().getFullName());
            }
        }
        r.setDescription(claim.getDescription());
        r.setClaimAmount(claim.getClaimAmount());
        r.setApprovedAmount(claim.getApprovedAmount());
        r.setStatus(claim.getStatus().name());
        r.setFraudScore(claim.getFraudScore());
        r.setAssignedAdjuster(claim.getAssignedAdjuster());
        r.setIncidentDate(DateUtils.formatDate(claim.getIncidentDate()));
        r.setFiledDate(DateUtils.formatDateTime(claim.getFiledDate()));
        r.setLastUpdated(DateUtils.formatDateTime(claim.getLastUpdated()));
        List<NoteResponse> notes = new ArrayList<NoteResponse>();
        if (claim.getNotes() != null) {
            for (Iterator<ClaimNote> it = claim.getNotes().iterator(); it.hasNext(); ) {
                ClaimNote note = it.next();
                notes.add(new NoteResponse(note.getAuthor(), note.getNoteText(), DateUtils.formatDateTime(note.getCreatedAt())));
            }
        }
        r.setNotes(notes);
        return r;
    }

    public static List<ClaimResponse> toClaimResponses(List<Claim> claims) {
        List<ClaimResponse> result = new ArrayList<ClaimResponse>();
        for (int i = 0; i < claims.size(); i++) {
            result.add(toResponse(claims.get(i)));
        }
        return result;
    }

    public static PolicyResponse toResponse(Policy policy) {
        PolicyResponse r = new PolicyResponse();
        r.setId(policy.getId());
        r.setPolicyNumber(policy.getPolicyNumber());
        r.setPolicyType(policy.getPolicyType().name());
        r.setCustomerId(policy.getCustomer().getId());
        r.setCustomerName(policy.getCustomer().getFullName());
        r.setCoverageLimit(policy.getCoverageLimit());
        r.setDeductible(policy.getDeductible());
        r.setAnnualPremium(policy.getAnnualPremium());
        r.setStartDate(DateUtils.formatDate(policy.getStartDate()));
        r.setEndDate(DateUtils.formatDate(policy.getEndDate()));
        r.setActive(policy.isActive());
        return r;
    }

    public static List<PolicyResponse> toPolicyResponses(List<Policy> policies) {
        List<PolicyResponse> result = new ArrayList<PolicyResponse>();
        for (Policy policy : policies) {
            result.add(toResponse(policy));
        }
        return result;
    }

    public static CustomerResponse toResponse(Customer customer) {
        CustomerResponse r = new CustomerResponse();
        r.setId(customer.getId());
        r.setFullName(customer.getFullName());
        r.setEmail(customer.getEmail());
        r.setPhone(customer.getPhone());
        r.setAddress(customer.getAddress());
        r.setDateOfBirth(DateUtils.formatDate(customer.getDateOfBirth()));
        if (customer.getDateOfBirth() != null) {
            r.setAge(DateUtils.calculateAge(customer.getDateOfBirth()));
        }
        return r;
    }

    public static List<CustomerResponse> toCustomerResponses(List<Customer> customers) {
        List<CustomerResponse> result = new ArrayList<CustomerResponse>();
        for (Customer customer : customers) {
            result.add(toResponse(customer));
        }
        return result;
    }

    public static DocumentResponse toResponse(ClaimDocument document) {
        DocumentResponse r = new DocumentResponse();
        r.setId(document.getId());
        r.setFileName(document.getFileName());
        r.setContentType(document.getContentType());
        r.setSizeInBytes(document.getSizeInBytes());
        r.setChecksum(document.getChecksum());
        r.setUploadedAt(DateUtils.formatDateTime(document.getUploadedAt()));
        r.setUploadedBy(document.getUploadedBy());
        return r;
    }

    public static List<DocumentResponse> toDocumentResponses(List<ClaimDocument> documents) {
        List<DocumentResponse> result = new ArrayList<DocumentResponse>();
        for (ClaimDocument document : documents) {
            result.add(toResponse(document));
        }
        return result;
    }
}
