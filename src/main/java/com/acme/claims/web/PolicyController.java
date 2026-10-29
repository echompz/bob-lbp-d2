package com.acme.claims.web;

import com.acme.claims.service.PolicyService;
import com.acme.claims.web.dto.PolicyResponse;
import com.acme.claims.web.dto.PremiumQuoteRequest;
import com.acme.claims.web.dto.PremiumQuoteResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
@Api(tags = "Policies", description = "Policy lookup and premium quotes")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    @ApiOperation("List all policies")
    @RequestMapping(method = RequestMethod.GET)
    public List<PolicyResponse> listPolicies() {
        return DtoMapper.toPolicyResponses(policyService.getAllPolicies());
    }

    @ApiOperation("Get a policy by its policy number")
    @RequestMapping(value = "/{policyNumber}", method = RequestMethod.GET)
    public PolicyResponse getPolicy(@PathVariable("policyNumber") String policyNumber) {
        return DtoMapper.toResponse(policyService.getByPolicyNumber(policyNumber));
    }

    @ApiOperation("List the policies held by a customer")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.GET)
    public List<PolicyResponse> getCustomerPolicies(@PathVariable("customerId") Long customerId) {
        return DtoMapper.toPolicyResponses(policyService.getByCustomer(customerId));
    }

    @ApiOperation("Calculate a premium quote")
    @RequestMapping(value = "/quote", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public PremiumQuoteResponse quote(@Valid @RequestBody PremiumQuoteRequest request) {
        return policyService.quote(request);
    }
}
