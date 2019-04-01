package latmod.xpt.init;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonObject;

import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

/**
 * @author p455w0rd
 *
 */
public class RecipesEnabledFactory implements IConditionFactory {

	@Override
	public BooleanSupplier parse(final JsonContext jsonContext, final JsonObject jsonObject) {
		return () -> ModConfig.enable_crafting;
	}

}