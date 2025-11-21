package net.letsdank.jd.kotlin;

import net.letsdank.jd.fixtures.SampleService;

public final class KotlinMetadataDemo {
    public static void main(String[] args) throws Exception {
        KotlinMetadataExtractor.printClassSummary(SampleService.class, System.out);

        Class<?> fileFacade = Class.forName("net.letsdank.jd.fixtures.SampleKotlinKt");
        KotlinMetadataExtractor.printClassSummary(fileFacade, System.out);
    }
}
