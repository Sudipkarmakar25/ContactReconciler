package com.reconciler.ContactReconciler.controller;

import com.reconciler.ContactReconciler.dto.request.IdentifyRequest;
import com.reconciler.ContactReconciler.dto.response.IdentifyResponse;
import com.reconciler.ContactReconciler.exception.InvalidRequestException;
import com.reconciler.ContactReconciler.service.IdentifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class IdentifyController {

    private final IdentifyService identifyService;

    @PostMapping("/identify")
    public ResponseEntity<IdentifyResponse> identify(
            @Valid @RequestBody IdentifyRequest request) {

        log.info("POST /identify called");

        if (request.getEmail() == null && request.getPhoneNumber() == null) {
            throw new InvalidRequestException(
                    "At least one of email or phoneNumber must be provided");
        }

        IdentifyResponse response = identifyService.identify(request);
        return ResponseEntity.ok(response);
    }
}