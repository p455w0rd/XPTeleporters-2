package latmod.xpt.client;

import latmod.xpt.init.ModGlobals;
import latmod.xpt.init.ModObjects;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(modid = ModGlobals.MOD_ID)
public class ModEventsClient {

	@SubscribeEvent
	public static void onModelRegistryReady(final ModelRegistryEvent e) {
		ModObjects.registerModels();
	}

}
