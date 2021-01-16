package com.grinderwolf.swm.api.world.properties.type;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.StringTag;
import com.grinderwolf.swm.api.world.properties.SlimeProperty;

import java.util.function.Function;

/**
 * A slime property of type integer
 */
public class SlimePropertyString extends SlimeProperty<String> {

	public SlimePropertyString(String nbtName, String defaultValue) {
		super(nbtName, defaultValue);
	}

	public SlimePropertyString(String nbtName, String defaultValue, Function<String, Boolean> validator) {
		super(nbtName, defaultValue, validator);
	}

	@Override
	protected void writeValue(CompoundMap compound, String value) {
		compound.put(getNbtName(), new StringTag(getNbtName(), value));
	}

	@Override
	protected String readValue(CompoundTag compound) {
		return compound.getStringValue(getNbtName())
			.orElse(getDefaultValue());
	}
}
