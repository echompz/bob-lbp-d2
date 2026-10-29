package com.acme.claims.web;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimDocument;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.service.ClaimImportService;
import com.acme.claims.service.ClaimService;
import com.acme.claims.service.DocumentService;
import com.acme.claims.service.fraud.FraudAssessment;
import com.acme.claims.web.dto.ClaimDecisionRequest;
import com.acme.claims.web.dto.ClaimRequest;
import com.acme.claims.web.dto.ClaimResponse;
import com.acme.claims.web.dto.DocumentResponse;
import com.acme.claims.web.dto.ImportResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@Api(tags = "Claims", description = "Claim intake, review workflow and documents")
public class ClaimController {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ClaimImportService claimImportService;

    @ApiOperation("List claims, optionally filtered by status")
    @RequestMapping(method = RequestMethod.GET)
    public List<ClaimResponse> listClaims(@RequestParam(value = "status", required = false) ClaimStatus status) {
        List<Claim> claims;
        if (status != null) {
            claims = claimService.getClaimsByStatus(status);
        } else {
            claims = claimService.getAllClaims();
        }
        return DtoMapper.toClaimResponses(claims);
    }

    @ApiOperation("Get a single claim")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ClaimResponse getClaim(@PathVariable("id") Long id) {
        return DtoMapper.toResponse(claimService.getClaim(id));
    }

    @ApiOperation("Submit (first notice of loss) a new claim")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClaimResponse> submitClaim(@Valid @RequestBody ClaimRequest request)
            throws ClaimValidationException {
        Claim claim = claimService.submitClaim(request);
        return new ResponseEntity<ClaimResponse>(DtoMapper.toResponse(claim), HttpStatus.CREATED);
    }

    @ApiOperation("Assign the claim to the current adjuster and start the review")
    @RequestMapping(value = "/{id}/review", method = RequestMethod.POST)
    public ClaimResponse startReview(@PathVariable("id") Long id, Principal principal) {
        return DtoMapper.toResponse(claimService.startReview(id, principal.getName()));
    }

    @ApiOperation("Approve or reject a claim (MANAGER only)")
    @RequestMapping(value = "/{id}/decision", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ClaimResponse decide(@PathVariable("id") Long id, @Valid @RequestBody ClaimDecisionRequest request,
                                Principal principal) throws ClaimValidationException {
        return DtoMapper.toResponse(claimService.decide(id, request, principal.getName()));
    }

    @ApiOperation("Release payment for an approved claim (MANAGER only)")
    @RequestMapping(value = "/{id}/pay", method = RequestMethod.POST)
    public ClaimResponse pay(@PathVariable("id") Long id, Principal principal) {
        return DtoMapper.toResponse(claimService.markPaid(id, principal.getName()));
    }

    @ApiOperation("Re-run the fraud rules against a claim")
    @RequestMapping(value = "/{id}/fraud-assessment", method = RequestMethod.GET)
    public FraudAssessment fraudAssessment(@PathVariable("id") Long id) {
        return claimService.getFraudAssessment(id);
    }

    @ApiOperation("Attach a supporting document")
    @RequestMapping(value = "/{id}/documents", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(@PathVariable("id") Long id,
                                                           @RequestParam("file") MultipartFile file,
                                                           Principal principal)
            throws IOException, ClaimValidationException {
        ClaimDocument document = documentService.attachDocument(id, file.getOriginalFilename(),
                file.getContentType(), file.getBytes(), principal.getName());
        return new ResponseEntity<DocumentResponse>(DtoMapper.toResponse(document), HttpStatus.CREATED);
    }

    @ApiOperation("List documents attached to a claim")
    @RequestMapping(value = "/{id}/documents", method = RequestMethod.GET)
    public List<DocumentResponse> listDocuments(@PathVariable("id") Long id) {
        return DtoMapper.toDocumentResponses(documentService.getDocuments(id));
    }

    @ApiOperation("Download a document")
    @RequestMapping(value = "/documents/{documentId}/content", method = RequestMethod.GET)
    public ResponseEntity<byte[]> downloadDocument(@PathVariable("documentId") Long documentId) {
        ClaimDocument document = documentService.getDocument(documentId);
        byte[] content = documentService.getDocumentContent(documentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getContentType()));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"");
        headers.setContentLength(content.length);
        return new ResponseEntity<byte[]>(content, headers, HttpStatus.OK);
    }

    @ApiOperation("Bulk import claims from the broker CSV feed")
    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importClaims(@RequestParam("file") MultipartFile file) throws IOException {
        return claimImportService.importClaims(file.getInputStream());
    }
}
