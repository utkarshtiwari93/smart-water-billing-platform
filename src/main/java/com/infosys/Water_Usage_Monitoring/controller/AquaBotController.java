package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AquaBotChatRequest;
import com.infosys.Water_Usage_Monitoring.dto.AquaBotChatResponse;
import com.infosys.Water_Usage_Monitoring.service.AquaBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.aquabot", name = "enabled", havingValue = "true")
public class AquaBotController {

    private final AquaBotService service;

    @PostMapping("/chat")
    @PreAuthorize("hasRole('RESIDENT')")
    public AquaBotChatResponse chat(@Valid @RequestBody AquaBotChatRequest request,
                                    Authentication authentication) {
        return service.chat(request.message(), authentication.getName());
    }

    @PostMapping("/admin/chat")
    @PreAuthorize("hasRole('ADMIN')")
    public AquaBotChatResponse adminChat(@Valid @RequestBody AquaBotChatRequest request,
                                         Authentication authentication) {
        return service.adminChat(request.message(), authentication.getName());
    }
}
