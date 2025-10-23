package org.serratec.trabalhoFinal.dto;

import java.math.BigDecimal;

public class ProdutoDTO {
	
	private Long id;
	private String nome;
	private BigDecimal preco;
	private Long categoriaId;
	private String categoriaNome;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public BigDecimal getPreco() {
		return preco;
	}
	
	public void setPreco(BigDecimal preco) {
		this.preco = preco;
	}
	
	public Long getCategoriaId() {
		return categoriaId;
	}
	
	public void setCategoriaId(Long categoriaId) {
		this.categoriaId = categoriaId;
	}
	
	public String getCategoriaNome() {
		return categoriaNome;
	}
	
	public void setCategoriaNome(String categoriaNome) {
		this.categoriaNome = categoriaNome;
	}
}
