package com.reconciler.ContactReconciler.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponse {

    private Integer primaryContactId;

    private List<String> emails;

    private List<String> phoneNumbers;

    private List<Integer> secondaryContactIds;

}