package latmod.xpt.block;

import javax.annotation.Nullable;

import latmod.xpt.init.ModConfig;
import latmod.xpt.init.ModObjects;
import latmod.xpt.item.ItemLinkCard;
import latmod.xpt.util.XPTUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("deprecation")
public class TileTeleporter extends TileEntity implements ITickable {

	static class Tags {

		public static final String TIMER = "Timer", LINK = "Link", NAME = "Name";

	}

	public int linkedX;
	public int linkedY;
	public int linkedZ;
	public int linkedDim;
	public int cooldown = 0;
	public int maxCooldown = 0;
	private String name = "";
	private boolean created = false;

	@Override
	public void readFromNBT(final NBTTagCompound tag) {
		if (tag.hasKey(Tags.LINK) && tag.hasKey(Tags.TIMER)) {
			final int[] link = tag.getIntArray(Tags.LINK);
			final int[] timer = tag.getIntArray(Tags.TIMER);
			linkedX = link[0];
			linkedY = link[1];
			linkedZ = link[2];
			linkedDim = link[3];
			cooldown = timer[0];
			maxCooldown = timer[1];
		}
		super.readFromNBT(tag);

		if (tag.hasKey(Tags.NAME, NBT.TAG_STRING)) {
			name = tag.getString(Tags.NAME);
		}

		//TODO remove
		if (tag.hasKey(Tags.NAME.toLowerCase(), NBT.TAG_STRING)) {
			name = tag.getString(Tags.NAME.toLowerCase());
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound tag) {
		tag.setIntArray(Tags.LINK, new int[] {
				linkedX, linkedY, linkedZ, linkedDim
		});
		tag.setIntArray(Tags.TIMER, new int[] {
				cooldown, maxCooldown
		});
		if (!name.isEmpty()) {
			tag.setString(Tags.NAME, name);
		}
		return super.writeToNBT(tag);
	}

	@Override
	public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity pkt) {
		final NBTTagCompound tag = pkt.getNbtCompound();
		final int[] link = tag.getIntArray(Tags.LINK);
		final int[] timer = tag.getIntArray(Tags.TIMER);
		linkedX = link[0];
		linkedY = link[1];
		linkedZ = link[2];
		linkedDim = link[3];
		cooldown = timer[0];
		maxCooldown = timer[1];
		name = tag.getString(Tags.NAME);
		//TODO remove
		if (name.isEmpty()) {
			name = tag.getString(Tags.NAME.toLowerCase());
		}
		getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 255, getUpdateTag());
	}

	public int getType() {
		if (getWorld() != null && linkedY > 0) {
			return linkedDim == getDimension() ? 1 : 2;
		}
		return 0;
	}

	@Override
	public void markDirty() {
		//super.markDirty();
		if (getWorld() != null) {
			final IBlockState state = getWorld().getBlockState(getPos());
			getWorld().notifyBlockUpdate(getPos(), state, state, 3);
		}
	}

	@Override
	public void update() {
		if (cooldown < 0) {
			cooldown = 0;
		}
		if (cooldown > 0) {
			--cooldown;
			if (cooldown == 0 && isServer()) {
				markDirty();
			}
		}
		if (!created && isServer()) {
			created = true;
			markDirty();
		}
	}

	public boolean createLink(final int x, final int y, final int z, final int dim, final boolean updateLink) {
		if (!isServer()) {
			return false;
		}
		if (linkedX == x && linkedY == y && linkedZ == z && dim == linkedDim) {
			// I dunno..this just allows overriding..I see no issue with this
			//return false;
		}
		if (pos.getX() == x && pos.getY() == y && pos.getZ() == z && dim == getDimension()) {
			return false;
		}
		TileTeleporter t = getLinkedTile();
		if (t != null) {
			t.linkedY = 0;
			t.markDirty();
		}
		linkedX = x;
		linkedY = y;
		linkedZ = z;
		linkedDim = dim;
		if (updateLink && (t = getLinkedTile()) != null) {
			t.createLink(pos.getX(), pos.getY(), pos.getZ(), getDimension(), false);
		}
		markDirty();
		return true;
	}

	private int getDimension() {
		return getWorld() == null ? 0 : getWorld().provider.getDimension();
	}

	private boolean isServer() {
		return getWorld() != null && !getWorld().isRemote;
	}

	public void onRightClick(final EntityPlayer ep, final ItemStack is) {
		if (!isServer() || is.isEmpty()) {
			return;
		}
		if (is.getItem() == Items.NAME_TAG) {
			if (!is.hasDisplayName()) {
				return;
			}
			setName(is.getDisplayName());
			if (!ep.capabilities.isCreativeMode) {
				is.shrink(1);
			}
			return;
		}
		if (is.getItem() != ModObjects.LINK_CARD) {
			return;
		}
		if (ItemLinkCard.hasData(is)) {
			final int[] pos = is.getTagCompound().getIntArray(ItemLinkCard.NBT_TAG);
			int xp = 0;
			if (ModConfig.only_linking_uses_xp) {
				final boolean crossdim = pos[3] != getDimension();
				if (crossdim) {
					xp = ModConfig.xp_for_crossdim;
				}
				else {
					final double dist = Math.sqrt(getDistanceSq(pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5));
					xp = ModConfig.xp_for_1000_blocks > 0 ? MathHelper.ceil(ModConfig.xp_for_1000_blocks * dist / 1000.0) : 0;
				}
				if (!ep.capabilities.isCreativeMode && XPTUtils.canTeleport(ep, xp)) {
					ep.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("messages.xpt.need_xp_to_link", xp)));
					return;
				}
			}
			if (createLink(pos[0], pos[1], pos[2], pos[3], true)) {
				is.shrink(1);
				XPTUtils.consumeXp(ep, xp);
				//String prefixkey = ;
				final String prefix = I18n.translateToLocal("messages.xpt." + (linkedDim == getDimension() ? "intra" : "extra"));
				ep.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("messages.xpt.link_created", prefix)));
			}
			else {
				ep.sendMessage(new TextComponentString(I18n.translateToLocal("messages.xpt.cant_create_link")));
			}
		}
		else if (getPos().getY() > 0) {
			if (is.getCount() > 1) {
				if (!ep.capabilities.isCreativeMode) {
					is.shrink(1);
				}
				final ItemStack is1 = new ItemStack(ModObjects.LINK_CARD, 1);
				is1.setTagCompound(new NBTTagCompound());
				is1.getTagCompound().setIntArray(ItemLinkCard.NBT_TAG, new int[] {
						pos.getX(), pos.getY(), pos.getZ(), getWorld().provider.getDimension()
				});
				if (ep.inventory.addItemStackToInventory(is1)) {
					ep.openContainer.detectAndSendChanges();
				}
				else {
					getWorld().spawnEntity(new EntityItem(getWorld(), ep.posX, ep.posY, ep.posZ, is1));
				}
			}
			else {
				is.setTagCompound(new NBTTagCompound());
				is.getTagCompound().setIntArray(ItemLinkCard.NBT_TAG, new int[] {
						pos.getX(), pos.getY(), pos.getZ(), getWorld().provider.getDimension()
				});
			}
		}
	}

	public TileTeleporter getLinkedTile() {
		if (linkedY > 0) {
			World w = null;
			if (isServer()) {
				w = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(linkedDim);
			}
			else {
				// I do it this way to keep from constantly loading the crossdim
				// world repeatedly client-side in a SP situation as I make calls to
				// this method for rendering
				w = DimensionManager.getWorld(linkedDim);
			}
			if (w != null) {
				final TileEntity te = w.getTileEntity(new BlockPos(linkedX, linkedY, linkedZ));
				if (te != null && !te.isInvalid() && te instanceof TileTeleporter) {
					return (TileTeleporter) te;
				}
			}
		}
		return null;
	}

	public boolean isEfficient() {
		return XPTUtils.isEfficient(this);
	}

	public void onPlayerCollided(final EntityPlayerMP ep) {
		if ((cooldown <= 0 || ep.capabilities.isCreativeMode) && ep.isSneaking() && !(ep instanceof FakePlayer)) {
			ep.setSneaking(false);

			final TileTeleporter t = getLinkedTile();
			if (t != null && (t.linkedY <= 0 || equals(t.getLinkedTile()))) {
				if (t.linkedY <= 0) {
					t.createLink(getPos().getX(), getPos().getY(), getPos().getZ(), getDimension(), true);
				}

				final boolean crossdim = linkedDim != getDimension();
				final double dist = crossdim ? 0.0 : Math.sqrt(getDistanceSq(t.pos.getX() + 0.5, t.pos.getY() + 0.5, t.pos.getZ() + 0.5));
				int xp = 0;
				if (!ModConfig.only_linking_uses_xp) {
					xp = XPTUtils.getXpCost(dist, crossdim, linkedDim, isEfficient());
				}
				if (!ep.capabilities.isCreativeMode && !XPTUtils.canTeleport(ep, xp)) {
					ep.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("messages.xpt.need_xp_to_teleport", xp)));
					return;
				}
				if (XPTUtils.teleportPlayer(ep, linkedX, linkedY, linkedZ, linkedDim)) {
					if (xp > 0 && !ep.capabilities.isCreativeMode) {
						XPTUtils.consumeXp(ep, xp);
					}
					t.cooldown = t.maxCooldown = ModConfig.cooldown_seconds * 20;
					maxCooldown = t.maxCooldown;
					cooldown = t.maxCooldown;
					ep.motionY = 0.05;
					//ep.sendMessage(new TextComponentString("Used teleportor '" + (!getName().isEmpty() ? getName() : getDimPosDesc()) + "'"));
					markDirty();
					t.markDirty();

				}
			}
			else {
				ep.sendMessage(new TextComponentString(I18n.translateToLocal("messages.xpt.link_broken")));
				if (t != null) {
					linkedY = 0;
					markDirty();
				}
			}
		}
	}

	public String getLinkedDimPosDesc() {
		String name = getLinkedTileName();
		if (name.isEmpty()) {
			if (linkedDim != getDimension()) {
				name = XPTUtils.getDimName(linkedDim);
				name = XPTUtils.capitaliseAllWords(name.replaceAll("_", " "));
			}
			name = " " + name;
			name = name + "\nX: " + linkedX + ", Y: " + linkedY + ", Z:" + linkedZ;
		}
		else {
			name = "\n" + name;
		}
		return I18n.translateToLocal("messages.xpt.linked_to") + name;
	}

	public void onPlacedBy(final EntityPlayer el, final ItemStack is) {
		if (is.hasDisplayName()) {
			setName(is.getDisplayName());
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		final double d = 0.5D;
		return new AxisAlignedBB(getPos().getX() - d, getPos().getY(), getPos().getZ() - d, getPos().getX() + 1D + d, getPos().getY() + 2D, getPos().getZ() + 1D + d);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 64D;
	}

	public void setName(final String s) {
		if (s == null || s.isEmpty()) {
			name = "";
		}
		else {
			name = s;
		}
		markDirty();
	}

	public String getName() {
		return name == null ? "" : name;
	}

	public String getLinkedTileName() {
		final TileTeleporter t = getLinkedTile();
		return t != null ? t.getName() : "";
	}

	@Override
	public boolean shouldRefresh(final World world, final BlockPos pos, final IBlockState oldState, final IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	public void onBroken() {
		if (ModConfig.unlink_broken) {
			createLink(0, 0, 0, 0, true);
		}
	}

}