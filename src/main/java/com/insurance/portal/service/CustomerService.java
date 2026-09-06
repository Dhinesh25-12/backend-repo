package com.insurance.portal.service;

import com.insurance.portal.dto.response.ClaimResponse;
import com.insurance.portal.dto.response.CustomerResponse;
import com.insurance.portal.dto.response.PolicyResponse;
import com.insurance.portal.entity.AppUser;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final ClaimService claimService;

    public Page<CustomerResponse> listCustomers(String query, Pageable pageable) {
        Page<Customer> page = StringUtils.hasText(query)
                ? customerRepository.search(query.trim(), pageable)
                : customerRepository.findAll(pageable);
        Map<Long, Long> policyCounts = policyCountsFor(page.getContent());
        return page.map(customer -> toResponse(customer, policyCounts.getOrDefault(customer.getId(), 0L)));
    }

    public CustomerResponse getCustomer(Long id) {
        Customer customer = findById(id);
        return toResponse(customer, policyRepository.countByCustomerId(customer.getId()));
    }

    public Page<PolicyResponse> listPolicies(Long customerId, Pageable pageable) {
        verifyCustomerExists(customerId);
        return policyService.listByCustomerId(customerId, pageable);
    }

    public Page<ClaimResponse> listClaims(Long customerId, Pageable pageable) {
        verifyCustomerExists(customerId);
        return claimService.listByCustomerId(customerId, pageable);
    }

    private void verifyCustomerExists(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found: " + id);
        }
    }

    private Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private Map<Long, Long> policyCountsFor(List<Customer> customers) {
        if (customers.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = customers.stream().map(Customer::getId).toList();
        return policyRepository.countGroupedByCustomerIds(ids).stream()
                .collect(Collectors.toMap(PolicyRepository.PolicyCustomerCount::getCustomerId,
                        PolicyRepository.PolicyCustomerCount::getTotal));
    }

    private CustomerResponse toResponse(Customer customer, long policyCount) {
        AppUser user = customer.getUser();
        return new CustomerResponse(
                customer.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null,
                user != null ? fullName(user) : null,
                user != null ? user.getPhone() : null,
                user != null && user.isActive(),
                customer.getDateOfBirth(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getPostalCode(),
                customer.getKycIdType(),
                customer.getKycIdNumber(),
                policyCount
        );
    }

    private String fullName(AppUser user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }
}
