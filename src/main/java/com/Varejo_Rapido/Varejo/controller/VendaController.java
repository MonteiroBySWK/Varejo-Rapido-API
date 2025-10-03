package com.Varejo_Rapido.Varejo.controller;

import com.Varejo_Rapido.Varejo.model.Cliente;
import com.Varejo_Rapido.Varejo.model.Produto;
import com.Varejo_Rapido.Varejo.model.Venda;
import com.Varejo_Rapido.Varejo.model.dto.ItemRequestBody;

import com.Varejo_Rapido.Varejo.model.dto.VendaResponse;
import com.Varejo_Rapido.Varejo.repository.ClienteRepository;
import com.Varejo_Rapido.Varejo.repository.ProdutoRepository;
import com.Varejo_Rapido.Varejo.repository.VendaRepository;
import com.Varejo_Rapido.Varejo.service.DatParserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/vendas")
public class VendaController {
    private final VendaRepository vendaRepository;
    private final DatParserService datParserService;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public VendaController(VendaRepository vendaRepository, DatParserService datParserService, 
                          ProdutoRepository produtoRepository, ClienteRepository clienteRepository) {
        this.vendaRepository = vendaRepository;
        this.datParserService = datParserService;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    // Endpoint principal - lista todas as vendas
    @GetMapping
    public List<Venda> listar() {
        return vendaRepository.findAll();
    }
    
    // Endpoint com paginação (para quando houver muitos dados)
    @GetMapping("/paginado")
    public Page<Venda> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        // Validar campo de ordenação para evitar erros
        String[] validSortFields = {"id", "dataVenda", "quantidade", "valorUnitario", "valorTotalVenda"};
        boolean isValidField = java.util.Arrays.asList(validSortFields).contains(sortBy);
        if (!isValidField) {
            sortBy = "id"; // Default seguro
        }
        
        org.springframework.data.domain.Sort.Direction direction = 
            sortDir.equalsIgnoreCase("desc") ? 
            org.springframework.data.domain.Sort.Direction.DESC : 
            org.springframework.data.domain.Sort.Direction.ASC;
            
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by(direction, sortBy));
            
        return vendaRepository.findAll(pageable);
    }
    
    // Pesquisa unificada - busca por nome de cliente OU nome de produto
    @GetMapping("/buscar")
    public List<Venda> buscar(@RequestParam String query) {
        List<Venda> vendas = new ArrayList<>();
        
        // Buscar vendas por nome de cliente
        List<Cliente> clientesEncontrados = clienteRepository.findByNomeContainingIgnoreCase(query);
        for (Cliente cliente : clientesEncontrados) {
            List<Venda> vendasDoCliente = vendaRepository.findByCliente(cliente);
            vendas.addAll(vendasDoCliente);
        }
        
        // Buscar vendas por nome de produto
        List<Produto> produtosEncontrados = produtoRepository.findByNomeContainingIgnoreCase(query);
        for (Produto produto : produtosEncontrados) {
            List<Venda> vendasDoProduto = vendaRepository.findByProduto(produto);
            // Evitar duplicatas
            for (Venda venda : vendasDoProduto) {
                if (!vendas.contains(venda)) {
                    vendas.add(venda);
                }
            }
        }
        
        return vendas;
    }

    // Forçar reload do arquivo
    @PostMapping("/reload")
    public ResponseEntity<?> reload() throws IOException {
        return ResponseEntity.ok(datParserService.parseAndStore());
    }

    // Processar dados do arquivo DAT
    @PostMapping
    public ResponseEntity<VendaResponse> processarVendas(@RequestBody List<ItemRequestBody> items) {
        List<Venda> vendasCriadas = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        int totalProcessed = items.size();
        int totalSuccess = 0;
        
        for (ItemRequestBody item : items) {
            try {
                Produto produto = obterOuCriarProduto(item.getProduto());
                
                Cliente cliente = obterOuCriarCliente(item.getCliente());
                
                Venda venda = new Venda();
                venda.setProduto(produto);
                venda.setCliente(cliente);
                venda.setQuantidade(item.getQtd_vendida());
                venda.setValorUnitario(item.getProduto().getValor_unit());
                venda.setValorTotalVenda(item.getQtd_vendida() * item.getProduto().getValor_unit());
                venda.setDataVenda(parsearData(item.getData_venda()));
                
                Venda vendaSalva = vendaRepository.save(venda);
                vendasCriadas.add(vendaSalva);
                totalSuccess++;
                
            } catch (Exception e) {
                String errorMsg = "Erro ao processar item (Produto ID: " + 
                    item.getProduto().getId() + ", Cliente ID: " + 
                    item.getCliente().getId_cliente() + "): " + e.getMessage();
                erros.add(errorMsg);
                System.err.println(errorMsg);
            }
        }
        
        VendaResponse response = new VendaResponse();
        response.setMessage("Processamento concluído");
        response.setTotalProcessed(totalProcessed);
        response.setTotalSuccess(totalSuccess);
        response.setTotalErrors(erros.size());
        response.setErrors(erros);
        
        return ResponseEntity.ok(response);
    }
    
    private Produto obterOuCriarProduto(com.Varejo_Rapido.Varejo.model.dto.ProdutoDTO produtoDTO) {
        String produtoId = String.valueOf(produtoDTO.getId());
        Optional<Produto> produtoExistente = produtoRepository.findById(produtoId);
        
        if (produtoExistente.isPresent()) {
            return produtoExistente.get();
        } else {
            Produto novoProduto = new Produto();
            novoProduto.setId(produtoId);
            novoProduto.setNome(produtoDTO.getNome_produto());
            novoProduto.setValorUnitario(produtoDTO.getValor_unit());
            return produtoRepository.save(novoProduto);
        }
    }
    
    private Cliente obterOuCriarCliente(com.Varejo_Rapido.Varejo.model.dto.ClienteDTO clienteDTO) {
        String clienteId = String.valueOf(clienteDTO.getId_cliente());
        Optional<Cliente> clienteExistente = clienteRepository.findById(clienteId);
        
        if (clienteExistente.isPresent()) {
            return clienteExistente.get();
        } else {
            Cliente novoCliente = new Cliente();
            novoCliente.setId(clienteId);
            novoCliente.setNome(clienteDTO.getNome_cliente());
            return clienteRepository.save(novoCliente);
        }
    }
    
    private LocalDate parsearData(String dataString) {
        try {
            // Assumindo que a data vem no formato "dd/mm/yyyy"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dataString, formatter);
        } catch (Exception e) {
            // Se houver erro no parse, usar a data atual
            return LocalDate.now();
        }
    }
}