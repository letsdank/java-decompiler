package net.letsdank.jd.model.cp;

/**
 * Базовый интерфейс для записей constant pool.
 */
public sealed interface CpInfo
        permits CpUtf8, CpInteger, CpFloat, CpLong, CpDouble,
        CpClass, CpString, CpFieldref, CpMethodref,
        CpInterfaceMethodref, CpNameAndType,
        CpMethodHandle, CpMethodType, CpDynamic,
        CpInvokeDynamic, CpModule, CpPackage {
    int tag();
}
