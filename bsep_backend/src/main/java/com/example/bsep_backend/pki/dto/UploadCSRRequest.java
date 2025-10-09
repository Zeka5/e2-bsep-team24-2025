package com.example.bsep_backend.pki.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadCSRRequest {
    @NotNull(message = "csrFile cannot be null")
    private MultipartFile csrFile;

    @NotNull(message = "Validity days is required")
    @Min(value = 1, message = "Validity days must be positive")
    private Integer validityDays;
}
