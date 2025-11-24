package net.letsdank.jd.kotlin

import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeProjection
import kotlin.metadata.KmVariance

/**
 * Стабильный принтер Kotlin-типов поверх KmType.
 * Java код использует его через KotlinTypePrinterKt.renderKotlinType(type).
 */
fun renderKotlinType(type: KmType): String {
    // Попытка распознать function type / suspend function type
    renderFunctionType(type)?.let { return it }

    val classifier = type.classifier
    val baseName = when (classifier) {
        is KmClassifier.Class -> classifier.name.replace('/', '.').replace('$', '.')
        is KmClassifier.TypeParameter -> "T${classifier.id}"
        is KmClassifier.TypeAlias -> classifier.name.replace('/', '.').replace('$', '.')
        else -> "Any?"
    }

    // Outer type для inner-классов: A<B>.C<D>
    val outer = type.outerType
    val qualifiedBase = if (outer != null) {
        val outerRendered = renderKotlinType(outer)
        "$outerRendered.${simpleNameFromInternal(baseName)}"
    } else {
        baseName
    }

    // Generic аргументы
    val args = type.arguments
    val withArgs = if (args.isNotEmpty()) {
        val renderedArgs = args.map { renderProjection(it) }
        "$qualifiedBase<${renderedArgs.joinToString(", ")}>"
    } else {
        qualifiedBase
    }

    // Nullable / not-null по фасаду MetadataFlags
    val nullable = isNullable(type)
    val result = if (nullable) "$withArgs?" else withArgs

    // Raw type / прочие флаги можно добавить позже, пока так сойдет
    return result
}

private fun simpleNameFromInternal(fqn: String): String {
    val idx = fqn.lastIndexOf('.')
    return if (idx >= 0 && idx + 1 < fqn.length) fqn.substring(idx + 1) else fqn
}

private fun renderProjection(p: KmTypeProjection): String {
    val t = p.type ?: return "*"
    val rendered = renderKotlinType(t)

    return when (p.variance) {
        null -> rendered // invariant
        KmVariance.IN -> "in $rendered"
        KmVariance.OUT -> "out $rendered"
        KmVariance.INVARIANT -> rendered // invariant
    }
}

/**
 * Распознаем function types вида (A, B) -> C и suspend (A) -> B
 */
private fun renderFunctionType(type: KmType): String? {
    val classifier = type.classifier as? KmClassifier.Class ?: return null
    val internalName = classifier.name  // например kotlin/Function2

    val isFunction =
        internalName.startsWith("kotlin/Function") ||
                internalName.startsWith("kotlin/reflect/KFunction")

    if (!isFunction) return null

    // Аргументы FunctionN: все KmTypeProjection, у последнего - return type
    val args = type.arguments.mapNotNull { it.type }
    if (args.isEmpty()) return null

    val paramTypes = args.dropLast(1).map { renderKotlinType(it) }
    val returnType = renderKotlinType(args.last())

    val suspendPrefix = if (isSuspend(type)) "suspend " else ""
    val paramsPart = paramTypes.joinToString(", ")

    return buildString {
        append(suspendPrefix)
        append("(")
        append(paramsPart)
        append(") -> ")
        append(returnType)
    }
}
