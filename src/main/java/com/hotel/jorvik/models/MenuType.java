package com.hotel.jorvik.models;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
@Entity
@Table(name = "Menu_Type")
public class MenuType {

    enum EMenu {
        BREAKFAST,
        LUNCH,
        DINNER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int ID;

    @Enumerated(EnumType.STRING)
    @NotBlank(message = "Name is mandatory")
    @Size(min = 3, max = 50, message = "Name cannot be less that 3 and more than 50 characters")
    @Column(name = "name", nullable = false)
    private EMenu name;

    public MenuType() {
    }

    public MenuType(EMenu name) {
        this.name = name;
    }
}
