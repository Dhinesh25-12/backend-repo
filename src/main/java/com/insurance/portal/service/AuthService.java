package com.insurance.portal.service;

import com.insurance.portal.dto.request.LoginRequest;
import com.insurance.portal.dto.request.RegisterRequest;
import com.insurance.portal.dto.response.AuthResponse;
import com.insurance.portal.entity.*;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.repository.AppUserRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.RoleRepository;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken");
        }
        if (appUserRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not seeded"));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        AppUser user = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .active(true)
                .roles(roles)
                .build();

        user = appUserRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .build();
        customerRepository.save(customer);

        SecurityUser securityUser = new SecurityUser(user);
        String token = jwtService.generateToken(securityUser);
        List<String> roleNames = roles.stream().map(r -> r.getName().name()).collect(Collectors.toList());
        return buildAuthResponse(token, user, roleNames);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        SecurityUser securityUser = new SecurityUser(user);
        String token = jwtService.generateToken(securityUser);
        List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList());
        return buildAuthResponse(token, user, roleNames);
    }

    private AuthResponse buildAuthResponse(String token, AppUser user, List<String> roleNames) {
        String fullName = buildFullName(user.getFirstName(), user.getLastName());
        AuthResponse.UserSummary userSummary = new AuthResponse.UserSummary(
                user.getId(), user.getUsername(), user.getEmail(), fullName, roleNames, user.isActive());
        return new AuthResponse(token, jwtService.getExpirationSeconds(), userSummary);
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }
}
