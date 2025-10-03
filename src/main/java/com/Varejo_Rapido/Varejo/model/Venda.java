package com.Varejo_Rapido.Varejo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class Venda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dataVenda;
    private int quantidade;
    private double valorUnitario;
    private double valorTotalVenda;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Produto produto;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Cliente cliente;
}
