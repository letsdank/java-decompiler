package net.letsdank.jd.ast;

/**
 * Настройки декомпилятора, которые можно менять через GUI.
 */
public final class DecompilerOptions {

    /**
     * Прятать ли Kotlin runtime-шумиху:
     * Intrinsics.checkNotNullParameter,
     * DefaultConstructorMarker, copy$default, и т.п.
     */
    private boolean hideKotlinIntrinsics = true;

    private boolean useKotlinxMetadata = true;

    public boolean hideKotlinIntrinsics() {
        return hideKotlinIntrinsics;
    }

    public boolean useKotlinxMetadata() {
        return useKotlinxMetadata;
    }

    public void setHideKotlinIntrinsics(boolean hideKotlinIntrinsics) {
        this.hideKotlinIntrinsics = hideKotlinIntrinsics;
    }

    public void setUseKotlinxMetadata(boolean useKotlinxMetadata) {
        this.useKotlinxMetadata = useKotlinxMetadata;
    }

    // сюда позже можно добавить:
    // - showSyntheticMethods
    // - preferKotlinOverJava
    // - showBytecode
    // и т.п.
}
