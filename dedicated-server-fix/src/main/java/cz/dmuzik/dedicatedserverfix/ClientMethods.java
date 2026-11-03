package cz.dmuzik.dedicatedserverfix;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/** Bodies of the restored client-only methods, the same as in Minecraft's client code. */
public final class ClientMethods {

    private ClientMethods() {}

    public static boolean isBlockNormalCube(Block block) {
        return block.getMaterial().blocksMovement() && block.renderAsNormalBlock();
    }

    public static int itemInUseDuration(ItemStack itemInUse, int itemInUseCount) {
        return itemInUse != null ? itemInUse.getMaxItemUseDuration() - itemInUseCount : 0;
    }

    public static float swingProgress(EntityLivingBase entity, float partialTicks) {
        float swing = entity.swingProgress - entity.prevSwingProgress;
        if (swing < 0.0F) {
            swing++;
        }
        return entity.prevSwingProgress + swing * partialTicks;
    }

    public static float cloudHeight() {
        return 128.0F;
    }
}
