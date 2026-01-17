package pt.raidline.petstore.model;

import java.time.LocalDateTime;
import java.util.List;

// --- Domain Models ---
public class Model {
    public record Category(long id, String name) {
    }

    public record Tag(long id, String name) {
    }

    public record Photo(String id, String url, LocalDateTime uploadedAt) {
    }

    public record Pet(
            long id,
            String name,
            Category category,
            String status, // available, pending, sold
            List<Tag> tags,
            List<Photo> photos,
            LocalDateTime createdAt
    ) {
    }

    public record User(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            String role, // user, admin, moderator
            LocalDateTime createdAt,
            LocalDateTime lastLogin
    ) {
    }

    public record Error(String code, String message, List<ErrorDetail> details, LocalDateTime timestamp) {
    }

    public record ErrorDetail(String field, String message) {
    }

// --- DTOs (Request/Response Bodies) ---

    public record CreatePetRequest(String name, Long categoryId, String status, List<Long> tagIds) {
    }

    public record UpdatePetRequest(String name, Long categoryId, String status, List<Long> tagIds) {
    }

    public record CreateUserRequest(String username, String email, String password, String firstName, String lastName) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, int expiresIn, User user) {
    }
}