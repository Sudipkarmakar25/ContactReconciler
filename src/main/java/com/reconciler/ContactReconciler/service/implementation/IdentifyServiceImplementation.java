package com.reconciler.ContactReconciler.service.implementation;

import com.reconciler.ContactReconciler.dto.request.IdentifyRequest;
import com.reconciler.ContactReconciler.dto.response.ContactResponse;
import com.reconciler.ContactReconciler.dto.response.IdentifyResponse;
import com.reconciler.ContactReconciler.enums.LinkPrecedence;
import com.reconciler.ContactReconciler.model.Contact;
import com.reconciler.ContactReconciler.repository.ContactRepository;
import com.reconciler.ContactReconciler.service.IdentifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifyServiceImplementation implements IdentifyService {

    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public IdentifyResponse identify(IdentifyRequest request) {

        String email = request.getEmail();
        String phoneNumber = request.getPhoneNumber();

        log.info("Identify request received — email: {}, phone: {}", email, phoneNumber);

        // Step 1 — Find all contacts matching email OR phone
        List<Contact> matchedContacts = contactRepository
                .findByEmailOrPhoneNumber(email, phoneNumber);

        // ─────────────────────────────────────────
        // CASE 1 — No matches found, create new primary
        // ─────────────────────────────────────────
        if (matchedContacts.isEmpty()) {
            log.info("No existing contact found. Creating new primary contact.");
            Contact newContact = createPrimaryContact(email, phoneNumber);
            return buildResponse(newContact, List.of());
        }

        // Step 2 — Get all primary contacts from matched list
        List<Contact> primaries = matchedContacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());

        Contact truePrimary;

        // ─────────────────────────────────────────
        // CASE 4 — Two different primaries matched, merge them
        // ─────────────────────────────────────────
        if (primaries.size() > 1) {
            log.info("Two primary contacts found. Merging...");

            // Oldest one is the true primary
            truePrimary = primaries.get(0);
            Contact todemote = primaries.get(1);

            // Demote the newer primary to secondary
            todemote.setLinkPrecedence(LinkPrecedence.SECONDARY);
            todemote.setLinkedId(truePrimary.getId());
            contactRepository.save(todemote);

            // Re-link all secondaries of demoted primary to true primary
            List<Contact> orphanSecondaries = contactRepository
                    .findByLinkedId(todemote.getId());

            for (Contact orphan : orphanSecondaries) {
                orphan.setLinkedId(truePrimary.getId());
                contactRepository.save(orphan);
            }

        } else if (primaries.size() == 1) {
            truePrimary = primaries.get(0);
        } else {
            // All matched contacts are secondary, find their primary
            Contact anySecondary = matchedContacts.get(0);
            truePrimary = contactRepository
                    .findById(anySecondary.getLinkedId())
                    .orElseThrow(() -> new RuntimeException(
                            "Primary contact not found for linkedId: "
                                    + anySecondary.getLinkedId()));
        }

        // Step 3 — Fetch all contacts in the cluster
        List<Contact> allInCluster = contactRepository
                .findByIdOrLinkedId(truePrimary.getId());

        // ─────────────────────────────────────────
        // CASE 3 — New info found, create secondary
        // ─────────────────────────────────────────
        boolean isNewEmail = email != null &&
                allInCluster.stream()
                        .noneMatch(c -> email.equals(c.getEmail()));

        boolean isNewPhone = phoneNumber != null &&
                allInCluster.stream()
                        .noneMatch(c -> phoneNumber.equals(c.getPhoneNumber()));

        if (isNewEmail || isNewPhone) {
            log.info("New information found. Creating secondary contact.");
            Contact secondary = Contact.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .linkedId(truePrimary.getId())
                    .linkPrecedence(LinkPrecedence.SECONDARY)
                    .build();
            contactRepository.save(secondary);

            // Refresh the cluster after adding new secondary
            allInCluster = contactRepository
                    .findByIdOrLinkedId(truePrimary.getId());
        }

        // ─────────────────────────────────────────
        // CASE 2 — No new info, just return response
        // ─────────────────────────────────────────
        log.info("Returning consolidated contact for primaryId: {}", truePrimary.getId());
        return buildResponse(truePrimary, allInCluster);
    }

    // ─────────────────────────────────────────
    // Helper — Create a new primary contact
    // ─────────────────────────────────────────
    private Contact createPrimaryContact(String email, String phoneNumber) {
        Contact contact = Contact.builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .linkedId(null)
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .build();
        return contactRepository.save(contact);
    }

    // ─────────────────────────────────────────
    // Helper — Build the final response
    // ─────────────────────────────────────────
    private IdentifyResponse buildResponse(Contact primary, List<Contact> allContacts) {

        // Collect emails — primary first, no nulls, no duplicates
        List<String> emails = new ArrayList<>();
        if (primary.getEmail() != null) {
            emails.add(primary.getEmail());
        }
        allContacts.stream()
                .filter(c -> !c.getId().equals(primary.getId()))
                .filter(c -> c.getEmail() != null)
                .filter(c -> !emails.contains(c.getEmail()))
                .forEach(c -> emails.add(c.getEmail()));

        // Collect phone numbers — primary first, no nulls, no duplicates
        List<String> phoneNumbers = new ArrayList<>();
        if (primary.getPhoneNumber() != null) {
            phoneNumbers.add(primary.getPhoneNumber());
        }
        allContacts.stream()
                .filter(c -> !c.getId().equals(primary.getId()))
                .filter(c -> c.getPhoneNumber() != null)
                .filter(c -> !phoneNumbers.contains(c.getPhoneNumber()))
                .forEach(c -> phoneNumbers.add(c.getPhoneNumber()));

        // Collect secondary ids
        List<Integer> secondaryIds = allContacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .collect(Collectors.toList());

        ContactResponse contactResponse = ContactResponse.builder()
                .primaryContactId(primary.getId())
                .emails(emails)
                .phoneNumbers(phoneNumbers)
                .secondaryContactIds(secondaryIds)
                .build();

        return IdentifyResponse.builder()
                .contact(contactResponse)
                .build();
    }
}