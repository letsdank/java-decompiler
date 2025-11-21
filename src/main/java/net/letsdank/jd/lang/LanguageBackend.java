package net.letsdank.jd.lang;

import net.letsdank.jd.ast.MethodAst;
import net.letsdank.jd.ast.MethodDecompiler;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.MethodInfo;

/**
 * Языковой бэкенд: знает, как из AST/модели класса сделать исходник на конкретном языке.
 */
public interface LanguageBackend {

    Language language();

    /**
     * Декомпилировать один метод.
     */
    String decompileMethod(ClassFile cf, MethodInfo method, MethodAst ast);

    /**
     * Декомпиляция всего класса целиком.
     * Для простоты бэкенд сам вызывает MethodDecompiler для каждого метода.
     */
    String decompileClass(ClassFile cf, MethodDecompiler methodDecompiler);
}
