package com.grinderwolf.swm.api.world.properties;

import com.flowpowered.nbt.*;
import lombok.*;

import java.util.*;
import java.util.function.Function;

/**
 * A Property Map object.
 */
@RequiredArgsConstructor()
public class SlimePropertyMap {

    @Getter(value = AccessLevel.PRIVATE)
    private final CompoundMap properties;

    public SlimePropertyMap() {
        this(new CompoundMap());
    }

    /**
     * Return the current value of the given property
     *
     * @param property The slime property
     * @return The current value
     */
    public <T> T getValue(SlimeProperty<T> property) {
        return property.readValue((CompoundTag) properties.get(property.getNbtName()));
    }

    /**
     * Update the value of the given property
     *
     * @param property The slime property
     * @param value The new value
     * @throws IllegalArgumentException if the value fails validation.
     */
    public <T> void setValue(SlimeProperty<T> property, T value) {
        if (property.getValidator() != null && !property.getValidator().apply(value)) {
            throw new IllegalArgumentException("'" + value + "' is not a valid property value.");
        }

        property.writeValue(properties, value);
    }

    /**
     * Returns the string value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link String} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not a string.
     * @deprecated Use generics method
     */
    @Deprecated
    public String getString(SlimeProperty<?> property) {
        try {
            return (String) getValue(property);
        } catch(ClassCastException err) {
            throw new IllegalArgumentException("Property type mismatch", err);
        }
    }

    /**
     * Returns the boolean value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link Boolean} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not a boolean.
     * @deprecated Use generics method
     */
    @Deprecated
    public Boolean getBoolean(SlimeProperty<?> property) {
        try {
            return (Boolean) getValue(property);
        } catch(ClassCastException err) {
            throw new IllegalArgumentException("Property type mismatch", err);
        }
    }

    /**
     * Returns the int value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the int value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not an integer.
     * @deprecated Use generics method
     */
    @Deprecated
    public int getInt(SlimeProperty<?> property) {
        try {
            return (Integer) getValue(property);
        } catch(ClassCastException err) {
            throw new IllegalArgumentException("Property type mismatch", err);
        }
    }

    /**
     * Copies all values from the specified {@link SlimePropertyMap}.
     * If the same property has different values on both maps, the one
     * on the providen map will be used.
     *
     * @param propertyMap A {@link SlimePropertyMap}.
     */
    public void merge(SlimePropertyMap propertyMap) {
        properties.putAll(propertyMap.properties);
    }

    /**
     * Returns a {@link CompoundTag} containing every property set in this map.
     *
     * @return A {@link CompoundTag} with all the properties stored in this map.
     */
    public CompoundTag toCompound() {
        return new CompoundTag("properties", properties);
    }

    @Override
    public String toString() {
        return "SlimePropertyMap" + properties;
    }
}
