package org.serratec.trabalhoFinal.service;

import java.util.Collections;
import java.util.Optional;

import org.serratec.trabalhoFinal.domain.Cliente;
import org.serratec.trabalhoFinal.domain.Funcionario;
import org.serratec.trabalhoFinal.repository.ClienteRepository;
import org.serratec.trabalhoFinal.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetalheImpl implements UserDetailsService {

	@Autowired
	private ClienteRepository clienteRepository;
	
	@Autowired 
    private FuncionarioRepository funcionarioRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(username);
        if (funcionarioOpt.isPresent()) {
            Funcionario funcionario = funcionarioOpt.get();
             if (funcionario.getUsuario() == null) {
                throw new UsernameNotFoundException("Funcionário não possui credenciais definidas.");
            }
            
            String role = funcionario.getUsuario().getRole();
            if (role == null) role = "ROLE_ADMIN";

            return User.builder()
	            .username(funcionario.getUsuario().getUsername())
	            .password(funcionario.getUsuario().getPassword())
	            .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
	            .build();
        }
		
        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(username);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            if (cliente.getUsuario() == null) {
	            throw new UsernameNotFoundException("Cliente não possui credenciais definidas.");
	        }
	        
            String role = cliente.getUsuario().getRole();
	        if (role == null) role = "ROLE_USER";

            return User.builder()
	            .username(cliente.getUsuario().getUsername())
	            .password(cliente.getUsuario().getPassword())
	            .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
	            .build();
        }

	    throw new UsernameNotFoundException("Usuário não encontrado com o email: " + username);
	}


}