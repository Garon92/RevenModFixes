package cz.dmuzik.aethervoid.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Aether (Legacy) moves whatever falls out of the bottom of its dimension to the Overworld, in three places.
 * Each one compares the entity's posY with a constant; the constant is replaced by a call that returns either the
 * original value or negative infinity, which makes the comparison never pass:
 * <ul>
 * <li>{@code PlayerAether.onUpdate}: {@code posY < -12} teleports the player,</li>
 * <li>{@code EntityHook.onUpdate}: {@code posY < -2} teleports mobs,</li>
 * <li>{@code AetherEntityEvents.entityUpdateEvents}: {@code posY <= 0} moves a ridden mount.</li>
 * </ul>
 * With the check gone, vanilla's own rule applies: below y=-64 an entity takes void damage.
 */
public class AetherVoidTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("AetherVoid");

    private static final String HOOKS = "cz/dmuzik/aethervoid/AetherVoidHooks";

    private static final String PLAYER_AETHER = "com.gildedgames.the_aether.player.PlayerAether";
    private static final String ENTITY_HOOK = "com.gildedgames.the_aether.entities.util.EntityHook";
    private static final String ENTITY_EVENTS = "com.gildedgames.the_aether.events.AetherEntityEvents";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (PLAYER_AETHER.equals(transformedName)) {
            return patch(basicClass, transformedName, "onUpdate", "()V", -12.0D, "playerFallLimit");
        }
        if (ENTITY_HOOK.equals(transformedName)) {
            return patch(basicClass, transformedName, "onUpdate", "()V", -2.0D, "mobFallLimit");
        }
        if (ENTITY_EVENTS.equals(transformedName)) {
            return patch(
                basicClass,
                transformedName,
                "entityUpdateEvents",
                "(Lnet/minecraft/entity/Entity;)V",
                0.0D,
                "mountFallLimit");
        }
        return basicClass;
    }

    private byte[] patch(byte[] bytes, String className, String methodName, String desc, double limit, String hook) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);

        int patched = 0;
        for (MethodNode method : classNode.methods) {
            if (!methodName.equals(method.name) || !desc.equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
                if (isConstant(node, limit) && isPosY(node.getPrevious())) {
                    AbstractInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, "()D", false);
                    method.instructions.set(node, call);
                    node = call;
                    patched++;
                }
            }
        }

        if (patched != 1) {
            // Not the version this was written for: leave the class alone rather than guess
            LOG.error(
                "Expected one 'posY vs {}' check in {}.{} but found {} - this part of the Aether stays unchanged",
                limit,
                className,
                methodName,
                patched);
            return bytes;
        }

        // A constant is swapped for a call with the same stack effect, so frames and maxs stay valid
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        LOG.info("Patched the fall-out check in {}.{}", className, methodName);
        return writer.toByteArray();
    }

    private static boolean isConstant(AbstractInsnNode node, double limit) {
        if (node instanceof LdcInsnNode) {
            Object constant = ((LdcInsnNode) node).cst;
            return constant instanceof Double && (Double) constant == limit;
        }
        return limit == 0.0D && node instanceof InsnNode && node.getOpcode() == Opcodes.DCONST_0;
    }

    private static boolean isPosY(AbstractInsnNode node) {
        if (!(node instanceof FieldInsnNode) || node.getOpcode() != Opcodes.GETFIELD) {
            return false;
        }
        String field = ((FieldInsnNode) node).name;
        return "field_70163_u".equals(field) || "posY".equals(field);
    }
}
