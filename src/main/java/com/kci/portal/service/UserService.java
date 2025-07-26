package com.kci.portal.service;

import com.kci.portal.model.User;

public interface UserService {
    User findByEmail(String email);
}
