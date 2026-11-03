package cz.dmuzik.dedicatedserverfix;

import net.minecraft.tileentity.TileEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = "dedicatedserverfix",
    name = "Dedicated Server Fix",
    version = "1.0.0",
    dependencies = "after:Glacia",
    acceptableRemoteVersions = "*")
public class DedicatedServerFix {

    private static final Logger LOG = LogManager.getLogger("DedicatedServerFix");

    /**
     * Glacia registers these two empty tile entities only in its client proxy. On a server each one fails on every
     * world save with "is missing a mapping", thousands of log lines at a time.
     */
    private static final String[] GLACIA_TILE_ENTITIES = { "TileEntityGlaciaCrystalOfPower", "TileEntityGlacialBerry" };

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (!FMLCommonHandler.instance().getSide().isServer()) {
            return;
        }
        for (String name : GLACIA_TILE_ENTITIES) {
            try {
                GameRegistry.registerTileEntity(Class.forName("net.minecraft.src." + name).asSubclass(TileEntity.class), name);
                LOG.info("Registered Glacia's {}", name);
            } catch (ClassNotFoundException e) {
                LOG.debug("{} not found, Glacia is not installed", name);
            }
        }
    }
}
