package cz.dmuzik.plasticoretoggle;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * MedicineCraft has no config and registers one world generator that only places Plastic Ore. FML has no API to
 * unregister a generator, so it is taken out of GameRegistry's collections after all mods have registered theirs.
 */
@Mod(
    modid = "plasticoretoggle",
    name = "Plastic Ore Toggle",
    version = "1.0.0",
    dependencies = "after:MedicineCraft",
    acceptableRemoteVersions = "*")
public class PlasticOreToggle {

    private static final Logger LOG = LogManager.getLogger("PlasticOreToggle");
    private static final String GENERATOR_CLASS = "net.medicmod.world.PlasticOreGen";

    private boolean generatePlasticOre;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        generatePlasticOre = config.getBoolean(
            "generatePlasticOre",
            Configuration.CATEGORY_GENERAL,
            false,
            "Generovat Plastic Ore z MedicineCraftu v nově vznikajících chuncích. Už vygenerovaný svět se tím nemění.");
        if (config.hasChanged()) {
            config.save();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (generatePlasticOre) {
            LOG.info("generatePlasticOre=true: Plastic Ore generation stays on");
            return;
        }
        if (!Loader.isModLoaded("MedicineCraft")) {
            LOG.info("MedicineCraft is not installed, nothing to do");
            return;
        }
        try {
            int removed = removeGenerators();
            if (removed == 1) {
                LOG.info("Plastic Ore generation is off");
            } else {
                LOG.error("Expected one generator of type {} but removed {}", GENERATOR_CLASS, removed);
            }
        } catch (Exception e) {
            LOG.error("Could not turn off Plastic Ore generation", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static int removeGenerators() throws Exception {
        Set<IWorldGenerator> generators = (Set<IWorldGenerator>) field("worldGenerators").get(null);
        Map<IWorldGenerator, Integer> weights = (Map<IWorldGenerator, Integer>) field("worldGeneratorIndex").get(null);

        int removed = 0;
        for (Iterator<IWorldGenerator> it = generators.iterator(); it.hasNext();) {
            IWorldGenerator generator = it.next();
            if (GENERATOR_CLASS.equals(generator.getClass().getName())) {
                it.remove();
                weights.remove(generator);
                removed++;
            }
        }
        // Rebuilt from the set on the next generated chunk
        field("sortedGeneratorList").set(null, null);
        return removed;
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GameRegistry.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
