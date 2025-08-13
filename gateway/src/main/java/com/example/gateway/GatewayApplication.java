package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }


    @Bean
    RouteLocator gateway(ExtensionAuthGatewayFilter filter, RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder
                .routes()
                .route(new Function<PredicateSpec, Buildable<Route>>() {
                    @Override
                    public Buildable<Route> apply(PredicateSpec predicateSpec) {
                        return predicateSpec
                                .path("/**")
                                .filters(spec -> spec.filters(filter))
                                .uri("http://localhost:8080");
                    }
                })
                .build();

    }
}


@Component
class ExtensionAuthGatewayFilter implements GatewayFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var sessionId = extractSessionFromHeaders(request);
        if (sessionId != null) {
            //
            var modifiedRequest = request
                    .mutate() //
                    .headers(httpHeaders -> {
                        var cookie = "Cookie";
                        var existingCookies = httpHeaders.getFirst(cookie);
                        var newCookieHeader = buildCookieHeader(existingCookies, sessionId);
                        httpHeaders.set(cookie, newCookieHeader);
                        for (var header : AUTH_HEADERS) {
                            if (!header.equals(AUTHORIZATION))
                                httpHeaders.remove(header);
                        }
                    }) //
                    .build();
            var modifiedExchange = exchange
                    .mutate()
                    .request(modifiedRequest)
                    .build();
            return chain.filter(modifiedExchange);
        }

        return chain.filter(exchange);
    }

    private final String AUTHORIZATION = "Authorization";

    private final String XSESSIONID = "X-Session-ID";

    private final String XSESSION = "X-Extension-Session";

    private final List<String> AUTH_HEADERS = List.of(AUTHORIZATION, XSESSIONID, XSESSION);

    private String extractSessionFromHeaders(ServerHttpRequest request) {
        var sessionId = (String) null;
        for (var header : AUTH_HEADERS) {
            sessionId = request.getHeaders().getFirst(header);
            if (sessionId != null) {
                break;
            }
        }
        var session = "Session ";
        if (sessionId != null && sessionId.startsWith(session)) {
            sessionId = sessionId.substring(session.length());
        }
        return sessionId;
    }

    private String buildCookieHeader(String existingCookies, String sessionId) {
        var cookieBuilder = new StringBuilder();
        if (existingCookies != null && !existingCookies.isEmpty()) {
            var cookies = existingCookies.split(";");
            for (var cookie : cookies) {
                var trimmed = cookie.trim();
                if (!trimmed.startsWith("JSESSIONID=")) {
                    if (!cookieBuilder.isEmpty()) {
                        cookieBuilder.append("; ");
                    }
                    cookieBuilder.append(trimmed);
                }
            }
        }
        if (!cookieBuilder.isEmpty()) {
            cookieBuilder.append("; ");
        }
        cookieBuilder.append("JSESSIONID=").append(sessionId);

        return cookieBuilder.toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
