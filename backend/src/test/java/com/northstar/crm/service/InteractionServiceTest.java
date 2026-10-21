
package com.northstar.crm.service;

import static com.northstar.crm.domain.InteractionType.NOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.Interaction;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.InteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class InteractionServiceTest {

    private InteractionService service;
    private InteractionRepository interactionRepository;
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        interactionRepository = mock(InteractionRepository.class);
        customerRepository = mock(CustomerRepository.class);

        Customer amina = mock(Customer.class);
        when(amina.getCustomerId()).thenReturn(1L);
        when(amina.getPublicId()).thenReturn("CUS-1001");

        when(customerRepository.findByPublicId("CUS-1001"))
                .thenReturn(Optional.of(amina));

        when(customerRepository.findByPublicId("CUS-9999"))
                .thenReturn(Optional.empty());

        when(interactionRepository.save(any(Interaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new InteractionService(
                interactionRepository,
                customerRepository,
                new InteractionEventPublisher());
    }

    @Test
    void create_forAmina_returnsCreatedShape() {
        CreateInteractionRequest request =
                new CreateInteractionRequest(
                        "CUS-1001",
                        NOTE,
                        "Follow-up on billing question",
                        "lab-request-001");

        InteractionResponse response =
                service.create(request, "lab-request-001");

        assertNotNull(response.id());
        assertEquals("CUS-1001", response.customerId());
        assertEquals("lab-request-001", response.correlationId());
    }

    @Test
    void create_unknownCustomer_fails() {
        CreateInteractionRequest request =
                new CreateInteractionRequest(
                        "CUS-9999",
                        NOTE,
                        "Should fail",
                        "lab-request-001");

        assertThrows(
                UnknownCustomerException.class,
                () -> service.create(request, null));
    }
}

