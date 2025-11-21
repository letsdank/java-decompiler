package net.letsdank.jd.kotlin;

import kotlin.metadata.KmClass;
import kotlin.metadata.KmPackage;

public interface KotlinMetadataProvider {
    KmClass getKmClass();
    KmPackage getKmPackage();
}
