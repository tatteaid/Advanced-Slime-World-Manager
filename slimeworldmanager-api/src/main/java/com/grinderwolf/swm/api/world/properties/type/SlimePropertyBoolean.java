package com.grinderwolf.swm.api.world.properties.type;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.properties.SlimeProperty;

import java.util.function.Function;

/**
 * A slime property of type boolean
 */
public class SlimePropertyBoolean extends SlimeProperty<Boolean> {

	public SlimePropertyBoolean(String nbtName, Boolean defaultValue) {
		super(nbtName, defaultValue);
	}

	public SlimePropertyBoolean(String nbtName, Boolean defaultValue, Function<Boolean, Boolean> validator) {
		super(nbtName, defaultValue, validator);
	}

	@Override
	protected void writeValue(CompoundMap compound, Boolean value) {
		compound.put(getNbtName(), new ByteTag(getNbtName(), (byte) (value ? 1 : 0)));
	}

	@Override
	protected Boolean readValue(CompoundTag compound) {
		return compound.getByteValue(getNbtName())
			.map((value) -> value == 1)
			.orElse(getDefaultValue());
	}
}
