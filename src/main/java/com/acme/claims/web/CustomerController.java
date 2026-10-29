package com.acme.claims.web;

import com.acme.claims.service.CustomerService;
import com.acme.claims.web.dto.CustomerResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Api(tags = "Customers", description = "Policyholder lookup")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @ApiOperation("List all customers")
    @RequestMapping(method = RequestMethod.GET)
    public List<CustomerResponse> listCustomers() {
        return DtoMapper.toCustomerResponses(customerService.getAllCustomers());
    }

    @ApiOperation("Get a customer")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public CustomerResponse getCustomer(@PathVariable("id") Long id) {
        return DtoMapper.toResponse(customerService.getCustomer(id));
    }
}
