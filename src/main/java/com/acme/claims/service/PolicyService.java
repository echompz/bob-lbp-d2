package com.acme.claims.service;

import com.acme.claims.exception.ResourceNotFoundException;
import com.acme.claims.model.Policy;
import com.acme.claims.repository.CustomerRepository;
import com.acme.claims.repository.PolicyRepository;
import com.acme.claims.util.DateUtils;
import com.acme.claims.util.LegacyCache;
import com.acme.claims.web.dto.PremiumQuoteRequest;
import com.acme.claims.web.dto.PremiumQuoteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PolicyService {

    private static final String QUOTE_CACHE_PREFIX = "quote:";

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PremiumCalculator premiumCalculator;

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll(new Sort("policyNumber"));
    }

    public Policy getByPolicyNumber(String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber);
        if (policy == null) {
            throw new ResourceNotFoundException("Policy", policyNumber);
        }
        return policy;
    }

    public List<Policy> getByCustomer(Long customerId) {
        if (!customerRepository.exists(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        return policyRepository.findByCustomerId(customerId);
    }

    public PremiumQuoteResponse quote(PremiumQuoteRequest request) {
        Date dob;
        try {
            dob = DateUtils.parseDate(request.getDateOfBirth());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date of birth '" + request.getDateOfBirth()
                    + "', expected " + DateUtils.DATE_PATTERN);
        }
        int age = DateUtils.calculateAge(dob);

        String cacheKey = QUOTE_CACHE_PREFIX + request.getPolicyType() + ":" + age + ":"
                + request.getCoverageAmount().toPlainString() + ":" + request.getPriorClaims();
        LegacyCache cache = LegacyCache.getInstance();
        BigDecimal annual = (BigDecimal) cache.get(cacheKey);
        if (annual == null) {
            annual = premiumCalculator.calculateAnnualPremium(request.getPolicyType(), age,
                    request.getCoverageAmount(), request.getPriorClaims());
            cache.put(cacheKey, annual);
        }

        PremiumQuoteResponse response = new PremiumQuoteResponse();
        response.setPolicyType(request.getPolicyType().name());
        response.setAge(age);
        response.setCoverageAmount(request.getCoverageAmount());
        response.setPriorClaims(request.getPriorClaims());
        response.setAnnualPremium(annual);
        response.setMonthlyPremium(premiumCalculator.monthlyInstallment(annual));
        response.setQuotedAt(DateUtils.formatDateTime(new Date()));
        return response;
    }
}
