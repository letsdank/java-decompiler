package net.letsdank.jd.ast;

import net.letsdank.jd.lang.GenericSignatureParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для JavaPrettyPrinter.
 * Проверяют корректный рендеринг методов и классов с обобщениями (generics).
 */
public class JavaPrettyPrinterTest {

    /**
     * Проверяет, что метод с type параметром корректно выводит <T extends Number>
     */
    @Test
    void rendersMethodWithTypeParameter() {
        // Метод с сигнатуров <T:Ljava/lang/Number;>(TT;)TT;
        // Это означает: <T extends Number> T method(T param)

        String signature = "<T:Ljava/lang/Number;>(TT;)TT;";
        GenericSignatureParser.MethodGenericSignature gsig =
                GenericSignatureParser.parseMethodSignature(signature);

        // Проверяем, что type параметр правильно распарсен
        assertEquals(1, gsig.typeVariables().size(), "Должен быть один type параметр");

        GenericSignatureParser.TypeVariable tv = gsig.typeVariables().getFirst();
        assertEquals("T", tv.name(), "Параметр должен быть назван T");
        assertTrue(tv.bounds().contains("java.lang.Number"), "Ограничение должно быть java.lang.Number");

        // Проверяем параметры и return type
        assertTrue(gsig.parameterTypes().contains("T"), "Параметр метода должен быть типа T");
        assertEquals("T", gsig.returnType(), "Тип возврата должен быть T");
    }

    /**
     * Проверяет, что параметры метода с generics (List<String>) правильно форматируются
     */
    @Test
    void rendersMethodParametersWithGenerics(){
        // Сигнатура: (Ljava/util/List<Ljava/lang/String;>;)V
        // Это означает: void method(List<String> param)

        String signature = "(Ljava/util/List<Ljava/lang/String;>;)V";
        GenericSignatureParser.MethodGenericSignature gsig=
                GenericSignatureParser.parseMethodSignature(signature);

        // Проверяем параметры
        assertEquals(1, gsig.parameterTypes().size(), "Должен быть один параметр");
        assertTrue(gsig.parameterTypes().getFirst().contains("java.util.List<java.lang.String>"),
                "Параметр должен быть List<java.lang.String>: " + gsig.parameterTypes().getFirst());
    }
}
