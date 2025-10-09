package com.example.bsep_backend.passwordmanager.repository;

import com.example.bsep_backend.passwordmanager.domain.PasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {

    @Query("SELECT pe FROM PasswordEntry pe " +
           "LEFT JOIN FETCH pe.owner " +
           "LEFT JOIN FETCH pe.certificate " +
           "WHERE pe.owner.id = :ownerId")
    List<PasswordEntry> findByOwnerIdWithCertificate(@Param("ownerId") Long ownerId);
}
