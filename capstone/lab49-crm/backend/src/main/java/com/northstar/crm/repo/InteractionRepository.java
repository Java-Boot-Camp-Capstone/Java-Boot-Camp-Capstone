
package com.northstar.crm.repo;

import com.northstar.crm.domain.Interaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InteractionRepository {

    private final Map<UUID, Interaction> store =
            new ConcurrentHashMap<>();

    public Interaction save(Interaction interaction) {
        if (interaction.getId() == null) {
            interaction.setId(UUID.randomUUID());
        }

        store.put(interaction.getId(), interaction);
        return interaction;
    }

    public Optional<Interaction> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Interaction> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean customerExists(String customerId) {
        return "CUS-1001".equals(customerId)
                || "CUS-1002".equals(customerId);
    }
}
