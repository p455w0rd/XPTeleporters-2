package latmod.xpt;

import latmod.xpt.init.ModConfig;
import latmod.xpt.init.ModGlobals;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ModGlobals.MOD_ID, name = ModGlobals.MOD_NAME, version = ModGlobals.MOD_VERSION)
public class XPT {

	@Instance(ModGlobals.MOD_ID)
	public static XPT inst;

	@EventHandler
	public void preInit(final FMLPreInitializationEvent e) {
		ModConfig.init();
	}

}