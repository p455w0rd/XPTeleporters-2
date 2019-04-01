package latmod.xpt.item;

import java.util.List;

import latmod.xpt.init.ModGlobals;
import latmod.xpt.util.IModelRegister;
import latmod.xpt.util.XPTUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
@SuppressWarnings("deprecation")
public class ItemLinkCard extends Item implements IModelRegister {

	public static final String NBT_TAG = "Coords";
	private static final ResourceLocation REG_NAME = new ResourceLocation(ModGlobals.MOD_ID, "link_card");

	public ItemLinkCard() {
		setRegistryName(REG_NAME);
		setUnlocalizedName(REG_NAME.toString().replace(":", "."));
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		setMaxStackSize(1);
	}

	public static boolean hasData(final ItemStack is) {
		return is.hasTagCompound() && is.getTagCompound().hasKey(NBT_TAG);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasEffect(final ItemStack is) {
		return hasData(is);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(final World w, final EntityPlayer ep, final EnumHand h) {
		final ItemStack is = ep.getHeldItemMainhand();
		if (h == EnumHand.MAIN_HAND && !is.isEmpty() && !w.isRemote && ep.isSneaking() && hasData(is)) {
			is.getTagCompound().removeTag(NBT_TAG);
			if (is.getTagCompound().hasNoTags()) {
				is.setTagCompound(null);
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, is);
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(final ItemStack is, final EntityPlayer ep, final List<String> l, final boolean b) {
		if (hasData(is)) {
			final int[] coords = is.getTagCompound().getIntArray(NBT_TAG);
			l.add(I18n.translateToLocal("messages.xpt.linked_to") + " " + coords[0] + ", " + coords[1] + ", " + coords[2] + " @ " + XPTUtils.getDimName(coords[3]));
		}
	}

	@Override
	public void registerModel() {
		ModelLoader.setCustomModelResourceLocation(this, 0, XPTUtils.getDefaultMRL(this));
	}

}
