package com.insurance.portal.service;

import com.insurance.portal.dto.request.UpdateUserRolesRequest;
import com.insurance.portal.entity.AppUser;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    private UserService userService;

    private AppUser adminUser;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, customerRepository, roleRepository);

        adminRole = Role.builder().id(1L).name(RoleName.ADMIN).build();
        customerRole = Role.builder().id(2L).name(RoleName.CUSTOMER).build();

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        adminUser = AppUser.builder().id(1L).username("admin1").email("admin1@example.com")
                .firstName("Ad").lastName("Min").active(true).roles(roles).build();
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
