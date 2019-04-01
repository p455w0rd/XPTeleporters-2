package latmod.xpt.init;

import java.util.List;

import javax.annotation.Nullable;

import latmod.xpt.block.BlockTeleporter;
import latmod.xpt.item.ItemLinkCard;
import latmod.xpt.util.IModelRegister;
import latmod.xpt.util.XPTUtils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
@SuppressWarnings("deprecation")
public class ModObjects {

	public static final BlockTeleporter TELEPORTER = new BlockTeleporter();
	public static final Item TELEPORTER_ITEM = new ItemBlock(TELEPORTER) {

		@Override
		public String getUnlocalizedName(final ItemStack stack) {
			return stack.getItem().getUnlocalizedName().replace("tile.", "block.") + (XPTUtils.isEfficient(stack) ? ".efficient" : "");
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> tooltip, final ITooltipFlag flag) {
			if (XPTUtils.isEfficient(stack)) {
				tooltip.add(I18n.translateToLocal("tooltip.xpt.teleporter.efficient"));
			}
		}

	}.setRegistryName(TELEPORTER.getRegistryName()).setHasSubtypes(true);
	public static final ItemLinkCard LINK_CARD = new ItemLinkCard();

	public static final Object[] MOD_OBJECTS = new Object[] {
			TELEPORTER, LINK_CARD
	};

	public static void registerItem(final RegistryEvent.Register<Item> e) {
		e.getRegistry().register(LINK_CARD);
		e.getRegistry().register(TELEPORTER_ITEM);
	}

	public static void registerBlock(final RegistryEvent.Register<Block> e) {
		e.getRegistry().register(TELEPORTER);
	}

	@SideOnly(Side.CLIENT)
	public static void registerModels() {
		for (final Object o : MOD_OBJECTS) {
			if (o instanceof IModelRegister) {
				((IModelRegister) o).registerModel();
			}
		}
	}

}
