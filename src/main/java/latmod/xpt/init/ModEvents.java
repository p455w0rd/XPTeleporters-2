package latmod.xpt.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by p455w0rd
 */
@EventBusSubscriber(modid = ModGlobals.MOD_ID)
public class ModEvents {

	@SubscribeEvent
	public static void onConfigChange(final ConfigChangedEvent.OnConfigChangedEvent e) {
		if (e.getModID().equals(ModGlobals.MOD_ID)) {
			ModConfig.init();
		}
	}

	@SubscribeEvent
	public static void onBlockRegistryReady(final RegistryEvent.Register<Block> e) {
		ModObjects.registerBlock(e);
	}

	@SubscribeEvent
	public static void onItemRegistryReady(final RegistryEvent.Register<Item> e) {
		ModObjects.registerItem(e);
	}

}
