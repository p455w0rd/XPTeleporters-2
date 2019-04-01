package latmod.xpt.block;

import com.google.common.collect.Lists;

import latmod.xpt.client.RenderTeleporter;
import latmod.xpt.init.ModGlobals;
import latmod.xpt.util.IModelRegister;
import latmod.xpt.util.XPTUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockTeleporter extends Block implements IModelRegister, ITileEntityProvider {

	AxisAlignedBB bb = new AxisAlignedBB(0F, 0F, 0F, 1F, 1F / 8F, 1F);
	private static final ResourceLocation REG_NAME = new ResourceLocation(ModGlobals.MOD_ID, "teleporter");
	static PropertyXPTType TYPE = PropertyXPTType.create();

	public BlockTeleporter() {
		super(Material.IRON);
		setRegistryName(REG_NAME);
		setUnlocalizedName(REG_NAME.toString().replace(":", "."));
		setHardness(1F);
		setResistance(100000F);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		GameRegistry.registerTileEntity(TileTeleporter.class, REG_NAME);
		setDefaultState(blockState.getBaseState().withProperty(TYPE, Type.NORMAL));
	}

	@Override
	public void getSubBlocks(final CreativeTabs itemIn, final NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));
		items.add(new ItemStack(this, 1, 1));
	}

	@Override
	public IBlockState getStateFromMeta(final int meta) {
		return blockState.getBaseState().withProperty(TYPE, Type.values()[meta]);
	}

	@Override
	public int getMetaFromState(final IBlockState state) {
		return state.getValue(TYPE).ordinal();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] {
				TYPE
		});
	}

	@Override
	public IBlockState getStateForPlacement(final World worldIn, final BlockPos pos, final EnumFacing facing, final float hitX, final float hitY, final float hitZ, final int meta, final EntityLivingBase placer) {
		final int dmg = placer.getHeldItemMainhand().getItemDamage();
		final IBlockState state = getDefaultState().withProperty(TYPE, Type.values()[dmg]);
		return state;
	}

	@Override
	public void onBlockPlacedBy(final World world, final BlockPos pos, final IBlockState state, final EntityLivingBase placer, final ItemStack stack) {
		if (placer instanceof EntityPlayer && getTeleporterTile(world, pos) != null) {
			getTeleporterTile(world, pos).onPlacedBy((EntityPlayer) placer, stack);
		}
		super.onBlockPlacedBy(world, pos, state, placer, stack);
	}

	@Override
	public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state, final EntityPlayer player, final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
		if (player instanceof EntityPlayer && hand == EnumHand.MAIN_HAND && !player.getHeldItemMainhand().isEmpty() && getTeleporterTile(world, pos) != null) {
			getTeleporterTile(world, pos).onRightClick(player, player.getHeldItemMainhand());
		}
		return false;
	}

	protected TileTeleporter getTeleporterTile(final World world, final BlockPos pos) {
		if (world.getTileEntity(pos) != null && world.getTileEntity(pos) instanceof TileTeleporter) {
			return (TileTeleporter) world.getTileEntity(pos);
		}
		return null;
	}

	@Override
	public boolean canHarvestBlock(final IBlockAccess world, final BlockPos pos, final EntityPlayer player) {
		return true;
	}

	@Override
	public boolean hasTileEntity() {
		return true;
	}

	@Override
	public TileEntity createTileEntity(final World world, final IBlockState state) {
		return new TileTeleporter();
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(final IBlockState blockState, final IBlockAccess worldIn, final BlockPos pos) {
		return bb;
	}

	@Override
	public AxisAlignedBB getBoundingBox(final IBlockState state, final IBlockAccess source, final BlockPos pos) {
		return bb;
	}

	@Override
	public boolean isPassable(final IBlockAccess worldIn, final BlockPos pos) {
		return true;
	}

	@Override
	public boolean doesSideBlockRendering(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing face) {
		return false;
	}

	@Override
	public boolean isFullCube(final IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(final IBlockState state) {
		return false;
	}

	@Override
	public boolean canEntityDestroy(final IBlockState state, final IBlockAccess world, final BlockPos pos, final Entity entity) {
		return !(entity instanceof EntityDragon || entity instanceof EntityWither);
	}

	@Override
	public void onEntityCollidedWithBlock(final World w, final BlockPos pos, final IBlockState state, final Entity e) {
		if (e != null && !e.isDead && e instanceof EntityPlayerMP) {
			final TileTeleporter t = (TileTeleporter) w.getTileEntity(pos);
			if (t != null) {
				t.onPlayerCollided((EntityPlayerMP) e);
			}
		}
	}

	@Override
	public void registerModel() {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, XPTUtils.getMRL(REG_NAME, Type.NORMAL.getBlockStateVariant()));
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 1, XPTUtils.getMRL(REG_NAME, Type.EFFICIENT.getBlockStateVariant()));
		ClientRegistry.bindTileEntitySpecialRenderer(TileTeleporter.class, new RenderTeleporter());
	}

	@Override
	public TileEntity createNewTileEntity(final World world, final int meta) {
		return createTileEntity(world, getStateFromMeta(meta));
	}

	@Override
	public EnumBlockRenderType getRenderType(final IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	static class PropertyXPTType extends PropertyEnum<Type> {

		static final String NAME = "type";

		protected PropertyXPTType() {
			super(NAME, Type.class, Lists.newArrayList(Type.values()));
		}

		public static PropertyXPTType create() {
			return new PropertyXPTType();
		}

	}

	protected static enum Type implements IStringSerializable {

			NORMAL, EFFICIENT;

		@Override
		public String getName() {
			return ordinal() == 1 ? "efficient" : "normal";
		}

		public String getBlockStateVariant() {
			return "type=" + getName();
		}

	}

}