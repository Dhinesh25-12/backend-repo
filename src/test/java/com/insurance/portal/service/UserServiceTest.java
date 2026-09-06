package com.insurance.portal.service;

import com.insurance.portal.dto.request.ChangePasswordRequest;
import com.insurance.portal.dto.request.UpdateProfileRequest;
import com.insurance.portal.dto.request.UpdateUserRolesRequest;
import com.insurance.portal.entity.AppUser;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.entity.Role;
import com.insurance.portal.entity.RoleName;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.repository.AppUserRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private AppUser adminUser;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(appUserRepository, customerRepository, roleRepository, passwordEncoder);

        adminRole = Role.builder().id(1L).name(RoleName.ADMIN).build();
        customerRole = Role.builder().id(2L).name(RoleName.CUSTOMER).build();

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        adminUser = AppUser.builder().id(1L).username("admin1").email("admin1@example.com")
                .firstName("Ad").lastName("Min").active(true).roles(roles).build();
    }

    @Test
    void updateProfilePersistsKycFieldsForCustomers() {
        AppUser user = customerUser();
        Customer customer = Customer.builder().id(9L).user(user).build();
        when(appUserRepository.findByUsername("cust1")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(2L)).thenReturn(Optional.of(customer));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate dob = LocalDate.of(1990, 5, 20);
        UpdateProfileRequest request = new UpdateProfileRequest("Cus", "Tomer", "555", "1 Main St",
                "Springfield", "IL", "62701", dob, "PASSPORT", "X1234567");

        var response = userService.updateProfile("cust1", request);

        assertThat(customer.getDateOfBirth()).isEqualTo(dob);
        assertThat(customer.getKycIdType()).isEqualTo("PASSPORT");
        assertThat(customer.getKycIdNumber()).isEqualTo("X1234567");
        assertThat(response.dateOfBirth()).isEqualTo(dob);
        assertThat(response.kycIdType()).isEqualTo("PASSPORT");
        assertThat(response.kycIdNumber()).isEqualTo("X1234567");
        assertThat(response.city()).isEqualTo("Springfield");
    }

    @Test
    void updateProfileIgnoresKycFieldsForNonCustomers() {
        when(appUserRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Ad", "Min", "555", "1 Main St",
                "Springfield", "IL", "62701", LocalDate.of(1990, 5, 20), "PASSPORT", "X1234567");

        var response = userService.updateProfile("admin1", request);

        verify(customerRepository, never()).save(any());
        assertThat(response.dateOfBirth()).isNull();
        assertThat(response.kycIdType()).isNull();
        assertThat(response.kycIdNumber()).isNull();
        assertThat(response.firstName()).isEqualTo("Ad");
    }

    @Test
    void changePasswordUpdatesHashWhenCurrentPasswordMatches() {
        AppUser user = customerUser();
        user.setPasswordHash(passwordEncoder.encode("OldPass123"));
        String originalHash = user.getPasswordHash();
        when(appUserRepository.findByUsername("cust1")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword("cust1", new ChangePasswordRequest("OldPass123", "NewPass123"));

        assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(passwordEncoder.matches("NewPass123", user.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        AppUser user = customerUser();
        user.setPasswordHash(passwordEncoder.encode("OldPass123"));
        when(appUserRepository.findByUsername("cust1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword("cust1",
                new ChangePasswordRequest("WrongPass123", "NewPass123")))
                .isInstanceOf(BadRequestException.class);

        verify(appUserRepository, never()).save(any());
    }

    private AppUser customerUser() {
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        return AppUser.builder().id(2L).username("cust1").email("cust1@example.com")
                .firstName("Cus").lastName("Tomer").active(true).roles(roles).build();
    }

    @Test
    void updateUserRolesRejectsRemovingLastAdmin() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(appUserRepository.countByRoles_Name(RoleName.ADMIN)).thenReturn(1L);

        UpdateUserRolesRequest request = new UpdateUserRolesRequest(List.of(RoleName.CUSTOMER));

        assertThatThrownBy(() -> userService.updateUserRoles(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateUserRolesAllowsRemovingAdminWhenOthersRemain() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(appUserRepository.countByRoles_Name(RoleName.ADMIN)).thenReturn(2L);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRolesRequest request = new UpdateUserRolesRequest(List.of(RoleName.CUSTOMER));

        var response = userService.updateUserRoles(1L, request);

        assertThat(response.roles()).containsExactly("CUSTOMER");
    }
}
