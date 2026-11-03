package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AquaBotChatResponse;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotErrorCode;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AquaBotService {

    private final ObjectProvider<AquaBotAnswerProvider> answerProvider;
    private final AquaBotResidentContextService contextService;
    private final AquaBotAdminContextService adminContextService;

    public AquaBotChatResponse chat(String message, String authenticatedEmail) {
        AquaBotAnswerProvider provider = getProvider();
        AquaBotResidentContext context = contextService.forResident(authenticatedEmail);
        return response(provider.answer(message, context));
    }

    public AquaBotChatResponse adminChat(String message, String authenticatedEmail) {
        AquaBotAnswerProvider provider = getProvider();
        AquaBotAdminContext context = adminContextService.forAdmin(authenticatedEmail);
        return response(provider.answer(message, context));
    }

    private AquaBotAnswerProvider getProvider() {
        AquaBotAnswerProvider provider = answerProvider.getIfAvailable();
        if (provider == null) {
            throw new AquaBotException(AquaBotErrorCode.PROVIDER_NOT_CONFIGURED);
        }
        return provider;
    }

    private AquaBotChatResponse response(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new AquaBotException(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE);
        }
        return new AquaBotChatResponse(answer);
    }
}
