package com.kci.portal.service;

import com.kci.portal.model.TestSubmission;
import com.kci.portal.repository.TestSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestSubmissionService {

    @Autowired
    private TestSubmissionRepository submissionRepository;

    public List<TestSubmission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    public Optional<TestSubmission> getSubmissionById(Long id) {
        return submissionRepository.findById(id);
    }

    public TestSubmission updateSubmission(TestSubmission submission) {
        return submissionRepository.save(submission);
    }
}
