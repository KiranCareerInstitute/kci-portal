package com.kci.portal.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(name = "flash_notice")
    private String flashNotice;

    @Column(unique = true)
    private String email;

    private String password;

    private String name;

    private String role;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();

    private String mobile;

    // ✅ NEW: Chat restriction field
    @Column(name = "restricted_from_chat", nullable = false)
    private boolean restrictedFromChat = false;

    // ✅ Getter
    public boolean isRestrictedFromChat() {
        return restrictedFromChat;
    }

    // ✅ Setter
    public void setRestrictedFromChat(boolean restrictedFromChat) {
        this.restrictedFromChat = restrictedFromChat;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {  // ✅ Required method
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    // ✅ Existing Getters and Setters
    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    public String getFlashNotice() {
        return flashNotice;
    }

    public void setFlashNotice(String flashNotice) {
        this.flashNotice = flashNotice;
    }
    // Utility: Return first role
    public String getRole() {
        return roles.stream().findFirst().orElse(null);
    }

    // Utility: Check if user has specific role
    public boolean hasRole(String role) {
        return this.roles != null && this.roles.contains(role);
    }

}
