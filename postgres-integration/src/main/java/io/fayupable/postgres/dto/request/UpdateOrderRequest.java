package io.fayupable.postgres.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

    @Pattern(regexp = "^(PENDING|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Status must be PENDING, PROCESSING, SHIPPED, DELIVERED, or CANCELLED")
    private String status;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}