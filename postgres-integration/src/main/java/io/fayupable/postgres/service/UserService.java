package io.fayupable.postgres.service;

import io.fayupable.postgres.dto.request.CreateUserRequest;
import io.fayupable.postgres.dto.request.UpdateUserRequest;
import io.fayupable.postgres.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    UserResponse getUserByEmail(String email);

    Page<UserResponse> getAllUsers(Pageable pageable);

    Page<UserResponse> getUsersByStatus(String status, Pageable pageable);

    Page<UserResponse> searchUsers(String search, Pageable pageable);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}