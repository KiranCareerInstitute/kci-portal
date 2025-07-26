package com.kci.portal.repository;

import com.kci.portal.model.StudentQuery;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentQueryRepository extends JpaRepository<StudentQuery, Long> {
    List<StudentQuery> findByUser(User user);
}
