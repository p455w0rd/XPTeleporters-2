package latmod.xpt.util;

import java.util.HashMap;
import java.util.Map;

import latmod.xpt.block.TileTeleporter;
import latmod.xpt.init.ModConfig;
import latmod.xpt.init.ModObjects;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * @author p455w0rd
 *
 */
public class XPTUtils {

	static Map<Integer, String> dimNameList = new HashMap<>();

	public static int getXpCost(final double distance, final boolean crossDim, final int dimension, final boolean isEfficient) {
		if (crossDim) {
			final int result = ModConfig.xp_for_crossdim * ModConfig.getDestinationMuliplier(dimension);
			if (!isEfficient) {
				return result;
			}
			int e = result / 2;
			e = e < 1 ? 1 : e;
			return e;
		}
		final int result = ModConfig.xp_for_1000_blocks > 0 ? MathHelper.ceil(ModConfig.xp_for_1000_blocks * distance / 1000.0) : 0;
		if (!isEfficient) {
			return result;
		}
		int e = result / 2;
		e = e < 1 ? 1 : e;
		return e;
	}

	public static boolean canConsumeXp(final EntityPlayer ep, final int xp) {
		return xp <= 0 || ep.capabilities.isCreativeMode || getPlayerXP(ep) >= xp;
	}

	public static boolean canTeleport(final EntityPlayer ep, final int xp) {
		if (xp <= 0) {
			return true;
		}
		if (ModConfig.use_food_levels == 0) {
			return getPlayerXP(ep) >= xp;
		}
		final int foodLevels = ep.getFoodStats().getFoodLevel();
		return foodLevels > 0 && (ModConfig.use_food_levels == 1 || foodLevels >= Math.min(xp, 20));
	}

	public static void consumeXp(final EntityPlayer ep, final int xp) {
		if (xp <= 0) {
			return;
		}
		if (ModConfig.use_food_levels == 0) {
			//ep.addExperience(-xp);
			final int playerXP = getPlayerXP(ep);
			if (playerXP >= xp) {
				addPlayerXP(ep, -xp);
			}
		}
		else {
			ep.getFoodStats().addStats(-Math.min(ep.getFoodStats().getFoodLevel(), Math.min(20, xp)), 0.0f);
		}
		if (ep instanceof EntityPlayerMP) {
			((EntityPlayerMP) ep).connection.sendPacket(new SPacketEntityStatus(ep, (byte) 9));
		}
	}

	public static int getPlayerXP(final EntityPlayer player) {
		return (int) (getExperienceForLevel(player.experienceLevel) + player.experience * player.xpBarCap());
	}

	public static void addPlayerXP(final EntityPlayer player, final int amount) {
		final int experience = getPlayerXP(player) + amount;
		player.experienceTotal = experience;
		player.experienceLevel = getLevelForExperience(experience);
		final int expForLevel = getExperienceForLevel(player.experienceLevel);
		player.experience = (float) (experience - expForLevel) / (float) player.xpBarCap();
	}

	private static int sum(final int n, final int a0, final int d) {
		return n * (2 * a0 + (n - 1) * d) / 2;
	}

	public static int getLevelForExperience(int targetXp) {
		int level = 0;
		while (true) {
			final int xpToNextLevel = xpBarCap(level);
			if (targetXp < xpToNextLevel) {
				return level;
			}
			level++;
			targetXp -= xpToNextLevel;
		}
	}

	public static int xpBarCap(final int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}

		if (level >= 15) {
			return 37 + (level - 15) * 5;
		}

		return 7 + level * 2;
	}

	public static int getExperienceForLevel(final int level) {
		if (level == 0) {
			return 0;
		}
		if (level <= 15) {
			return sum(level, 7, 2);
		}
		if (level <= 30) {
			return 315 + sum(level - 15, 37, 5);
		}
		return 1395 + sum(level - 30, 112, 9);
	}

	public static void removeXP(final EntityPlayer player, final int amt) {
		if (player.experience >= amt && player.experienceLevel == 0) {
			player.experienceTotal = player.experienceTotal - amt;
			player.experience -= amt / player.xpBarCap();
		}
		else if (player.experienceLevel > 0 && player.experience >= amt) {
			player.experienceTotal = player.experienceTotal - amt;
			player.experienceLevel--;
			player.experience = (float) (player.xpBarCap() - amt) / player.xpBarCap();
			if (player.experienceLevel == 0) {
				player.experience = 0;
			}
		}
	}

	public static String getDimName(final int id) {
		if (!dimNameList.containsKey(id)) {
			dimNameList.put(id, FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(id).provider.getDimensionType().getName());
		}
		return dimNameList.get(id);
	}

	public static void teleportEffects(final EntityPlayer player) {
		player.getEntityWorld().playSound(player, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	public static boolean teleportPlayer(final Entity entity, final double x, final double y, final double z, final int dim) {
		if (entity == null) {
			return false;
		}
		entity.fallDistance = 0F;
		final EntityPlayerMP player = entity instanceof EntityPlayer ? (EntityPlayerMP) entity : null;
		if (player != null) {
			return TeleportUtils.teleportEntity(player, dim, x + 0.5, y + 0.25, z + 0.5) != null;
		}
		return false;
	}

	public static double getMovementFactor(final int dim) {
		if (dim == 0) {
			return 1D;
		}
		else if (dim == 1) {
			return 1D;
		}
		else if (dim == -1) {
			return 8D;
		}
		else {
			final World w = DimensionManager.getWorld(dim);
			return w == null ? 1D : w.provider.getMovementFactor();
		}
	}

	public static ModelResourceLocation getMRL(final ResourceLocation registryName) {
		return getMRL(registryName, "inventory");
	}

	public static ModelResourceLocation getMRL(final ResourceLocation registryName, final String variant) {
		return new ModelResourceLocation(registryName, variant);
	}

	public static ModelResourceLocation getDefaultMRL(final IForgeRegistryEntry.Impl<?> registryObject, final String variant) {
		return getMRL(registryObject.getRegistryName(), variant);
	}

	public static ModelResourceLocation getDefaultMRL(final IForgeRegistryEntry.Impl<?> registryObject) {
		return getMRL(registryObject.getRegistryName());
	}

	private static boolean isEfficient(final int meta) {
		return meta == 1;
	}

	public static boolean isEfficient(final ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() == ModObjects.TELEPORTER_ITEM) {
			return isEfficient(stack.getItemDamage());
		}
		return false;
	}

	public static boolean isEfficient(final IBlockState xpt) {
		return isEfficient(xpt.getBlock().getMetaFromState(xpt));
	}

	public static boolean isEfficient(final TileTeleporter xptTile) {
		return xptTile != null && xptTile.getWorld() != null && xptTile.getWorld().getBlockState(xptTile.getPos()) != null && isEfficient(xptTile.getWorld().getBlockState(xptTile.getPos()));
	}

}
