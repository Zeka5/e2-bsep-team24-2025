package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.pki.domain.KeystorePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeystorePasswordRepository extends JpaRepository<KeystorePassword, Long> {

    Optional<KeystorePassword> findByKeystoreAlias(String keystoreAlias);

    void deleteByKeystoreAlias(String keystoreAlias);

    boolean existsByKeystoreAlias(String keystoreAlias);
}