package project.code.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.stream.Collectors;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtConfig jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        //log check
        logger.info("JwtAuthFilter: Request to URI: {}", request.getRequestURI());
        logger.info("JwtAuthFilter: Authorization Header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                //Lấy UserDetails để kiểm tra token
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                //Lấy Claims và Roles từ Token
                final Claims claims = jwtService.extractAllClaims(jwt);
                final List<String> roles = claims.get("roles", List.class);

                if (roles == null) {
                    logger.warn("JwtAuthFilter: Token for user {} does not contain 'roles' claim.", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(roleString -> new SimpleGrantedAuthority(roleString))
                        .collect(Collectors.toList());

                logger.info("JwtAuthFilter: User: {}. Roles from Token: {}", userEmail, authorities);


                // Kiểm tra token có hợp lệ không
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JwtAuthFilter: Authenticated user: {} successfully.", userEmail);
                }
            }
        } catch (Exception e) {
            logger.error("!!! JwtAuthFilter Error: {} !!!", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}