package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.FinalizeResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.service.InvoicePdfService;
import com.infosys.Water_Usage_Monitoring.service.InvoiceService;
import com.infosys.Water_Usage_Monitoring.security.AdminApartmentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final InvoicePdfService pdfService;
    private final AdminApartmentScope apartmentScope;

    @PostMapping("/billing-cycles/{cycleId}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    public FinalizeResponse finalizeCycle(@PathVariable Long cycleId, Authentication authentication) {
        apartmentScope.cycle(cycleId, authentication.getName());
        return service.finalizeCycle(cycleId);
    }

    @PostMapping("/billing-cycles/{cycleId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public FinalizeResponse archive(@PathVariable Long cycleId, Authentication authentication) {
        apartmentScope.cycle(cycleId, authentication.getName());
        return service.archive(cycleId);
    }

    @GetMapping("/billing-cycles/{cycleId}/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> listForCycle(@PathVariable Long cycleId, Authentication authentication) {
        apartmentScope.cycle(cycleId, authentication.getName());
        return service.listForCycle(cycleId);
    }

    @GetMapping("/invoices/my")
    public List<InvoiceResponse> myInvoices(Authentication authentication) {
        return service.myInvoices(authentication.getName());
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse get(@PathVariable Long id, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            apartmentScope.invoice(id, authentication.getName());
        }
        return service.get(id, authentication.getName());
    }

    @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Authentication authentication) throws IOException {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            apartmentScope.invoice(id, authentication.getName());
        }
        var pdfData = service.getPdfData(id, authentication.getName());
        byte[] pdf = pdfService.generate(pdfData);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(pdfData.invoice().invoiceNumber() + ".pdf").build().toString())
                .body(pdf);
    }
}