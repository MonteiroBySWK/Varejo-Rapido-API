package com.Varejo_Rapido.Varejo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Produto {
    @Id
    private String id;
    private String nome;
    private double valorUnitario;
}
