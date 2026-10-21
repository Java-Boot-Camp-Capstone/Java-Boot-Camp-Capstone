
package com.northstar.crm.repo;

import com.northstar.crm.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByCustomerIdAsc();

    Optional<Customer> findByPublicId(String publicId);
}

