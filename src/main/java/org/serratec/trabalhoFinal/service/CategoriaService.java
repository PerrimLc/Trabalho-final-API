package org.serratec.trabalhoFinal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.serratec.trabalhoFinal.domain.Categoria;
import org.serratec.trabalhoFinal.domain.Produto;
import org.serratec.trabalhoFinal.dto.CategoriaDTO;
import org.serratec.trabalhoFinal.exception.NotFoundException;
import org.serratec.trabalhoFinal.repository.CategoriaRepository;
import org.serratec.trabalhoFinal.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {
	
	@Autowired
    private final CategoriaRepository categoriaRepository;
	
	@Autowired
	private final ProdutoRepository produtoRepository;
    
    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) { 
    	this.categoriaRepository = categoriaRepository; 
    	this.produtoRepository = produtoRepository;
    }

    public CategoriaDTO criar(CategoriaDTO dto) {
    	
        Categoria c = new Categoria();
        c.setNome(dto.getNome());
        Categoria saved = categoriaRepository.save(c);
        dto.setId(saved.getId());
        return dto;
    }

    public CategoriaDTO atualizar(Long id, CategoriaDTO dto) {
    	
        Categoria c = categoriaRepository.findById(id).orElseThrow(() -> new NotFoundException("Não foi possível realizar essa atualização. Id não encontrado!"));
        c.setNome(dto.getNome());
        categoriaRepository.save(c);
        dto.setId(c.getId());
        return dto;
    }

    public List<CategoriaDTO> listar() {
    	
        return categoriaRepository.findAll().stream().map(c -> {
            CategoriaDTO dto = new CategoriaDTO();
            dto.setId(c.getId());
            dto.setNome(c.getNome());
            return dto;
        }).collect(Collectors.toList());
    }

    public CategoriaDTO buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada! Id não encontrada."));
        return toDto(categoria);
    }
    
    private CategoriaDTO toDto(Categoria c) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
 

        return dto;
    }

    public void deletarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Não foi possível deletar esta categoria. Id não encontrada"));

        for (Produto p : categoria.getProdutos()) {
            p.setCategoria(null);
            produtoRepository.save(p);
        }

        categoriaRepository.delete(categoria);
    }
}