package com.insurance.portal.service;

import com.insurance.portal.dto.response.CustomerResponse;
import com.insurance.portal.entity.AppUser;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private ClaimService claimService;

    private CustomerService customerService;

    private Customer customer;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, policyRepository, policyService, claimService);

        AppUser user = AppUser.builder().id(7L).username("jdoe").email("jdoe@example.com")
                .firstName("John").lastName("Doe").phone("555").active(true).build();
        customer = Customer.builder().id(3L).user(user).dateOfBirth(LocalDate.of(1990, 1, 2))
                .address("1 Main St").city("Springfield").state("IL").postalCode("62701")
                .kycIdType("PASSPORT").kycIdNumber("X123").build();
    }

    private PolicyRepository.PolicyCustomerCount policyCount(Long customerId, long total) {
        return new PolicyRepository.PolicyCustomerCount() {
            @Override
            public Long getCustomerId() {
                return customerId;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void listCustomersWithoutQueryUsesFindAll() {
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer)));
        when(policyRepository.countGroupedByCustomerIds(List.of(3L))).thenReturn(List.of(policyCount(3L, 2L)));

        Page<CustomerResponse> page = customerService.listCustomers(null, pageable);

        assertThat(page.getContent()).hasSize(1);
        CustomerResponse response = page.getContent().get(0);
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("jdoe");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.policyCount()).isEqualTo(2L);
        verify(customerRepository, never()).search(anyString(), any());
    }

    @Test
    void listCustomersWithQueryUsesSearch() {
        when(customerRepository.search("doe", pageable)).thenReturn(new PageImpl<>(List.of(customer)));
        when(policyRepository.countGroupedByCustomerIds(List.of(3L))).thenReturn(List.of());

        Page<CustomerResponse> page = customerService.listCustomers("  doe  ", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).policyCount()).isZero();
        verify(customerRepository, never()).findAll(pageable);
    }

    @Test
    void getCustomerReturnsKycDetails() {
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(policyRepository.countByCustomerId(3L)).thenReturn(1L);

        CustomerResponse response = customerService.getCustomer(3L);

        assertThat(response.kycIdType()).isEqualTo("PASSPORT");
        assertThat(response.kycIdNumber()).isEqualTo("X123");
        assertThat(response.city()).isEqualTo("Springfield");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
    }

    @Test
    void getCustomerThrowsWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listPoliciesDelegatesToPolicyService() {
        when(customerRepository.existsById(3L)).thenReturn(true);
        when(policyService.listByCustomerId(3L, pageable)).thenReturn(Page.empty());

        customerService.listPolicies(3L, pageable);

        verify(policyService).listByCustomerId(3L, pageable);
    }

    @Test
    void listClaimsDelegatesToClaimService() {
        when(customerRepository.existsById(3L)).thenReturn(true);
        when(claimService.listByCustomerId(3L, pageable)).thenReturn(Page.empty());

        customerService.listClaims(3L, pageable);

        verify(claimService).listByCustomerId(3L, pageable);
    }

    @Test
    void listPoliciesThrowsForUnknownCustomer() {
        when(customerRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.listPolicies(42L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(policyService, never()).listByCustomerId(anyLong(), any());
    }

    @Test
    void listClaimsThrowsForUnknownCustomer() {
        when(customerRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.listClaims(42L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimService, never()).listByCustomerId(anyLong(), any());
    }
}
