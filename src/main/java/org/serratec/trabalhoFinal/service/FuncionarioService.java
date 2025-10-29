package org.serratec.trabalhoFinal.service;

import java.util.List;

import org.serratec.trabalhoFinal.domain.Funcionario;
import org.serratec.trabalhoFinal.domain.Usuario;
import org.serratec.trabalhoFinal.dto.FuncionarioCadastroDTO; // Novo DTO
import org.serratec.trabalhoFinal.repository.FuncionarioRepository;
import org.serratec.trabalhoFinal.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class FuncionarioService {

    private FuncionarioRepository funcionarioRepository;
    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder, EmailService emailService) {
		this.funcionarioRepository = funcionarioRepository;
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
	}
    
    public List<Funcionario> listarTodosEntidades() {
        return funcionarioRepository.findAll();
    }
    
	public Funcionario cadastrarFuncionario(FuncionarioCadastroDTO dto) {
		
		if (funcionarioRepository.findByEmail(dto.getEmail()).isPresent()) {
	        throw new RuntimeException("Email já cadastrado como funcionário."); 
	    }
		
        Usuario usuario = new Usuario();
        usuario.setUsername(dto.getEmail());
        
        usuario.setPassword(passwordEncoder.encode(dto.getSenha())); 
        
        usuario.setRole("ROLE_ADMIN"); 

        usuarioRepository.save(usuario);
        
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.getNome());
        funcionario.setEmail(dto.getEmail());
        funcionario.setUsuario(usuario); 

        Funcionario saved = funcionarioRepository.save(funcionario);
        emailService.enviarNotificacaoFuncionario(saved, "Criado");

        return saved;
    }
    
    public Funcionario buscarPorEmail(String email) {
        return funcionarioRepository.findByEmail(email).orElse(null);
    }
}
