
package com.northstar.crm.repo;

import com.northstar.crm.domain.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InteractionRepository
        extends JpaRepository<Interaction, UUID> {

    @Query("""
            SELECT COUNT(c) > 0
            FROM Customer c
            WHERE c.publicId = :publicId
            """)
    boolean customerExists(
            @Param("publicId") String publicId);
}
