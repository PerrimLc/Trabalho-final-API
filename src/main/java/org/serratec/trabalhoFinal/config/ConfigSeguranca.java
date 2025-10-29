package org.serratec.trabalhoFinal.config;

import java.util.Arrays;

import org.serratec.trabalhoFinal.security.JwtAuthenticationFilter;
import org.serratec.trabalhoFinal.security.JwtAuthorizationFilter;
import org.serratec.trabalhoFinal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class ConfigSeguranca {

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	JwtUtil jwtUtil;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
	            .authorizeHttpRequests(auth -> auth
	                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
	                    .requestMatchers(HttpMethod.POST, "api/login").permitAll()
	                    .requestMatchers(HttpMethod.POST, "api/clientes").permitAll()
	                    .requestMatchers(HttpMethod.GET, "api/produtos/**").permitAll()
	                    .requestMatchers(HttpMethod.GET, "api/categorias/**").permitAll()
	                    .requestMatchers(HttpMethod.GET, "api/enderecos/**").permitAll()
	                    .requestMatchers(HttpMethod.GET, "api/planos/**").permitAll()
	                    .requestMatchers("api/funcionarios/**").hasRole("ADMIN")
	                    .requestMatchers("api/cashbacks/adicionar/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.GET, "api/clientes").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.GET, "api/pedidos").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.POST, "api/produtos").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.PUT, "api/produtos/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.DELETE, "api/produtos/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.POST, "api/categorias").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.PUT, "api/categorias/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.DELETE, "api/categorias/**").hasRole("ADMIN")
	                    .requestMatchers("api/estoque/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.POST, "api/planos").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.PUT, "api/planos/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.DELETE, "api/planos/**").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.GET, "api/clientes/{id}").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers(HttpMethod.PUT, "api/clientes/{id}").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers(HttpMethod.DELETE, "api/clientes/{id}").hasRole("ADMIN")
	                    .requestMatchers(HttpMethod.GET, "api/pedidos/meus").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers(HttpMethod.GET, "api/pedidos/{id}").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers(HttpMethod.DELETE, "api/pedidos/{id}").hasRole("ADMIN")
	                    .requestMatchers("api/cashbacks/cliente/{clienteId}").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers("api/pedidos/cart/*").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers("api/pedidos/pagar/*").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers("api/clientes/*/wishlist/**").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers("api/assinaturas/**").hasAnyRole("ADMIN", "USER")
	                    .requestMatchers("api/recomendacoes/**").hasAnyRole("ADMIN", "USER")
	                    .anyRequest().authenticated() 
	            ).sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
				authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)), jwtUtil);
		jwtAuthenticationFilter.setFilterProcessesUrl("/login");

		JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(
				authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)), jwtUtil,
				userDetailsService);

		http.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilter(jwtAuthenticationFilter);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
		corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
		corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
		corsConfiguration.setExposedHeaders(Arrays.asList("Authorization"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfiguration);
		return source;
	}

	@Bean
	public BCryptPasswordEncoder bcryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
