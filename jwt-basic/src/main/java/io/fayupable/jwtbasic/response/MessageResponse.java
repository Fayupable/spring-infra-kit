package io.fayupable.jwtbasic.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple Message Response DTO
 * <p>
 * Used for simple success/error messages.
 * Example: "User registered successfully"
 */
@Data
@AllArgsConstructor
public class MessageResponse {

    /**
     * Response message
     */
    private String message;
}