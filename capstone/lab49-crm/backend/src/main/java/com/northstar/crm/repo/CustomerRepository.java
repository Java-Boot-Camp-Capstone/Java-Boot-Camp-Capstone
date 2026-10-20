
package com.northstar.crm.repo;

import com.northstar.crm.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByCustomerIdAsc();
}
