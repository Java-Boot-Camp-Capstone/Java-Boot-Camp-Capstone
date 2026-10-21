
package com.northstar.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "status", nullable = false)
    private String status;

    public Customer() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStatus() {
        return status;
    }
}
