package latmod.xpt.init;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.config.Configuration;

/**
 * @author p455w0rd
 *
 */
@SuppressWarnings("deprecation")
public class ModConfig extends Configuration {

	private static final ModConfig INSTANCE = new ModConfig();
	public static int xp_for_1000_blocks = 20;
	public static int xp_for_crossdim = 30;
	public static int cooldown_seconds = 3;
	public static boolean enable_crafting = true;
	public static boolean only_linking_uses_xp = false;
	public static boolean unlink_broken = true;
	public static int use_food_levels = 0;
	// DimensionID->Multiplier
	public static final Map<Integer, Integer> destinationMultipliers = new HashMap<>();

	public ModConfig() {
		super(ModGlobals.MOD_CONFIG_FILE);
	}

	public static ModConfig getInstance() {
		return INSTANCE;
	}

	public static void init() {
		xp_for_1000_blocks = getInstance().getInt("xp_for_1000_blocks", CATEGORY_GENERAL, 20, 0, Integer.MAX_VALUE, I18n.translateToLocal("configdesc.xpt.xp_required") + " " + I18n.translateToLocal("configdesc.xpt.samedim"));
		xp_for_crossdim = getInstance().getInt("xp_for_crossdim", CATEGORY_GENERAL, 30, 0, Integer.MAX_VALUE, I18n.translateToLocal("configdesc.xpt.xp_required") + " " + I18n.translateToLocal("configdesc.xpt.crossdim"));
		cooldown_seconds = getInstance().getInt("cooldown_seconds", CATEGORY_GENERAL, 3, 1, 3600, I18n.translateToLocal("configdesc.xpt.teleporter_cooldown"));
		enable_crafting = getInstance().getBoolean("enable_crafting", CATEGORY_GENERAL, true, I18n.translateToLocal("configgdesc.xpt.enable_recipes"));
		only_linking_uses_xp = getInstance().getBoolean("only_linking_uses_xp", CATEGORY_GENERAL, false, I18n.translateToLocal("configdesc.xpt.only_linking_uses_xp"));
		unlink_broken = getInstance().getBoolean("unlink_broken", "general", false, I18n.translateToLocal("configdesc.xpt.unlink_broken"));
		use_food_levels = getInstance().getInt("use_food_levels", "general", 0, 0, 2, I18n.translateToLocal("configdesc.xpt.use_food_levels"));
		parseMultipliers(getInstance().getStringList("destination_multipliers", CATEGORY_GENERAL, new String[] {
				"-1:8"
		}, I18n.translateToLocal("configdesc.xpt.destination_multipliers")));
		if (getInstance().hasChanged()) {
			getInstance().save();
		}
	}

	public static int cooldownTicks() {
		return cooldown_seconds * 20;
	}

	private static void parseMultipliers(final String[] entries) {
		destinationMultipliers.clear();
		for (final String s : entries) {
			if (s.contains(":")) {
				final String[] cs = s.split(":");
				final int dimension = Integer.parseInt(cs[0]);
				int multiplier = Math.abs(Integer.parseInt(cs[1]));
				if (multiplier == 0) {
					++multiplier;
				}
				destinationMultipliers.put(dimension, multiplier);
			}
		}
	}

	public static int getDestinationMuliplier(final int dimension) {
		if (!destinationMultipliers.containsKey(dimension)) {
			destinationMultipliers.put(dimension, 1);
		}
		return destinationMultipliers.get(dimension);
	}

}
