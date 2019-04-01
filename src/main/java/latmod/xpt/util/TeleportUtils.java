package latmod.xpt.util;

import java.util.LinkedList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

/**
 * Created by brandon3055 on 6/12/2016.
 * <p>
 * This is a universal class for handling teleportation. Simply tell it where to send an entity and it just works!
 * Also has support for teleporting mounts.
 */
public class TeleportUtils {

	/**
	 * Universal method for teleporting entities of all shapes and sizes!
	 * This method will teleport an entity and any entity it is riding or that are ring it recursively. If riding the riding entity will be re mounted on the other side.
	 * <p>
	 * Note: When teleporting riding entities it is the rider that must be teleported and the mount will follow automatically.
	 * As long as you teleport the rider you should not need wo worry about the mount.
	 *
	 * @return the entity. This may be a new instance so be sure to keep that in mind.
	 */
	public static Entity teleportEntity(final Entity entity, final int dimension, final double xCoord, final double yCoord, final double zCoord, final float yaw, final float pitch) {
		if (entity == null || entity.world.isRemote) {
			return entity;
		}

		final MinecraftServer server = entity.getServer();
		final int sourceDim = entity.world.provider.getDimension();

		if (!entity.isBeingRidden() && !entity.isRiding()) {
			return handleEntityTeleport(entity, server, sourceDim, dimension, xCoord, yCoord, zCoord, yaw, pitch);
		}

		final Entity rootEntity = entity.getLowestRidingEntity();
		final PassengerHelper passengerHelper = new PassengerHelper(rootEntity);
		final PassengerHelper rider = passengerHelper.getPassenger(entity);
		if (rider == null) {
			return entity;
		}
		passengerHelper.teleport(server, sourceDim, dimension, xCoord, yCoord, zCoord, yaw, pitch);
		passengerHelper.remountRiders();
		passengerHelper.updateClients();

		return rider.entity;
	}

	/**
	 * Convenience method that does not require pitch and yaw.
	 */
	public static Entity teleportEntity(final Entity entity, final int dimension, final double xCoord, final double yCoord, final double zCoord) {
		return teleportEntity(entity, dimension, xCoord, yCoord, zCoord, entity.rotationYaw, entity.rotationPitch);
	}

	/**
	 * Added by TheRealp455w0rd on 3/31/2019
	 * Convenience method that accepts BlockPos and auto adjust to center-of-block
	 */
	public static Entity teleportEntity(final Entity entity, final int dimension, final BlockPos pos) {
		final double x = pos.getX() + 0.5;
		final double y = pos.getY() + 0.25; // Teleporter block in XPT is 0.2 units high.
		// I add 0.05 more to ensure the player lands on rather than in the block
		final double z = pos.getZ() + 0.5;
		return teleportEntity(entity, dimension, x, y, z);
	}

	/**
	 * This is the base teleport method that figures out how to handle the teleport and makes it happen!
	 */
	private static Entity handleEntityTeleport(final Entity entity, final MinecraftServer server, final int sourceDim, final int targetDim, final double xCoord, final double yCoord, final double zCoord, final float yaw, final float pitch) {
		if (entity == null || entity.world.isRemote) {
			return entity;
		}

		final boolean interDimensional = sourceDim != targetDim;

		if (interDimensional && !ForgeHooks.onTravelToDimension(entity, targetDim)) {
			return entity;
		}

		if (interDimensional) {
			if (entity instanceof EntityPlayerMP) {
				return teleportPlayerInterdimentional((EntityPlayerMP) entity, server, sourceDim, targetDim, xCoord, yCoord, zCoord, yaw, pitch);
			}
			else {
				return teleportEntityInterdimentional(entity, server, sourceDim, targetDim, xCoord, yCoord, zCoord, yaw, pitch);
			}
		}
		else {
			if (entity instanceof EntityPlayerMP) {
				final EntityPlayerMP player = (EntityPlayerMP) entity;
				player.connection.setPlayerLocation(xCoord, yCoord, zCoord, yaw, pitch);
				player.setRotationYawHead(yaw);
			}
			else {
				entity.setLocationAndAngles(xCoord, yCoord, zCoord, yaw, pitch);
				entity.setRotationYawHead(yaw);
			}
		}

		return entity;
	}

	private static Entity teleportEntityInterdimentional(final Entity entity, final MinecraftServer server, final int sourceDim, final int targetDim, final double xCoord, final double yCoord, final double zCoord, final float yaw, final float pitch) {
		if (entity.isDead) {
			return null;
		}

		final WorldServer sourceWorld = server.getWorld(sourceDim);
		final WorldServer targetWorld = server.getWorld(targetDim);

		if (!entity.isDead && entity instanceof EntityMinecart) {
			entity.setDropItemsWhenDead(false);
		}

		entity.dimension = targetDim;

		sourceWorld.removeEntity(entity);
		entity.isDead = false;
		entity.setLocationAndAngles(xCoord, yCoord, zCoord, yaw, pitch);
		sourceWorld.updateEntityWithOptionalForce(entity, false);

		final Entity newEntity = EntityList.newEntity(entity.getClass(), targetWorld);
		if (newEntity != null) {
			newEntity.copyDataFromOld(entity);
			newEntity.setLocationAndAngles(xCoord, yCoord, zCoord, yaw, pitch);
			final boolean flag = newEntity.forceSpawn;
			newEntity.forceSpawn = true;
			targetWorld.spawnEntity(newEntity);
			newEntity.forceSpawn = flag;
			targetWorld.updateEntityWithOptionalForce(newEntity, false);
		}

		entity.isDead = true;
		sourceWorld.resetUpdateEntityTick();
		targetWorld.resetUpdateEntityTick();

		return newEntity;
	}

	/**
	 * This is the black magic responsible for teleporting players between dimensions!
	 */
	private static EntityPlayer teleportPlayerInterdimentional(final EntityPlayerMP player, final MinecraftServer server, final int sourceDim, final int targetDim, final double xCoord, final double yCoord, final double zCoord, final float yaw, final float pitch) {
		final WorldServer sourceWorld = server.getWorld(sourceDim);
		final WorldServer targetWorld = server.getWorld(targetDim);
		final PlayerList playerList = server.getPlayerList();

		player.dimension = targetDim;
		player.connection.sendPacket(new SPacketRespawn(player.dimension, targetWorld.getDifficulty(), targetWorld.getWorldInfo().getTerrainType(), player.interactionManager.getGameType()));
		playerList.updatePermissionLevel(player);
		sourceWorld.removeEntityDangerously(player);
		player.isDead = false;

		//region Transfer to world

		player.setLocationAndAngles(xCoord, yCoord, zCoord, yaw, pitch);
		player.connection.setPlayerLocation(xCoord, yCoord, zCoord, yaw, pitch);
		targetWorld.spawnEntity(player);
		targetWorld.updateEntityWithOptionalForce(player, false);
		player.setWorld(targetWorld);

		//endregion

		playerList.preparePlayer(player, sourceWorld);
		player.connection.setPlayerLocation(xCoord, yCoord, zCoord, yaw, pitch);
		player.interactionManager.setWorld(targetWorld);
		player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));
		player.invulnerableDimensionChange = true;

		playerList.updateTimeAndWeatherForPlayer(player, targetWorld);
		playerList.syncPlayerInventory(player);

		for (final PotionEffect potioneffect : player.getActivePotionEffects()) {
			player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
		}
		net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, sourceDim, targetDim);
		player.setLocationAndAngles(xCoord, yCoord, zCoord, yaw, pitch);

		//Fixes stat syncing
		player.connection.sendPacket(new SPacketEffect(1032, BlockPos.ORIGIN, 0, false));
		player.lastExperience = -1;
		player.lastHealth = -1.0F;
		player.lastFoodLevel = -1;

		return player;
	}

	public static Entity getHighestRidingEntity(final Entity mount) {
		Entity entity;

		for (entity = mount; entity.getPassengers().size() > 0; entity = entity.getPassengers().get(0)) {
			;
		}

		return entity;
	}

	private static class PassengerHelper {
		public Entity entity;
		public LinkedList<PassengerHelper> passengers = new LinkedList<>();
		public double offsetX, offsetY, offsetZ;

		/**
		 * Creates a new passenger helper for the given entity and recursively adds all of the entities passengers.
		 *
		 * @param entity The root entity. If you have multiple stacked entities this would be the one at the bottom of the stack.
		 */
		public PassengerHelper(final Entity entity) {
			this.entity = entity;
			if (entity.isRiding()) {
				offsetX = entity.posX - entity.getRidingEntity().posX;
				offsetY = entity.posY - entity.getRidingEntity().posY;
				offsetZ = entity.posZ - entity.getRidingEntity().posZ;
			}
			for (final Entity passenger : entity.getPassengers()) {
				passengers.add(new PassengerHelper(passenger));
			}
		}

		/**
		 * Recursively teleports the entity and all of its passengers after dismounting them.
		 * @param server The minecraft server.
		 * @param sourceDim The source dimension.
		 * @param targetDim The target dimension.
		 * @param xCoord The target x position.
		 * @param yCoord The target y position.
		 * @param zCoord The target z position.
		 * @param yaw The target yaw.
		 * @param pitch The target pitch.
		 */
		public void teleport(final MinecraftServer server, final int sourceDim, final int targetDim, final double xCoord, final double yCoord, final double zCoord, final float yaw, final float pitch) {
			entity.removePassengers();
			entity = handleEntityTeleport(entity, server, sourceDim, targetDim, xCoord, yCoord, zCoord, yaw, pitch);
			for (final PassengerHelper passenger : passengers) {
				passenger.teleport(server, sourceDim, targetDim, xCoord, yCoord, zCoord, yaw, pitch);
			}
		}

		/**
		 * Recursively remounts all of this entities riders and offsets their position relative to their position before teleporting.
		 */
		public void remountRiders() {
			//If the mount was dead before teleporting then entity will be null.
			if (entity == null) {
				return;
			}
			if (entity.isRiding()) {
				entity.setLocationAndAngles(entity.posX + offsetX, entity.posY + offsetY, entity.posZ + offsetZ, entity.rotationYaw, entity.rotationPitch);
			}
			for (final PassengerHelper passenger : passengers) {
				passenger.entity.startRiding(entity, true);
				passenger.remountRiders();
			}
		}

		/**
		 * This method sends update packets to any players that were teleported with the entity stack.
		 */
		public void updateClients() {
			if (entity instanceof EntityPlayerMP) {
				updateClient((EntityPlayerMP) entity);
			}
			for (final PassengerHelper passenger : passengers) {
				passenger.updateClients();
			}
		}

		/**
		 * This is the method that is responsible for actually sending the update to each client.
		 * @param playerMP The Player.
		 */
		private void updateClient(final EntityPlayerMP playerMP) {
			if (entity.isBeingRidden()) {
				playerMP.connection.sendPacket(new SPacketSetPassengers(entity));
			}
			for (final PassengerHelper passenger : passengers) {
				passenger.updateClients();
			}
		}

		/**
		 * This method returns the helper for a specific entity in the stack.
		 * @param passenger The passenger you are looking for.
		 * @return The passenger helper for the specified passenger.
		 */
		public PassengerHelper getPassenger(final Entity passenger) {
			if (entity == passenger) {
				return this;
			}

			for (final PassengerHelper rider : passengers) {
				final PassengerHelper re = rider.getPassenger(passenger);
				if (re != null) {
					return re;
				}
			}

			return null;
		}
	}
}