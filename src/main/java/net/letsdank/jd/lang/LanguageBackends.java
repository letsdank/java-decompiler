package net.letsdank.jd.lang;

import net.letsdank.jd.model.ClassFile;

import java.util.EnumMap;
import java.util.Map;

/**
 * Реестр доступных языковых бэкендов + простая фабрика.
 */
public final class LanguageBackends {
    private static final Map<Language, LanguageBackend> BY_LANG = new EnumMap<>(Language.class);

    static {
        register(new JavaLanguageBackend());
        register(new KotlinLanguageBackend());
    }

    private LanguageBackends() {
    }

    private static void register(LanguageBackend backend) {
        BY_LANG.put(backend.language(), backend);
    }

    public static LanguageBackend forLanguage(Language language) {
        LanguageBackend backend = BY_LANG.get(language);
        if (backend == null) {
            throw new IllegalArgumentException("No backend for language: " + language);
        }
        return backend;
    }

    /**
     * Очень простая автоматическая детекция:
     * - если класс выглядит как Kotlin (по эвристике), используем KOTLIN,
     * - иначе по умолчанию JAVA.
     */
    public static LanguageBackend autoDetect(ClassFile cf) {
        if (KotlinClassDetector.isKotlinClass(cf)) {
            return forLanguage(Language.KOTLIN);
        }
        return forLanguage(Language.JAVA);
    }
}
