package net.letsdank.jd.kotlin.metadata;

public record KotlinMetadata(Kind kind,
                             int[] metadataVersion, int[] bytecodeVersion,
                             String[] data1, String[] data2,
                             String extraString, String packageName,
                             int extraInt) {
    public enum Kind {
        CLASS(1),
        FILE_FACADE(2),
        SYNTHETIC_CLASS(3),
        MULTIFILE_CLASS_FACADE(4),
        MULTIFILE_CLASS_PART(5),
        UNKNOWN(-1);

        private final int id;

        Kind(int id) {
            this.id = id;
        }

        public static Kind fromId(int id) {
            for (Kind k : values()) {
                if (k.id == id) return k;
            }
            return UNKNOWN;
        }

        public int getId() {
            return id;
        }
    }
}
