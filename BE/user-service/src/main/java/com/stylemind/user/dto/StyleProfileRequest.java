package com.stylemind.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StyleProfileRequest {
    @Size(max = 20, message = "Giới tính tối đa 20 ký tự")
    private String gender;

    @Min(value = 1, message = "Tuổi phải lớn hơn 0")
    @Max(value = 150, message = "Tuổi không quá 150")
    private Integer age;

    @DecimalMin(value = "50.0", message = "Chiều cao tối thiểu 50 cm")
    @DecimalMax(value = "300.0", message = "Chiều cao tối đa 300 cm")
    private java.math.BigDecimal heightCm;

    @DecimalMin(value = "20.0", message = "Cân nặng tối thiểu 20 kg")
    @DecimalMax(value = "500.0", message = "Cân nặng tối đa 500 kg")
    private java.math.BigDecimal weightKg;

    @Size(max = 50, message = "Dáng người tối đa 50 ký tự")
    private String bodyMorphology;

    @Size(max = 30, message = "Form dáng tối đa 30 ký tự")
    private String preferredFit;

    private String stylePersonas; // JSON string
}