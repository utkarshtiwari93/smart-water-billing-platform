package com.infosys.Water_Usage_Monitoring.service;

public interface AquaBotAnswerProvider {

    String answer(String message);

    default String answer(String message, AquaBotResidentContext context) {
        return answer(message);
    }

    String answer(String message, AquaBotAdminContext context);
}
