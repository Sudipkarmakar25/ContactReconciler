package com.reconciler.ContactReconciler.repository;

import com.reconciler.ContactReconciler.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {

    // Find all contacts matching email OR phone
    // Used in Step 1 of identify logic

    @Query("SELECT c FROM Contact c WHERE " +
            "(c.email = :email AND :email IS NOT NULL) OR " +
            "(c.phoneNumber = :phoneNumber AND :phoneNumber IS NOT NULL) " +
            "AND c.deletedAt IS NULL")
    List<Contact> findByEmailOrPhoneNumber(
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber
    );


    // Find all secondaries linked to a primary
    // Used when demoting a primary contact
    @Query("SELECT c FROM Contact c WHERE " +
            "c.linkedId = :linkedId " +
            "AND c.deletedAt IS NULL")
    List<Contact> findByLinkedId(
            @Param("linkedId") Integer linkedId
    );

    // Find entire cluster — primary + all secondaries
    // Used to build the final response
    @Query("SELECT c FROM Contact c WHERE " +
            "(c.id = :primaryId OR c.linkedId = :primaryId) " +
            "AND c.deletedAt IS NULL")
    List<Contact> findByIdOrLinkedId(
            @Param("primaryId") Integer primaryId
    );

}