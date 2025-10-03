package com.Varejo_Rapido.Varejo.service;
import com.Varejo_Rapido.Varejo.model.Cliente;
import com.Varejo_Rapido.Varejo.model.Produto;
import com.Varejo_Rapido.Varejo.model.Venda;
import com.Varejo_Rapido.Varejo.repository.ClienteRepository;
import com.Varejo_Rapido.Varejo.repository.ProdutoRepository;
import com.Varejo_Rapido.Varejo.repository.VendaRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class DatParserService {
    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final Path datPath;

    public DatParserService(VendaRepository vendaRepository,
                            ProdutoRepository produtoRepository,
                            ClienteRepository clienteRepository,
                            @Value("${varejo.dat.path}") String datPathStr) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.datPath = Path.of(datPathStr);
    }

    @PostConstruct
    public void loadOnStartup() {
        try {
            System.out.println("📂 Arquivo .dat configurado em: " + datPath.toAbsolutePath());

            if (Files.exists(datPath)) {
                System.out.println("✅ Arquivo encontrado, iniciando parser...");
                parseAndStore();
            } else {
                System.err.println("⚠️ Arquivo não encontrado nesse caminho!");
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao carregar .dat: " + e.getMessage());
        }
    }

    public List<Venda> parseAndStore() throws IOException {
        List<Venda> saved = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(datPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Exemplo de parsing (ajuste conforme layout do .dat)
                String produtoId = line.substring(0,4).trim();
                String produtoNome = line.substring(4,58).trim();
                String clienteId = line.substring(58,62).trim();
                String clienteNome = line.substring(62,113).trim();
                int quantidade = Integer.parseInt(line.substring(113,115));
                double valorUnitario = Double.parseDouble(line.substring(115,125));
                LocalDate dataVenda = LocalDate.parse(line.substring(125,135));

                Produto p = produtoRepository.findById(produtoId).orElseGet(() -> {
                    Produto np = new Produto();
                    np.setId(produtoId);
                    np.setNome(produtoNome);
                    np.setValorUnitario(valorUnitario);
                    return np;
                });

                Cliente c = clienteRepository.findById(clienteId).orElseGet(() -> {
                    Cliente nc = new Cliente();
                    nc.setId(clienteId);
                    nc.setNome(clienteNome);
                    return nc;
                });

                Venda v = new Venda();
                v.setProduto(p);
                v.setCliente(c);
                v.setQuantidade(quantidade);
                v.setValorUnitario(valorUnitario);
                v.setDataVenda(dataVenda);
                v.setValorTotalVenda(quantidade * valorUnitario);

                saved.add(vendaRepository.save(v));
            }
        }
        return saved;
    }
}