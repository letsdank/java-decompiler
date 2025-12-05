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

    /**
     * Прятать ли вызовы статических методов, имя которых начинается на '$'.
     * Например: $$$reportNull$$$0(...)
     */
    private boolean hideDollarMethods = true;

    public boolean hideKotlinIntrinsics() {
        return hideKotlinIntrinsics;
    }

    public boolean useKotlinxMetadata() {
        return useKotlinxMetadata;
    }

    public boolean hideDollarMethods() {
        return hideKotlinIntrinsics;
    }

    public void setHideKotlinIntrinsics(boolean hideKotlinIntrinsics) {
        this.hideKotlinIntrinsics = hideKotlinIntrinsics;
    }

    public void setUseKotlinxMetadata(boolean useKotlinxMetadata) {
        this.useKotlinxMetadata = useKotlinxMetadata;
    }

    public void setHideDollarMethods(boolean hideDollarMethods) {
        this.hideDollarMethods = hideDollarMethods;
    }

    // сюда позже можно добавить:
    // - showSyntheticMethods
    // - preferKotlinOverJava
    // - showBytecode
    // и т.п.
}
