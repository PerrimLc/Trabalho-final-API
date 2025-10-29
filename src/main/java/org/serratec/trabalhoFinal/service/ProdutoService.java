package org.serratec.trabalhoFinal.service;

import org.serratec.trabalhoFinal.domain.Categoria;
import org.serratec.trabalhoFinal.domain.Produto;
import org.serratec.trabalhoFinal.dto.ProdutoDTO;
import org.serratec.trabalhoFinal.exception.NotFoundException;
import org.serratec.trabalhoFinal.repository.CategoriaRepository;
import org.serratec.trabalhoFinal.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProdutoService(ProdutoRepository produtoRepo, CategoriaRepository categoriaRepo) {
        this.produtoRepo = produtoRepo;
        this.categoriaRepo = categoriaRepo;
    }

	public ProdutoDTO buscarPorId(Long id) {
        Produto produto = produtoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado com ID: " + id));
		
		return toDto(produto);
	}

    public ProdutoDTO criar(ProdutoDTO dto) {
    	
    	if(dto.getCategoriaId() == null) {
    		throw new IllegalArgumentException("O ID da categoria não pode ser nulo");
    	}
    	
        Categoria cat = categoriaRepo.findById(dto.getCategoriaId()).orElseThrow(() -> new NotFoundException("Categoria não encontrada!"));
        Produto p = new Produto();
        p.setNome(dto.getNome());
        p.setPreco(dto.getPreco());
        p.setCategoria(cat);
        
        Integer quantidadeEstoqueDto = dto.getQuantidadeEstoque();
        
        if (quantidadeEstoqueDto != null) {
            if (quantidadeEstoqueDto < 0) {
                
                throw new IllegalArgumentException("A quantidade em estoque não pode ser negativa.");
            }

            p.setQuantidadeEstoque(quantidadeEstoqueDto);
        } else {
        	p.setQuantidadeEstoque(0);
        }
        
        Produto saved = produtoRepo.save(p);
        return toDto(saved);
    }

    public ProdutoDTO atualizar(Long id, ProdutoDTO dto) {
        Produto p = produtoRepo.findById(id).orElseThrow(() -> new NotFoundException("Produto não encontrado!"));
        Categoria cat = categoriaRepo.findById(dto.getCategoriaId()).orElseThrow(() -> new NotFoundException("Categoria não encontrada!"));
        p.setNome(dto.getNome());
        p.setPreco(dto.getPreco());
        p.setCategoria(cat);
        
        Integer quantidadeEstoqueDto = dto.getQuantidadeEstoque();
        
        if (quantidadeEstoqueDto != null) {
            if (quantidadeEstoqueDto < 0) {
                
                throw new IllegalArgumentException("A quantidade em estoque não pode ser negativa.");
            }
            
            p.setQuantidadeEstoque(quantidadeEstoqueDto);
        }
        
        Produto s = produtoRepo.save(p);
        return toDto(s);
    }

    public List<ProdutoDTO> listar() {
        return produtoRepo.findAll()
                .stream()
                .filter(Produto::getAtivo)
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    public void deletar(Long id) {
        Produto p = produtoRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Produto não encontrado!"));
        p.setAtivo(false);
        produtoRepo.save(p);
    }
    
    public boolean verificarEstoque(Long produtoId, int quantidadeSolicitada) {
        Produto produto = produtoRepo.findById(produtoId).orElse(null);
        return produto != null && (produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0) >= quantidadeSolicitada;
    }
    
    public void darBaixaEstoque(Long produtoId, int quantidade) {
        Produto produto = produtoRepo.findById(produtoId)
            .orElseThrow(() -> new NotFoundException("Produto não encontrado!"));
        
        Integer estoqueAtual = produto.getQuantidadeEstoque();
        
        if (estoqueAtual == null || estoqueAtual < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente para dar baixa. Disponível: " + estoqueAtual);
        }
        
        produto.setQuantidadeEstoque(estoqueAtual - quantidade);
        produtoRepo.save(produto);
    }

    public void adicionarEstoque(Long produtoId, int quantidade) {
        Produto produto = produtoRepo.findById(produtoId)
            .orElseThrow(() -> new NotFoundException("Produto não encontrado!"));
        
        Integer estoqueAtual = produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0;
        
        produto.setQuantidadeEstoque(estoqueAtual + quantidade);
        produtoRepo.save(produto);
    }

    private ProdutoDTO toDto(Produto p) {
        ProdutoDTO dto = new ProdutoDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setPreco(p.getPreco());
        dto.setCategoriaId(p.getCategoria().getId());
        dto.setCategoriaNome(p.getCategoria().getNome());
        dto.setQuantidadeEstoque(p.getQuantidadeEstoque());
        return dto;
    }
}