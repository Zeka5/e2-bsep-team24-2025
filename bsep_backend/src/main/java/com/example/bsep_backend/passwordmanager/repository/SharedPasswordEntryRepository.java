package com.example.bsep_backend.passwordmanager.repository;

import com.example.bsep_backend.passwordmanager.domain.SharedPasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedPasswordEntryRepository extends JpaRepository<SharedPasswordEntry, Long> {

    @Query("SELECT spe FROM SharedPasswordEntry spe " +
           "LEFT JOIN FETCH spe.sharedByUser " +
           "LEFT JOIN FETCH spe.sharedWithUser " +
           "LEFT JOIN FETCH spe.sharedWithCertificate " +
           "WHERE spe.sharedWithUser.id = :userId")
    List<SharedPasswordEntry> findBySharedWithUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT spe FROM SharedPasswordEntry spe " +
           "LEFT JOIN FETCH spe.sharedByUser " +
           "LEFT JOIN FETCH spe.sharedWithUser " +
           "WHERE spe.sharedByUser.id = :userId")
    List<SharedPasswordEntry> findBySharedByUserIdWithDetails(@Param("userId") Long userId);
}
