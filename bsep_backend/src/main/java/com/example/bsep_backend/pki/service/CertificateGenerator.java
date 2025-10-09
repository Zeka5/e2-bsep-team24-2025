package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.pki.domain.Issuer;
import com.example.bsep_backend.pki.domain.Subject;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class CertificateGenerator {

    public CertificateGenerator() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private SubjectKeyIdentifier createSubjectKeyIdentifier(java.security.PublicKey publicKey) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(encoded);
        return new SubjectKeyIdentifier(hash);
    }

    private AuthorityKeyIdentifier createAuthorityKeyIdentifier(java.security.PublicKey publicKey) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(encoded);
        return new AuthorityKeyIdentifier(hash);
    }

    public X509Certificate generateCertificate(Subject subject, Issuer issuer,
                                             LocalDateTime notBefore, LocalDateTime notAfter,
                                             String serialNumber) throws Exception {

        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        builder = builder.setProvider("BC");
        ContentSigner contentSigner = builder.build(issuer.getPrivateKey());

        Date startDate = Date.from(notBefore.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(notAfter.atZone(ZoneId.systemDefault()).toInstant());

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                issuer.getX500Name(),
                new BigInteger(serialNumber),
                startDate,
                endDate,
                subject.getX500Name(),
                subject.getPublicKey());

        X509CertificateHolder certHolder = certGen.build(contentSigner);

        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        certConverter = certConverter.setProvider("BC");

        return certConverter.getCertificate(certHolder);
    }

    public X509Certificate generateCertificateWithSAN(Subject subject, Issuer issuer,
                                                    LocalDateTime notBefore, LocalDateTime notAfter,
                                                    String serialNumber, List<String> subjectAlternativeNames,
                                                    boolean isCA) throws Exception {

        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        builder = builder.setProvider("BC");
        ContentSigner contentSigner = builder.build(issuer.getPrivateKey());

        Date startDate = Date.from(notBefore.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(notAfter.atZone(ZoneId.systemDefault()).toInstant());

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                issuer.getX500Name(),
                new BigInteger(serialNumber),
                startDate,
                endDate,
                subject.getX500Name(),
                subject.getPublicKey());

        // BasicConstraints - critical for CA certificates
        if (isCA) {
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } else {
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        }

        // KeyUsage - critical
        if (isCA) {
            // CA certificates: keyCertSign and cRLSign
            certGen.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else {
            // End-entity certificates: digitalSignature, keyEncipherment, dataEncipherment
            certGen.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment));
        }

        // Subject Key Identifier (SKI) - non-critical
        SubjectKeyIdentifier ski = createSubjectKeyIdentifier(subject.getPublicKey());
        certGen.addExtension(Extension.subjectKeyIdentifier, false, ski);

        // Authority Key Identifier (AKI) - non-critical
        // For self-signed certificates, issuer public key = subject public key
        AuthorityKeyIdentifier aki = createAuthorityKeyIdentifier(issuer.getPublicKey());
        certGen.addExtension(Extension.authorityKeyIdentifier, false, aki);

        // Subject Alternative Names
        if (subjectAlternativeNames != null && !subjectAlternativeNames.isEmpty()) {
            GeneralName[] altNames = subjectAlternativeNames.stream()
                    .map(name -> {
                        if (name.startsWith("DNS:")) {
                            return new GeneralName(GeneralName.dNSName, name.substring(4));
                        } else if (name.startsWith("IP:")) {
                            return new GeneralName(GeneralName.iPAddress, name.substring(3));
                        } else {
                            // fallback: assume DNS
                            return new GeneralName(GeneralName.dNSName, name);
                        }
                    })
                    .toArray(GeneralName[]::new);

            GeneralNames subjectAltNames = new GeneralNames(altNames);
            certGen.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    subjectAltNames
            );
        }

        X509CertificateHolder certHolder = certGen.build(contentSigner);

        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        certConverter = certConverter.setProvider("BC");

        return certConverter.getCertificate(certHolder);
    }
}