package com.kci.portal.service;

import com.kci.portal.model.StudentResult;
import com.kci.portal.model.User;
import com.kci.portal.repository.StudentResultRepository;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StudentResultService {

    private final StudentResultRepository studentResultRepository;

    public StudentResultService(StudentResultRepository studentResultRepository) {
        this.studentResultRepository = studentResultRepository;
    }

    public List<StudentResult> getResultsByUser(User user) {
        return studentResultRepository.findByUser(user);
    }

    // Optional: for seeding data if needed
    public void saveResult(StudentResult result) {
        studentResultRepository.save(result);
    }
}
