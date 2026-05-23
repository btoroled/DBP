package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateFlashcardRequest {

    @NotBlank(message = "La pregunta es obligatoria")
    private String question;

    @NotBlank(message = "La respuesta es obligatoria")
    private String answer;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}