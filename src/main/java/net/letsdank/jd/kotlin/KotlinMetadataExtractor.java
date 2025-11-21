package net.letsdank.jd.kotlin;

import kotlin.Metadata;
import kotlin.metadata.KmClass;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.jvm.KotlinClassMetadata;
import net.letsdank.jd.model.ClassFile;
import net.letsdank.jd.model.annotation.AnnotationInfo;
import net.letsdank.jd.model.annotation.KotlinMetadataAnnotationInfo;
import net.letsdank.jd.model.attribute.AttributeInfo;
import net.letsdank.jd.model.attribute.RuntimeVisibleAnnotationsAttribute;

import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * Утилита для чтения Kotlin-метаданных через kotlin-metadata-jvm.
 * <p>
 * Это первый шаг: работаем через reflection (Class<?>),
 * чтобы просто увидеть, что лежит в @Metadata.
 * Потом можно будет перейти к чтению метаданных из собственного ClassFile.
 */
public final class KotlinMetadataExtractor {
    private KotlinMetadataExtractor() {
    }

    public static void printClassSummary(Class<?> clazz, Appendable out) throws IOException {
        out.append("=== Kotlin metadata for ").append(clazz.getName()).append(" ===\n");

        Metadata metadataAnn = clazz.getAnnotation(Metadata.class);
        if (metadataAnn == null) {
            out.append("No @kotlin.Metadata annotation\n\n");
            return;
        }

        KotlinClassMetadata metadata = KotlinClassMetadata.Companion.readLenient(metadataAnn);
        if (metadata == null) {
            out.append("Failed to parse Kotlin Metadata (readLenient returned null)");
            return;
        }

        if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            printKmClass(classMeta.getKmClass(), out);
        } else if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            printKmPackage(fileFacadeMeta.getKmPackage(), out);
        } else if (metadata instanceof KotlinClassMetadata.SyntheticClass syn) {
            out.append("Synthetic Kotlin class, kind=").append(String.valueOf(syn.getFlags()))
                    .append("\n\n");
        } else if (metadata instanceof KotlinClassMetadata.MultiFileClassFacade facade) {
            out.append("Multi-file facade (no detailed view yet)\n\n");
        } else if (metadata instanceof KotlinClassMetadata.MultiFileClassPart part) {
            out.append("Multi-file part, facade=").append(part.getFacadeClassName())
                    .append("\n\n");
        } else {
            out.append("Unsupported KotlinClassMetadata subclass: ")
                    .append(metadata.getClass().getName()).append("\n\n");
        }

        out.append("\n");
    }

    private static void printKmClass(KmClass kmClass, Appendable out) throws IOException {
        out.append("Kotlin class: ").append(kmClass.getName()).append("\n");

        if (!kmClass.getProperties().isEmpty()) {
            out.append("  Properties:\n");
            for (KmProperty p : kmClass.getProperties()) {
                out.append("    - ").append(p.getName()).append("\n");
            }
        }

        if (!kmClass.getFunctions().isEmpty()) {
            out.append("  Functions:\n");
            for (KmFunction f : kmClass.getFunctions()) {
                out.append("    - ").append(f.getName()).append("\n");
            }
        }
    }

    private static void printKmPackage(KmPackage kmPackage, Appendable out) throws IOException {
        out.append("Kotlin file facade (package fragment)\n");

        if (!kmPackage.getProperties().isEmpty()) {
            out.append("  Top-level properties:\n");
            for (KmProperty p : kmPackage.getProperties()) {
                out.append("    - ").append(p.getName()).append("\n");
            }
        }

        if (!kmPackage.getFunctions().isEmpty()) {
            out.append("  Top-level functions:\n");
            for (KmFunction f : kmPackage.getFunctions()) {
                out.append("    - ").append(f.getName()).append("\n");
            }
        }
    }

    /**
     * Извлечь KotlinClassMetadata напрямую из нашего ClassFile.
     * Возвращает null, если @kotlin.Metadata не найден или не парсится.
     */
    public static KotlinClassMetadata extractFromClassFile(ClassFile cf) {
        if (cf.attributes() == null) return null;
        KotlinMetadataAnnotationInfo metaInfo = null;

        for (AttributeInfo attr : cf.attributes()) {
            if (attr instanceof RuntimeVisibleAnnotationsAttribute annsAttr) {
                for (AnnotationInfo ann : annsAttr.annotations()) {
                    if (ann instanceof KotlinMetadataAnnotationInfo km) {
                        metaInfo = km;
                        break;
                    }
                }
            }
            if (metaInfo != null) break;
        }

        if (metaInfo == null) return null;

        // Собираем kotlin.Metadata из наших полей
        int kind = metaInfo.kind();
        int[] mv = metaInfo.metadataVersion();
        int[] bv = metaInfo.bytecodeVersion();
        String[] d1 = metaInfo.data1();
        String[] d2 = metaInfo.data2();
        String extraString = metaInfo.extraString();
        int extraInt = metaInfo.extraInt();

        Metadata metadata = new Metadata() {
            @Override
            public int k() {
                return kind;
            }

            @Override
            public int[] mv() {
                return mv != null ? mv : new int[0];
            }

            @Override
            public int[] bv() {
                return bv != null ? bv : new int[0];
            }

            @Override
            public String[] d1() {
                return d1 != null ? d1 : new String[0];
            }

            @Override
            public String[] d2() {
                return d2 != null ? d2 : new String[0];
            }

            @Override
            public String xs() {
                return extraString != null ? extraString : "";
            }

            @Override
            public int xi() {
                return extraInt;
            }

            @Override
            public String pn() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Metadata.class;
            }
        };

        return KotlinClassMetadata.Companion.readLenient(metadata);
    }

    public static void printClassSummary(ClassFile cf, Appendable out) throws IOException {
        out.append("=== Kotlin metadata for ").append(cf.thisClassFqn()).append(" ===\n");
        KotlinClassMetadata metadata = extractFromClassFile(cf);
        if (metadata == null) {
            out.append("No @kotlin.Metadata or failed to parse\n\n");
            return;
        }

        if (metadata instanceof KotlinClassMetadata.Class classMeta) {
            printKmClass(classMeta.getKmClass(), out);
        } else if (metadata instanceof KotlinClassMetadata.FileFacade fileFacadeMeta) {
            printKmPackage(fileFacadeMeta.getKmPackage(), out);
        } else if (metadata instanceof KotlinClassMetadata.SyntheticClass syn) {
            out.append("Synthetic Kotlin class, kind=").append(String.valueOf(syn.getFlags()))
                    .append("\n\n");
        } else if (metadata instanceof KotlinClassMetadata.MultiFileClassFacade facade) {
            out.append("Multi-file facade (no detailed view yet)\n\n");
        } else if (metadata instanceof KotlinClassMetadata.MultiFileClassPart part) {
            out.append("Multi-file part, facade=").append(part.getFacadeClassName())
                    .append("\n\n");
        } else {
            out.append("Unsupported KotlinClassMetadata subclass: ")
                    .append(metadata.getClass().getName()).append("\n\n");
        }

        out.append("\n");
    }
}
