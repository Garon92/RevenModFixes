package cz.dmuzik.aethervoid;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "aethervoid", name = "Aether Void", version = "1.0.0", acceptableRemoteVersions = "*")
public class AetherVoid {

    private static final Logger LOG = LogManager.getLogger("AetherVoid");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        AetherVoidHooks.playersFallIntoVoid = config.getBoolean(
            "playersFallIntoVoid",
            Configuration.CATEGORY_GENERAL,
            true,
            "Hráč, který spadne z Aetheru, padá do voidu a zemře (true), místo aby ho to přeneslo do Overworldu (false).");
        AetherVoidHooks.mobsFallIntoVoid = config.getBoolean(
            "mobsFallIntoVoid",
            Configuration.CATEGORY_GENERAL,
            true,
            "Totéž pro moby a osedlané mounty (true = spadnou do voidu, false = přenese je to do Overworldu).");
        if (config.hasChanged()) {
            config.save();
        }
        LOG.info(
            "Falling out of the Aether: players into the {}, mobs into the {}",
            AetherVoidHooks.playersFallIntoVoid ? "void" : "Overworld",
            AetherVoidHooks.mobsFallIntoVoid ? "void" : "Overworld");
    }
}
