package io.fayupable.jwtrefreshtoken.response;

/**
 * Simple Message Response DTO
 * <p>
 * Used for operations that only need to return a message.
 * <p>
 * Examples:
 * - User registered successfully
 * - Password changed successfully
 * - Email sent successfully
 */
public record MessageResponse(String message) {
}