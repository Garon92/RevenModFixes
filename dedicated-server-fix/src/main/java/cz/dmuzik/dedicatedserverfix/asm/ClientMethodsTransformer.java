package cz.dmuzik.dedicatedserverfix.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Minecraft's client-only methods do not exist on a dedicated server. A mod calling one from common code works in
 * singleplayer and on LAN, and crashes a dedicated server with NoSuchMethodError. This puts back the ones that mods
 * in the pack reach on the server. Names are SRG; each method is added only if missing. The client is left alone.
 */
public class ClientMethodsTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("DedicatedServerFix");
    private static final String HELPER = "cz/dmuzik/dedicatedserverfix/ClientMethods";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || FMLLaunchHandler.side() != Side.SERVER) {
            return basicClass;
        }
        if ("net.minecraft.block.Block".equals(transformedName)) {
            // Block.isBlockNormalCube: Glacia crystals, portal and doors
            MethodNode m = method("func_149637_q", "()Z", 1, 1);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(helper("isBlockNormalCube", "(Lnet/minecraft/block/Block;)Z"));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            return add(basicClass, transformedName, m);
        }
        if ("net.minecraft.entity.player.EntityPlayer".equals(transformedName)) {
            // EntityPlayer.getItemInUseDuration: Betweenlands, blocking a Wight's soul
            MethodNode m = method("func_71057_bx", "()I", 2, 1);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(
                new FieldInsnNode(
                    Opcodes.GETFIELD,
                    "net/minecraft/entity/player/EntityPlayer",
                    "field_71074_e",
                    "Lnet/minecraft/item/ItemStack;"));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(
                new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/player/EntityPlayer", "field_71072_f", "I"));
            m.instructions.add(helper("itemInUseDuration", "(Lnet/minecraft/item/ItemStack;I)I"));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            return add(basicClass, transformedName, m);
        }
        if ("net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
            // EntityLivingBase.getSwingProgress: Betweenlands, Swiftarm elixir
            MethodNode m = method("func_70678_g", "(F)F", 2, 2);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.FLOAD, 1));
            m.instructions.add(helper("swingProgress", "(Lnet/minecraft/entity/EntityLivingBase;F)F"));
            m.instructions.add(new InsnNode(Opcodes.FRETURN));
            return add(basicClass, transformedName, m);
        }
        if ("net.minecraft.util.FoodStats".equals(transformedName)) {
            // FoodStats.setFoodLevel: Betweenlands, Starvation elixir
            MethodNode m = method("func_75114_a", "(I)V", 2, 2);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            m.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/util/FoodStats", "field_75127_a", "I"));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            return add(basicClass, transformedName, m);
        }
        if ("net.minecraft.world.WorldProvider".equals(transformedName)) {
            // WorldProvider.getCloudHeight: Twilight Forest 2.4.3, Magic Beans
            MethodNode m = method("func_76571_f", "()F", 1, 1);
            m.instructions.add(helper("cloudHeight", "()F"));
            m.instructions.add(new InsnNode(Opcodes.FRETURN));
            return add(basicClass, transformedName, m);
        }
        return basicClass;
    }

    private static MethodNode method(String name, String desc, int maxStack, int maxLocals) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
        m.maxStack = maxStack;
        m.maxLocals = maxLocals;
        return m;
    }

    private static MethodInsnNode helper(String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, name, desc, false);
    }

    private static byte[] add(byte[] bytes, String className, MethodNode added) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);
        for (MethodNode existing : classNode.methods) {
            if (existing.name.equals(added.name) && existing.desc.equals(added.desc)) {
                return bytes;
            }
        }
        classNode.methods.add(added);
        // No jumps in the added code and the others stay as they were, so nothing needs recomputing
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        LOG.info("Added missing {}.{}{}", className, added.name, added.desc);
        return writer.toByteArray();
    }
}
