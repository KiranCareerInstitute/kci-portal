package com.kci.portal.repository;

import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Find user by email
    Optional<User> findByEmail(String email);

    // ✅ Search user by name or mobile
    List<User> findByFullNameContainingIgnoreCaseOrMobileContainingIgnoreCase(String name, String mobile);

    // ✅ Fetch all users by role name
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") String role);

    // ✅ Count users by role (needed for tutor-dashboard)
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") String role);
}
