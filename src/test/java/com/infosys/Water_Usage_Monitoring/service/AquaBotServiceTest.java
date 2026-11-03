package com.infosys.Water_Usage_Monitoring.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotErrorCode;
import com.infosys.Water_Usage_Monitoring.exception.AquaBotException;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AquaBotServiceTest {

    @Mock ObjectProvider<AquaBotAnswerProvider> answerProvider;
    @Mock AquaBotAnswerProvider provider;
    @Mock AquaBotResidentContextService contextService;
    @Mock AquaBotAdminContextService adminContextService;
    @InjectMocks AquaBotService service;

    @Test
    void chat_withoutProvider_returnsServiceUnavailable() {
        when(answerProvider.getIfAvailable()).thenReturn(null);

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> service.chat("How can I save water?", "resident@example.com"));

        assertEquals(AquaBotErrorCode.PROVIDER_NOT_CONFIGURED, exception.getErrorCode());
        verifyNoInteractions(contextService);
    }

    @Test
    void chat_withProviderDelegatesMessageAndReturnsAnswer() {
        AquaBotResidentContext context = AquaBotResidentContext.withoutHousehold();
        when(answerProvider.getIfAvailable()).thenReturn(provider);
        when(contextService.forResident("resident@example.com")).thenReturn(context);
        when(provider.answer("How can I save water?", context)).thenReturn("Check for leaks.");

        var response = service.chat("How can I save water?", "resident@example.com");

        assertEquals("Check for leaks.", response.answer());
        verify(provider).answer("How can I save water?", context);
    }

    @Test
    void chat_whenProviderReturnsEmptyAnswer_returnsBadGateway() {
        AquaBotResidentContext context = AquaBotResidentContext.withoutHousehold();
        when(answerProvider.getIfAvailable()).thenReturn(provider);
        when(contextService.forResident("resident@example.com")).thenReturn(context);
        when(provider.answer("How can I save water?", context)).thenReturn("  ");

        AquaBotException exception = assertThrows(
                AquaBotException.class, () -> service.chat("How can I save water?", "resident@example.com"));

        assertEquals(AquaBotErrorCode.PROVIDER_INVALID_RESPONSE, exception.getErrorCode());
    }

    @Test
    void chat_whenResidentCannotBeAuthorized_doesNotCallProvider() {
        when(answerProvider.getIfAvailable()).thenReturn(provider);
        when(contextService.forResident("resident@example.com"))
                .thenThrow(new AccessDeniedException("Resident access denied"));

        assertThrows(AccessDeniedException.class,
                () -> service.chat("Show my bill", "resident@example.com"));

        verifyNoInteractions(provider);
    }

    @Test
    void adminChat_usesOnlyAdminContextAndReturnsExistingResponseContract() {
        AquaBotAdminContext context = new AquaBotAdminContext(
                java.util.List.of(), java.util.List.of(), 0, false, false, false);
        when(answerProvider.getIfAvailable()).thenReturn(provider);
        when(adminContextService.forAdmin("admin@example.com")).thenReturn(context);
        when(provider.answer("Show apartment totals", context)).thenReturn("Total: **900 L**");

        var response = service.adminChat("Show apartment totals", "admin@example.com");

        assertEquals("Total: **900 L**", response.answer());
        verify(provider).answer("Show apartment totals", context);
        verifyNoInteractions(contextService);
    }

    @Test
    void adminChat_whenAdminContextRejectsCaller_doesNotInvokeProvider() {
        when(answerProvider.getIfAvailable()).thenReturn(provider);
        when(adminContextService.forAdmin("resident@example.com"))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Denied"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.adminChat("Show totals", "resident@example.com"));

        verifyNoInteractions(provider, contextService);
    }
}
