package com.insurance.portal.repository;

import com.insurance.portal.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByUserUsername(String username);

    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.user.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.user.email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(CONCAT(COALESCE(c.user.firstName, ''), ' ', COALESCE(c.user.lastName, ''))) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Customer> search(@Param("query") String query, Pageable pageable);
}
