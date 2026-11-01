package cz.dmuzik.aethervoid.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/** Coremod entry point: the Aether's fall-out checks can only be changed in its bytecode. */
@IFMLLoadingPlugin.Name("AetherVoidCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions("cz.dmuzik.aethervoid.asm")
public class AetherVoidPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { AetherVoidTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
