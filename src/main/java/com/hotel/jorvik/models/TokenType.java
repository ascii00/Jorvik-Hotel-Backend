package com.hotel.jorvik.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Token_Type")
public class TokenType {

    public enum ETokenType {
        ACCESS,
        EMAIL_CONFIRMATION,
        RESET_PASSWORD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ETokenType type;

    public TokenType() {
    }

    public TokenType(ETokenType type) {
        this.type = type;
    }
}