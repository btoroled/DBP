package com.streakstudy.application.dto;

import com.streakstudy.domain.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateFlashcardRequest {

    @NotBlank(message = "La pregunta es obligatoria")
    @Size(max = 200)
    private String question;
    Difficulty difficulty;
    @NotBlank(message = "La respuesta es obligatoria")
    @Size(max = 2000)
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
    public Difficulty getDifficulty() {
        return difficulty;
    }
}