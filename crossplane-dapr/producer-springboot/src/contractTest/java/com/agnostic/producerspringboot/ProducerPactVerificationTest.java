package com.agnostic.producerspringboot;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("order-event-producer-springboot")
@PactFolder("../consumer-springboot/pacts")
@ExtendWith(PactVerificationInvocationContextProvider.class)
class ProducerPactVerificationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @PactVerifyProvider("an order created v1 event from producer-springboot")
    String orderCreatedMessage() throws Exception {
        return objectMapper.writeValueAsString(
            Map.of(
                "id", "ORD-SB-300",
                "amount", 55.1,
                "eventVersion", "v1"));
    }
}
