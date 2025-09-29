package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.CSRStatus;
import com.example.bsep_backend.pki.dto.CSRResponse;
import com.example.bsep_backend.pki.dto.CreateCSRRequest;
import com.example.bsep_backend.pki.dto.ReviewCSRRequest;

import java.util.List;

public interface CSRService {

    CSRResponse createCSR(CreateCSRRequest request, User requester);

    List<CSRResponse> getMyCSRs(User requester);

    List<CSRResponse> getAllCSRs();

    List<CSRResponse> getCSRsByStatus(CSRStatus status);

    CSRResponse reviewCSR(Long csrId, ReviewCSRRequest request, User reviewer);

    CSRResponse getCSRById(Long csrId);
}