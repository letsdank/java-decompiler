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

fun isVarProperty(prop: KmProperty): Boolean =
    prop.isVar

fun isNullable(type: KmType): Boolean =
    type.isNullable

fun isDefinitelyNotNull(type: KmType): Boolean =
    type.isDefinitelyNonNull

fun isSuspend(type: KmType): Boolean =
    type.isSuspend

/** sealed class / sealed interface */
fun isSealedClass(kmClass: KmClass): Boolean =
    kmClass.modality == Modality.SEALED

/** enum class */
fun isEnumClass(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.ENUM_CLASS

/** object Foo */
fun isObjectClass(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.OBJECT

/** companion object Foo.Companion */
fun isCompanionObjectClass(kmClass: KmClass): Boolean =
    kmClass.kind == ClassKind.COMPANION_OBJECT

/** value class */
fun isValueClass(kmClass: KmClass): Boolean =
    kmClass.isValue

fun classKind(kmClass: KmClass): ClassKind =
    kmClass.kind

fun enumEntries(kmClass: KmClass): List<String> =
    kmClass.enumEntries

/** sealed subclasses (FQ-имена наследников sealed-класса) */
fun sealedSubclasses(kmClass: KmClass): List<String> =
    kmClass.sealedSubclasses