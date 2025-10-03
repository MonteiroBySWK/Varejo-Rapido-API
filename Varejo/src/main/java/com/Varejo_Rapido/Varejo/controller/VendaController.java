package com.Varejo_Rapido.Varejo.controller;

import com.Varejo_Rapido.Varejo.model.Venda;
import com.Varejo_Rapido.Varejo.repository.VendaRepository;
import com.Varejo_Rapido.Varejo.service.DatParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/vendas")
public class VendaController {
    private final VendaRepository vendaRepository;
    private final DatParserService datParserService;

    public VendaController(VendaRepository vendaRepository, DatParserService datParserService) {
        this.vendaRepository = vendaRepository;
        this.datParserService = datParserService;
    }

    // Endpoint principal
    @GetMapping
    public List<Venda> listar() {
        return vendaRepository.findAll();
    }

    // Forçar reload do arquivo
    @PostMapping("/reload")
    public ResponseEntity<?> reload() throws IOException {
        return ResponseEntity.ok(datParserService.parseAndStore());
    }

    // Criar nova venda
    @PostMapping
    public ResponseEntity<Venda> criarVenda(@RequestBody Venda venda) {
        Venda saved = vendaRepository.save(venda);
        return ResponseEntity.ok(saved);
    }
}