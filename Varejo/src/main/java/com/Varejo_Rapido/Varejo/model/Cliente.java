package com.Varejo_Rapido.Varejo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Cliente {
    @Id
    private String id;
    private String nome;
}
