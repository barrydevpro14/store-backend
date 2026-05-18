package org.store.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;
import org.store.security.application.service.ICurrentUserService;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * KeyGenerator partagé pour tous les caches scopés entreprise. Préfixe chaque clé par l'{@code entrepriseId}
 * du caller (résolu via {@link ICurrentUserService}) et le nom de méthode, puis ajoute les paramètres reçus.
 * <p>Évite la fuite cross-tenant : deux entreprises distinctes lisant le même cache (ex. {@code findAll(pageable)})
 * ont des clés différentes et ne se voient pas mutuellement leurs entrées.
 * <p>Référencé dans {@code @Cacheable(keyGenerator = "entrepriseScopedKeyGenerator")}.
 */
@Component
public class EntrepriseScopedKeyGenerator implements KeyGenerator {

    private final ICurrentUserService currentUserService;

    public EntrepriseScopedKeyGenerator(ICurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        Object[] keyParts = new Object[params.length + 2];
        keyParts[0] = entrepriseId;
        keyParts[1] = method.getName();
        System.arraycopy(params, 0, keyParts, 2, params.length);
        return new SimpleKey(keyParts);
    }
}
