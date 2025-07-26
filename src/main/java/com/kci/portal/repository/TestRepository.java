package com.kci.portal.repository;

import com.kci.portal.model.Test;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findAllByUploadedByEmail(String email);
    List<Test> findAllByUploadedBy_Email(String email); // preferred
}
