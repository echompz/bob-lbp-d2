package com.acme.claims.service;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.exception.ResourceNotFoundException;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimDocument;
import com.acme.claims.repository.ClaimDocumentRepository;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.service.notification.ClaimEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

/**
 * Stores supporting documents for claims. Content is persisted Base64-encoded
 * together with an MD5 checksum that is verified on download.
 */
@Service
@Transactional
public class DocumentService {

    public static final long MAX_DOCUMENT_SIZE = 5 * 1024 * 1024;

    @Autowired
    private ClaimDocumentRepository documentRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private NotificationService notificationService;

    public ClaimDocument attachDocument(Long claimId, String fileName, String contentType, byte[] content,
                                        String uploadedBy) throws ClaimValidationException {
        Claim claim = claimRepository.findOne(claimId);
        if (claim == null) {
            throw new ResourceNotFoundException("Claim", claimId);
        }
        if (content == null || content.length == 0) {
            throw new ClaimValidationException("Document '" + fileName + "' is empty");
        }
        if (content.length > MAX_DOCUMENT_SIZE) {
            throw new ClaimValidationException("Document '" + fileName + "' exceeds the maximum size of 5 MB");
        }
        if (claim.getStatus().isTerminal()) {
            throw new ClaimValidationException("Cannot attach documents to a " + claim.getStatus() + " claim");
        }

        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setFileName(fileName);
        document.setContentType(contentType != null ? contentType : "application/octet-stream");
        document.setSizeInBytes(content.length);
        document.setContentBase64(encode(content));
        document.setChecksum(checksum(content));
        document.setUploadedAt(new Date());
        document.setUploadedBy(uploadedBy);

        ClaimDocument saved = documentRepository.save(document);
        notificationService.publish(new ClaimEvent(ClaimEvent.DOCUMENT_ADDED, claim));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClaimDocument> getDocuments(Long claimId) {
        if (!claimRepository.exists(claimId)) {
            throw new ResourceNotFoundException("Claim", claimId);
        }
        return documentRepository.findByClaimId(claimId);
    }

    @Transactional(readOnly = true)
    public ClaimDocument getDocument(Long documentId) {
        ClaimDocument document = documentRepository.findOne(documentId);
        if (document == null) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return document;
    }

    @Transactional(readOnly = true)
    public byte[] getDocumentContent(Long documentId) {
        ClaimDocument document = getDocument(documentId);
        byte[] content = decode(document.getContentBase64());
        if (!checksum(content).equals(document.getChecksum())) {
            throw new IllegalStateException("Checksum mismatch for document " + documentId);
        }
        return content;
    }

    static String encode(byte[] content) {
        return new BASE64Encoder().encode(content);
    }

    static byte[] decode(String base64) {
        try {
            return new BASE64Decoder().decodeBuffer(base64);
        } catch (IOException e) {
            throw new RuntimeException("Unable to decode document content", e);
        }
    }

    static String checksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return DatatypeConverter.printHexBinary(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
