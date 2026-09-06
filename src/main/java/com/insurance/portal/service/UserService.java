package com.insurance.portal.service;

import com.insurance.portal.dto.request.ChangePasswordRequest;
import com.insurance.portal.dto.request.UpdateProfileRequest;
import com.insurance.portal.dto.request.UpdateUserRoleRequest;
import com.insurance.portal.dto.request.UpdateUserRolesRequest;
import com.insurance.portal.dto.response.UserResponse;
import com.insurance.portal.entity.AppUser;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.entity.Role;
import com.insurance.portal.entity.RoleName;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.AppUserRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(String username) {
        AppUser user = findByUsername(username);
        return toProfileResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        AppUser user = findByUsername(username);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        appUserRepository.save(user);

        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        if (customer != null) {
            customer.setAddress(request.address());
            customer.setCity(request.city());
            customer.setState(request.state());
            customer.setPostalCode(request.postalCode());
            customer.setDateOfBirth(request.dateOfBirth());
            customer.setKycIdType(request.kycIdType());
            customer.setKycIdNumber(request.kycIdNumber());
            customerRepository.save(customer);
        }
        return toProfileResponse(user);
    }

    /**
     * Changes the authenticated user's password after verifying the current one.
     * Known limitation: JWTs are stateless and there is no token blocklist, so tokens
     * issued before the change remain valid until their natural expiry.
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = findByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + request.role()));
        HashSet<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        appUserRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean wasAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
        boolean willBeAdmin = request.roles().contains(RoleName.ADMIN);
        if (wasAdmin && !willBeAdmin && appUserRepository.countByRoles_Name(RoleName.ADMIN) <= 1) {
            throw new BadRequestException("Cannot remove the last remaining ADMIN user");
        }

        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        appUserRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse setUserActive(Long userId, boolean active) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(active);
        appUserRepository.save(user);
        return toResponse(user);
    }

    private AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private UserResponse toResponse(AppUser user) {
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList());
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhone(), user.isActive(), roles,
                null, null, null, null, null, null, null);
    }

    /** Profile responses additionally expose the customer/KYC details when the user is a customer. */
    private UserResponse toProfileResponse(AppUser user) {
        UserResponse base = toResponse(user);
        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        if (customer == null) {
            return base;
        }
        return new UserResponse(base.id(), base.username(), base.email(), base.firstName(),
                base.lastName(), base.phone(), base.active(), base.roles(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getPostalCode(),
                customer.getDateOfBirth(),
                customer.getKycIdType(),
                customer.getKycIdNumber());
    }
}
