package org.serratec.trabalhoFinal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.serratec.trabalhoFinal.domain.Cliente;
import org.serratec.trabalhoFinal.domain.Endereco;
import org.serratec.trabalhoFinal.domain.Usuario;
import org.serratec.trabalhoFinal.dto.ClienteAtualizacaoDTO;
import org.serratec.trabalhoFinal.dto.ClienteCriacaoDTO;
import org.serratec.trabalhoFinal.dto.ClienteDTO;
import org.serratec.trabalhoFinal.dto.EnderecoDTO;
import org.serratec.trabalhoFinal.exception.NotFoundException;
import org.serratec.trabalhoFinal.repository.ClienteRepository;
import org.serratec.trabalhoFinal.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EnderecoService enderecoService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, EnderecoService enderecoService,
                          EmailService emailService, UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.enderecoService = enderecoService;
        this.emailService = emailService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .filter(Cliente::getAtivo)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteDTO criar(ClienteCriacaoDTO dto) {
    	Usuario usuario = new Usuario();
    	usuario.setUsername(dto.getEmail());
    	usuario.setPassword(passwordEncoder.encode(dto.getSenha() != null ? dto.getSenha() : "123456"));
    	usuario.setRole("ROLE_USER");

    	Cliente cliente = new Cliente();
    	cliente.setNome(dto.getNome());
    	cliente.setTelefone(dto.getTelefone());
    	cliente.setEmail(dto.getEmail());
    	cliente.setCpf(dto.getCpf());
    	cliente.setUsuario(usuario);
    	usuario.setCliente(cliente);

    	Endereco endereco = new Endereco();
    	if (dto.getCep() != null) {
    	    EnderecoDTO enderecoDTO = enderecoService.buscar(dto.getCep());
    	    endereco = enderecoService.buscarOuCriar(enderecoDTO);
    	}
    	cliente.setEndereco(endereco);

    	Cliente saved = clienteRepository.save(cliente);
        emailService.enviarNotificacaoCliente(saved, "Sua conta foi criada em nosso site! Seja bem-vindo e beba muito café!");
        
        return toDto(saved);
    }

    @Transactional
    public ClienteDTO atualizar(Long id, @Valid ClienteAtualizacaoDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("As informações que foram passadas estão incorretas, tente novamente!"));

        cliente.setNome(dto.getNome());
        cliente.setTelefone(dto.getTelefone());
        cliente.setEmail(dto.getEmail());

        Usuario usuario = cliente.getUsuario();
        if (usuario != null) {
            usuario.setUsername(dto.getEmail());
            usuarioRepository.save(usuario);
        }

        if (dto.getCep() != null) {
            EnderecoDTO enderecoDTO = enderecoService.buscar(dto.getCep());
            Endereco endereco = enderecoService.buscarOuCriar(enderecoDTO);
            cliente.setEndereco(endereco);
        }

        Cliente saved = clienteRepository.save(cliente);
        emailService.enviarNotificacaoCliente(saved, "Sua conta foi atualizada!! Vai mais café aí?");
        
        return toDto(saved);
    }

    public ClienteDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado! Tente novamente."));
        return toDto(cliente);
    }
    
    @Transactional
    public void deletar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Não foi possível deletar esta conta! Tente novamente."));

        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private ClienteDTO toDto(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getUsuario() != null ? cliente.getUsuario().getId() : null);
        dto.setNome(cliente.getNome());
        dto.setTelefone(cliente.getTelefone());
        dto.setEmail(cliente.getEmail());
        dto.setCpf(cliente.getCpf());
        
        if (cliente.getUsuario() != null) {
            dto.setRole(cliente.getUsuario().getRole());
        }

        if (cliente.getEndereco() != null) {
            EnderecoDTO ed = new EnderecoDTO();
            ed.setCep(cliente.getEndereco().getCep());
            ed.setLogradouro(cliente.getEndereco().getLogradouro());
            ed.setComplemento(cliente.getEndereco().getComplemento());
            ed.setBairro(cliente.getEndereco().getBairro());
            ed.setLocalidade(cliente.getEndereco().getLocalidade());
            ed.setUf(cliente.getEndereco().getUf());
            dto.setEndereco(ed);
        }

        return dto;
    }
}