package org.serratec.trabalhoFinal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.serratec.trabalhoFinal.domain.Endereco;
import org.serratec.trabalhoFinal.dto.EnderecoDTO;
import org.serratec.trabalhoFinal.repository.EnderecoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;

    public EnderecoService(EnderecoRepository enderecoRepository) {
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional
    public Endereco buscarOuCriar(EnderecoDTO dto) {
        List<Endereco> existentes = enderecoRepository.findByCep(dto.getCep());
        if (!existentes.isEmpty()) {
            return existentes.get(0); 
        }

        Endereco novo = new Endereco();
        novo.setCep(dto.getCep());
        novo.setLogradouro(dto.getLogradouro());
        novo.setComplemento(dto.getComplemento());
        novo.setBairro(dto.getBairro());
        novo.setLocalidade(dto.getLocalidade());
        novo.setUf(dto.getUf());
        return enderecoRepository.save(novo);
    }

    public EnderecoDTO buscar(String cep) {
        List<Endereco> enderecos = enderecoRepository.findByCep(cep);
        if (!enderecos.isEmpty()) {
            return toDto(enderecos.get(0)); 
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://viacep.com.br/ws/" + cep + "/json/";
            EnderecoDTO enderecoViaCep = restTemplate.getForObject(url, EnderecoDTO.class);

            if (enderecoViaCep != null && enderecoViaCep.getCep() != null) {
                Endereco novo = new Endereco();
                novo.setCep(enderecoViaCep.getCep().replaceAll("-", ""));
                novo.setLogradouro(enderecoViaCep.getLogradouro());
                novo.setComplemento(enderecoViaCep.getComplemento());
                novo.setBairro(enderecoViaCep.getBairro());
                novo.setLocalidade(enderecoViaCep.getLocalidade());
                novo.setUf(enderecoViaCep.getUf());
                enderecoRepository.save(novo);
                return toDto(novo);
            }

            throw new RuntimeException("CEP não encontrado: " + cep);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar o CEP: " + cep, e);
        }
    }


    public List<EnderecoDTO> listarTodos() {
        return enderecoRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private EnderecoDTO toDto(Endereco e) {
        EnderecoDTO dto = new EnderecoDTO();
        dto.setCep(e.getCep());
        dto.setLogradouro(e.getLogradouro());
        dto.setComplemento(e.getComplemento());
        dto.setBairro(e.getBairro());
        dto.setLocalidade(e.getLocalidade());
        dto.setUf(e.getUf());
        return dto;
    }
}