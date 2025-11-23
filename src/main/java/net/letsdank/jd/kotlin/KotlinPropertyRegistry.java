package net.letsdank.jd.kotlin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Глобальный реестр Kotlin-свойств:
 * - ключ: внутреннее имя класса/файла, например "net/letsdank/jd/fixtures/User";
 * - значение: имена свойств (val/var) этого типа/файла.
 * <p>
 * Никакого сложного кэша: простой static-реестр на время жизни процесса.
 */
public final class KotlinPropertyRegistry {

    private static final Map<String, Set<String>> propertiesByOwner = new HashMap<>();

    private KotlinPropertyRegistry() {
    }

    public static synchronized void register(String ownerInternalName, Set<String> propertyNames) {
        if (ownerInternalName == null || propertyNames == null || propertyNames.isEmpty()) return;

        // Временный лог, чтобы убедиться, что сюда доходим:
        System.out.println("KPR: register " + ownerInternalName + " -> " + propertyNames);

        Set<String> existing = propertiesByOwner.computeIfAbsent(ownerInternalName, k -> new HashSet<>());
        existing.addAll(propertyNames);
    }

    public static synchronized boolean hasProperty(String ownerInternalName, String propertyName) {
        if (ownerInternalName == null || propertyName == null) return false;
        Set<String> props = propertiesByOwner.get(ownerInternalName);
        return props != null && props.contains(propertyName);
    }

    public static synchronized Set<String> getProperties(String ownerInternalName) {
        Set<String> props = propertiesByOwner.get(ownerInternalName);
        return props == null ? Set.of() : Set.copyOf(props);
    }

    public static synchronized void clear() {
        propertiesByOwner.clear();
    }
}
