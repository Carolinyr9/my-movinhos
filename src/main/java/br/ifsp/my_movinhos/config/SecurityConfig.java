package br.ifsp.my_movinhos.config;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de Segurança para o Monolito (Resource Server).
 *
 * Esta classe configura o serviço para atuar como um "Resource Server",
 * responsável por proteger os endpoints e validar os tokens JWT recebidos.
 * Inclui customização para mapear roles do claim 'authorities' do JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita a segurança em nível de método (ex: @PreAuthorize)
public class SecurityConfig {

    // Injeta a chave pública RSA a partir do application.properties.
    // Esta chave é usada para VERIFICAR a assinatura dos tokens.
    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    /**
     * Define a cadeia de filtros de segurança que será aplicada a todas as requisições.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desabilita a proteção contra CSRF, pois a API é stateless.
            .csrf(csrf -> csrf.disable())

            // 2. Configura a gestão de sessão para ser STATELESS.
            // O servidor não criará nem manterá nenhuma sessão HTTP.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. Configura o serviço como um Resource Server que valida tokens JWT.
            // Todas as requisições devem conter um Bearer Token válido.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())) // Usa o conversor customizado
            )

            // 4. Define as regras de autorização para os endpoints.
            .authorizeHttpRequests(authorize -> authorize
                // Exemplo: Permitir acesso a endpoints públicos (se houver)
                // .requestMatchers("/api/public/**").permitAll()
                // Define que o endpoint de criação de filmes requer a role ADMIN
                // .requestMatchers(HttpMethod.POST, "/api/movies").hasRole("ADMIN") // Exemplo do seu controller
                // Exige que QUALQUER outra requisição seja autenticada.
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Cria um Bean de JwtDecoder.
     * Este bean será usado pelo Spring Security para decodificar e validar os tokens JWT
     * usando a chave pública RSA fornecida.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
    }

    /**
     * Cria um JwtAuthenticationConverter customizado para extrair as autoridades (roles) do JWT.
     *
     * Este conversor:
     * 1. Define o nome do claim onde as autoridades estão ("authorities").
     * 2. Remove o prefixo padrão "SCOPE_" se não for usado.
     * 3. Garante que as roles sejam adicionadas como SimpleGrantedAuthority.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Define que as autoridades serão extraídas do claim "authorities"
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        
        // Como o seu JWT já retorna "ROLE_ADMIN", não precisamos adicionar o prefixo "ROLE_" novamente.
        // Se o seu JWT retornasse apenas "ADMIN", você precisaria de:
        // grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        // Mas como já vem formatado, podemos deixar o prefixo vazio ou não setar.
        // Para garantir que nada seja adicionado por engano, setamos para vazio.
        grantedAuthoritiesConverter.setAuthorityPrefix(""); 

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtConverter;
    }
}
