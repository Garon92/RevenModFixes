package cz.dmuzik.rmftest;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameData;

/**
 * Test-only helper, never shipped. /rmf commands print to the server log as "RMF ..." lines for the test scripts:
 * mods, dims, where, move (hand-written transfer to a dimension), shaft (clears a column so nothing is in the way
 * of a fall), place, spawn (pig/item), tracked (what happened to the last spawned entity), mount, count (generates
 * chunks around a point and counts one block), items (registry names matching a filter), useitem (uses an item on
 * a block like a player would), tickblock (forces random ticks on a block), sidecheck (calls the client-only methods
 * DedicatedServerFix restores).
 */
@Mod(modid = "rmftest", name = "RevenModFixes Test Helper", version = "1.0.0", acceptableRemoteVersions = "*")
public class RmfTestMod {

    static final Logger LOG = LogManager.getLogger("RMF");
    static Entity tracked;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {

            @Override
            public String getCommandName() {
                return "rmf";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/rmf <mods|dims|where|move|shaft|place|spawn|tracked|mount|count|items|useitem|tickblock|sidecheck> ...";
            }

            @Override
            public int getRequiredPermissionLevel() {
                return 2;
            }

            @Override
            public void processCommand(ICommandSender sender, String[] a) {
                if (a.length == 0) {
                    throw new WrongUsageException(getCommandUsage(sender));
                }
                run(sender, a);
            }

            private void run(ICommandSender s, String[] a) {
                String sub = a[0];
                if ("mods".equals(sub)) {
                    StringBuilder sb = new StringBuilder();
                    for (ModContainer mod : Loader.instance().getActiveModList()) {
                        sb.append(mod.getModId()).append('=').append(mod.getVersion()).append('|');
                    }
                    LOG.info("RMF mods {}", sb);
                } else if ("dims".equals(sub)) {
                    for (int id : DimensionManager.getStaticDimensionIDs()) {
                        LOG.info("RMF dim {} {}", id, DimensionManager.createProviderFor(id).getDimensionName());
                    }
                } else if ("where".equals(sub)) {
                    EntityPlayerMP p = getPlayer(s, a[1]);
                    LOG.info(
                        "RMF where {} dim={} x={} y={} z={} hp={}",
                        p.getCommandSenderName(),
                        p.dimension,
                        String.format("%.2f", p.posX),
                        String.format("%.2f", p.posY),
                        String.format("%.2f", p.posZ),
                        p.getHealth());
                } else if ("move".equals(sub)) {
                    move(
                        getPlayer(s, a[1]),
                        parseInt(s, a[2]),
                        Double.parseDouble(a[3]),
                        Double.parseDouble(a[4]),
                        Double.parseDouble(a[5]));
                } else if ("shaft".equals(sub)) {
                    shaft(parseInt(s, a[1]), parseInt(s, a[2]), parseInt(s, a[3]));
                } else if ("place".equals(sub)) {
                    Block block = GameData.getBlockRegistry().getObject(a[5]);
                    world(parseInt(s, a[1])).setBlock(parseInt(s, a[2]), parseInt(s, a[3]), parseInt(s, a[4]), block, 0, 2);
                    LOG.info("RMF placed {}", a[5]);
                } else if ("spawn".equals(sub)) {
                    spawn(a[1], parseInt(s, a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]), Double.parseDouble(a[5]));
                } else if ("tracked".equals(sub)) {
                    LOG.info(
                        "RMF tracked dim={} y={} dead={}",
                        tracked.dimension,
                        String.format("%.2f", tracked.posY),
                        tracked.isDead);
                } else if ("mount".equals(sub)) {
                    getPlayer(s, a[1]).mountEntity(tracked);
                    LOG.info("RMF mounted {}", a[1]);
                } else if ("items".equals(sub)) {
                    for (Object key : GameData.getItemRegistry().getKeys()) {
                        if (((String) key).toLowerCase().contains(a[1])) {
                            LOG.info("RMF item {}", key);
                        }
                    }
                } else if ("useitem".equals(sub)) {
                    EntityPlayerMP p = getPlayer(s, a[1]);
                    ItemStack stack = new ItemStack(GameData.getItemRegistry().getObject(a[2]));
                    boolean used = stack.tryPlaceItemIntoWorld(p, p.worldObj, parseInt(s, a[3]), parseInt(s, a[4]), parseInt(s, a[5]), 1, 0.5F, 0.5F, 0.5F);
                    LOG.info("RMF used {} result={}", a[2], used);
                } else if ("tickblock".equals(sub)) {
                    WorldServer w = world(parseInt(s, a[1]));
                    int x = parseInt(s, a[2]);
                    int y = parseInt(s, a[3]);
                    int z = parseInt(s, a[4]);
                    int times = parseInt(s, a[5]);
                    java.util.Random random = new java.util.Random(1);
                    for (int i = 0; i < times; i++) {
                        w.getBlock(x, y, z).updateTick(w, x, y, z, random);
                    }
                    LOG.info("RMF ticked {} times", times);
                } else if ("sidecheck".equals(sub)) {
                    sideCheck(getPlayer(s, a[1]));
                } else if ("count".equals(sub)) {
                    count(a[1], parseInt(s, a[2]), parseInt(s, a[3]), parseInt(s, a[4]));
                } else {
                    throw new WrongUsageException(getCommandUsage(s));
                }
            }
        });
    }

    static WorldServer world(int dim) {
        return MinecraftServer.getServer().worldServerForDimension(dim);
    }

    /** Reflection, because these methods are not supposed to exist on a server and the names are SRG. */
    static void sideCheck(EntityPlayerMP p) {
        try {
            Object stone = net.minecraft.block.Block.class.getMethod("func_149637_q").invoke(Blocks.stone);
            Object glass = net.minecraft.block.Block.class.getMethod("func_149637_q").invoke(Blocks.glass);
            Object duration = net.minecraft.entity.player.EntityPlayer.class.getMethod("func_71057_bx").invoke(p);
            Object swing = net.minecraft.entity.EntityLivingBase.class.getMethod("func_70678_g", float.class).invoke(p, 0.5F);
            int food = p.getFoodStats().getFoodLevel();
            net.minecraft.util.FoodStats.class.getMethod("func_75114_a", int.class).invoke(p.getFoodStats(), 7);
            int newFood = p.getFoodStats().getFoodLevel();
            net.minecraft.util.FoodStats.class.getMethod("func_75114_a", int.class).invoke(p.getFoodStats(), food);
            Object cloud = net.minecraft.world.WorldProvider.class.getMethod("func_76571_f").invoke(p.worldObj.provider);
            LOG.info("RMF sidecheck ok stone={} glass={} duration={} swing={} food={} cloud={}", stone, glass, duration, swing, newFood, cloud);
        } catch (NoSuchMethodException e) {
            LOG.info("RMF sidecheck missing {}", e.getMessage());
        } catch (Exception e) {
            LOG.info("RMF sidecheck error {}", e);
        }
    }

    /** Moves a player without any mod's teleporter, so the test decides exactly where they end up. */
    static void move(EntityPlayerMP p, int dim, double x, double y, double z) {
        ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
        WorldServer oldWorld = (WorldServer) p.worldObj;
        WorldServer newWorld = world(dim);
        int oldDim = p.dimension;
        p.dimension = dim;
        p.playerNetServerHandler.sendPacket(
            new S07PacketRespawn(
                dim,
                newWorld.difficultySetting,
                newWorld.getWorldInfo().getTerrainType(),
                p.theItemInWorldManager.getGameType()));
        oldWorld.removePlayerEntityDangerously(p);
        p.isDead = false;
        newWorld.theChunkProviderServer.loadChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        p.setLocationAndAngles(x, y, z, p.rotationYaw, p.rotationPitch);
        newWorld.spawnEntityInWorld(p);
        newWorld.updateEntityWithOptionalForce(p, false);
        p.setWorld(newWorld);
        scm.func_72375_a(p, oldWorld);
        p.playerNetServerHandler.setPlayerLocation(x, y, z, p.rotationYaw, p.rotationPitch);
        p.theItemInWorldManager.setWorld(newWorld);
        scm.updateTimeAndWeatherForPlayer(p, newWorld);
        scm.syncPlayerInventory(p);
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(p, oldDim, dim);
        LOG.info("RMF moved {} to {}", p.getCommandSenderName(), dim);
    }

    /** Clears a 5x5 column, so a fall cannot land on an island. */
    static void shaft(int dim, int x, int z) {
        WorldServer w = world(dim);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                w.theChunkProviderServer.loadChunk((x + dx) >> 4, (z + dz) >> 4);
                for (int y = 0; y < 256; y++) {
                    if (!w.isAirBlock(x + dx, y, z + dz)) {
                        w.setBlock(x + dx, y, z + dz, Blocks.air, 0, 2);
                    }
                }
            }
        }
        LOG.info("RMF shaft cleared");
    }

    static void spawn(String what, int dim, double x, double y, double z) {
        WorldServer w = world(dim);
        Entity entity;
        if ("pig".equals(what)) {
            entity = new EntityPig(w);
        } else {
            EntityItem item = new EntityItem(w, x, y, z, new ItemStack(Items.diamond));
            item.lifespan = 72000;
            entity = item;
        }
        entity.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        w.spawnEntityInWorld(entity);
        tracked = entity;
        LOG.info("RMF spawned {}", what);
    }

    /** Generates (2r+1)^2 chunks around a chunk position, then counts the block in those fully populated. */
    static void count(String blockName, int cx, int cz, int r) {
        WorldServer w = world(0);
        Block block = GameData.getBlockRegistry().getObject(blockName);
        for (int x = cx - r - 1; x <= cx + r + 1; x++) {
            for (int z = cz - r - 1; z <= cz + r + 1; z++) {
                w.theChunkProviderServer.loadChunk(x, z);
            }
        }
        long found = 0;
        for (int x = (cx - r) * 16; x < (cx + r + 1) * 16; x++) {
            for (int z = (cz - r) * 16; z < (cz + r + 1) * 16; z++) {
                for (int y = 1; y < 80; y++) {
                    if (w.getBlock(x, y, z) == block) {
                        found++;
                    }
                }
            }
        }
        LOG.info("RMF count {} = {}", blockName, found);
    }
}
