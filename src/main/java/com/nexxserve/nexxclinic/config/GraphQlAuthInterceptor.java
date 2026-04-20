package com.nexxserve.nexxclinic.config;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.auth.AccessTokenInfo;
import com.nexxserve.nexxclinic.auth.JwtService;
import com.nexxserve.nexxclinic.service.SessionTokenService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GraphQlAuthInterceptor implements WebGraphQlInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GraphQlAuthInterceptor.class);

    private final JwtService jwtService;
    private final SessionTokenService sessionTokenService;

    public GraphQlAuthInterceptor(JwtService jwtService, SessionTokenService sessionTokenService) {
        this.jwtService = jwtService;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        Optional<AccessTokenInfo> tokenInfo = extractTokenInfo(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        Optional<AuthenticatedUser> authUser = tokenInfo.map(jwtService::toAuthenticatedUser);

        if (authUser.isPresent()) {
            logger.debug("User authenticated: {} with roles: {}", authUser.get().principal(), authUser.get().roles());
        } else {
            logger.debug("No authentication token found in request");
        }

        request.configureExecutionInput((executionInput, builder) ->
                builder.graphQLContext(contextBuilder -> {
                    authUser.ifPresent(user -> contextBuilder.put("authUser", user));
                    tokenInfo.ifPresent(info -> contextBuilder.put("accessTokenInfo", info));
                }).build()
        );

        return chain.next(request);
    }

    private Optional<AccessTokenInfo> extractTokenInfo(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.trace("No Bearer token found in Authorization header");
            return Optional.empty();
        }

        String token = authHeader.substring(7);
        try {
            AccessTokenInfo tokenInfo = jwtService.parseAccessToken(token);
            if (sessionTokenService.isAccessTokenRevoked(tokenInfo.tokenId())) {
                logger.warn("Access token has been revoked: {}", tokenInfo.tokenId());
                return Optional.empty();
            }
            logger.debug("Token parsed successfully for user: {}", tokenInfo.principal());
            return Optional.of(tokenInfo);
        } catch (Exception e) {
            logger.debug("Failed to parse access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
