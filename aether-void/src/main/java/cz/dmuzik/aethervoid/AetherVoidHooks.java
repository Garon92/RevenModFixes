package cz.dmuzik.aethervoid;

/**
 * Called from the patched Aether code in place of its fall-out constants.
 * Negative infinity means "never": no posY is below it.
 */
public final class AetherVoidHooks {

    private static final double NEVER = Double.NEGATIVE_INFINITY;

    static boolean playersFallIntoVoid = true;
    static boolean mobsFallIntoVoid = true;

    private AetherVoidHooks() {}

    public static double playerFallLimit() {
        return playersFallIntoVoid ? NEVER : -12.0D;
    }

    public static double mobFallLimit() {
        return mobsFallIntoVoid ? NEVER : -2.0D;
    }

    public static double mountFallLimit() {
        return mobsFallIntoVoid ? NEVER : 0.0D;
    }
}
