package net.letsdank.jd.jang;

import net.letsdank.jd.lang.GenericSignatureParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericSignatureParserTest {

    @Test
    void parsesPrimitivesAndArrays() {
        var ms1 = GenericSignatureParser.parseMethodSignature("(I)I");
        assertEquals(List.of("int"), ms1.parameterTypes());
        assertEquals("int", ms1.returnType());

        var ms2 = GenericSignatureParser.parseMethodSignature("([I)[[I");
        assertEquals(List.of("int[]"), ms2.parameterTypes());
        assertEquals("int[][]", ms2.returnType());

        var ms3 = GenericSignatureParser.parseMethodSignature("([Ljava/lang/String;)V");
        assertEquals(List.of("java.lang.String[]"), ms3.parameterTypes());
        assertEquals("void", ms3.returnType());
    }

    @Test
    void parsesObjectGenerics() {
        var sig = "(Ljava/util/List<Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;";
        var ms = GenericSignatureParser.parseMethodSignature(sig);
        assertEquals(List.of("java.util.List<java.lang.String>"), ms.parameterTypes());
        assertEquals("java.util.Map<java.lang.String, java.lang.Integer>", ms.returnType());
    }

    @Test
    void parsesTypeVariablesAndBounds() {
        var sig = "<T:Ljava/lang/Number;>(TT;)TT;";
        var ms = GenericSignatureParser.parseMethodSignature(sig);
        assertEquals(1, ms.typeVariables().size());
        var tv = ms.typeVariables().getFirst();
        assertEquals("T", tv.name());
        assertEquals(List.of("java.lang.Number"), tv.bounds());
        assertEquals(List.of("T"), ms.parameterTypes());
        assertEquals("T", ms.returnType());
    }

    @Test
    void parseWildcardAndBounds() {
        var ms1 = GenericSignatureParser.parseMethodSignature("(Ljava/util/List<*>;)V");
        assertEquals(List.of("java.util.List<?>"), ms1.parameterTypes());
        assertEquals("void", ms1.returnType());

        var ms2 = GenericSignatureParser.parseMethodSignature("(Ljava/util/List<+Ljava/lang/Number;>;)V");
        assertEquals(List.of("java.util.List<? extends java.lang.Number>"), ms2.parameterTypes());

        var ms3 = GenericSignatureParser.parseMethodSignature("(Ljava/util/List<-Ljava/lang/Integer;>;)V");
        assertEquals(List.of("java.util.List<? super java.lang.Integer>"), ms3.parameterTypes());
    }

    @Test
    void parsesMultipleBounds() {
        // Примечание: пустое ограничение класса, за которым следует ограничение по интерфейсу, тоже допустимо;
        // здесь мы явно указываем Object как верхнюю границу.
        var sig = "<T:Ljava/lang/Object;:Ljava/io/Serializable;>(TT;)V";
        var ms = GenericSignatureParser.parseMethodSignature(sig);
        assertEquals(1, ms.typeVariables().size());
        var tv = ms.typeVariables().getFirst();
        assertEquals("T", tv.name());
        assertEquals(List.of("java.lang.Object", "java.io.Serializable"), tv.bounds());
        assertEquals(List.of("T"), ms.parameterTypes());
        assertEquals("void", ms.returnType());
    }

    @Test
    void parsesNestedGenerics() {
        var sig = "(Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ljava/lang/Integer;>;>;)V";
        var ms = GenericSignatureParser.parseMethodSignature(sig);
        assertEquals(List.of("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>"), ms.parameterTypes());
        assertEquals("void", ms.returnType());
    }
}
