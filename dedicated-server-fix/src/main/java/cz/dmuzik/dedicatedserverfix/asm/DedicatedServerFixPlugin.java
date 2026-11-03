package cz.dmuzik.dedicatedserverfix.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Coremod entry point. Above sorting index 1000 method names are SRG and FML has already stripped the client-only
 * members, so "is the method there" gives the real answer for this side.
 */
@IFMLLoadingPlugin.Name("DedicatedServerFixCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions("cz.dmuzik.dedicatedserverfix.asm")
public class DedicatedServerFixPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { ClientMethodsTransformer.class.getName() };
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
