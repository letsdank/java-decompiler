package net.letsdank.jd.kotlin

import kotlin.metadata.*

/**
 * Утилиты над kotlin-metadata-jvm для использования из Java.
 *
 * Важно:
 *  - здесь мы используем только публичные extension-свойства:
 *    KmClass.isData, KmProperty.isVar, KmType.isNullable, isDefinitelyNonNull, isSuspend и т.п.
 *  - никакого прямого доступа к flags и никаких классов Flags.
 *
 * В Java это все видно как статические методы класса MetadataFlagsKt:
 *    MetadataFlagsKt.isDataClass(kmClass)
 */
fun isDataClass(kmClass: KmClass): Boolean =
    kmClass.isData

fun isEnumClass(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.ENUM_CLASS

fun isObjectClass(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.OBJECT

fun isCompanionObject(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.COMPANION_OBJECT

// --- KmProperty ---

fun isVarProperty(prop: KmProperty): Boolean =
    prop.isVar

// --- KmType ---

fun isNullable(type: KmType): Boolean =
    type.isNullable

fun isDefinitelyNotNull(type: KmType): Boolean =
    type.isDefinitelyNonNull

fun isSuspend(type: KmType): Boolean =
    type.isSuspend