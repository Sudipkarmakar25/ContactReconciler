package com.reconciler.ContactReconciler.service;

import com.reconciler.ContactReconciler.dto.request.IdentifyRequest;
import com.reconciler.ContactReconciler.dto.response.IdentifyResponse;

public interface IdentifyService {
    IdentifyResponse identify(IdentifyRequest request);
}