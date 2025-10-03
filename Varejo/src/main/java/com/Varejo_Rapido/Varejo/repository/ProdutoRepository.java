package com.Varejo_Rapido.Varejo.repository;

import com.Varejo_Rapido.Varejo.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, String> {}
