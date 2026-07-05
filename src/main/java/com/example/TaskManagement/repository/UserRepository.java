package com.example.TaskManagement.repository;

import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:active IS NULL OR u.active = :active)")
    List<User> findUsersByFilters(
            @Param("role") Role role,
            @Param("active") Boolean active);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:input) OR LOWER(u.email) = LOWER(:input)")
    Optional<User> findByUsernameOrEmailIgnoreCase(@Param("input") String input);
}
