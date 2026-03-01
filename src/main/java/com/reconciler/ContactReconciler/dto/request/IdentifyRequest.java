package com.reconciler.ContactReconciler.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentifyRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

}