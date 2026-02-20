package com.agnostic.producerspringboot;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

record PublishOrderRequest(
    @NotBlank(message = "id must not be blank")
    String id,
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than zero")
    BigDecimal amount
) {}
