package committee.nova.mkb.api;

import committee.nova.mkb.keybinding.KeyModifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public interface IKeyBinding {
    InputUtil.Key mkb$getKey();

    IKeyConflictContext mkb$getKeyConflictContext();

    KeyModifier mkb$getKeyModifierDefault();

    KeyModifier mkb$getKeyModifier();

    void mkb$setKeyConflictContext(IKeyConflictContext keyConflictContext);

    void mkb$setKeyModifierAndCode(KeyModifier keyModifier, InputUtil.Key keyCode);

    void mkb$press();

    default boolean mkb$isConflictContextAndModifierActive() {
        return mkb$getKeyConflictContext().isActive() && mkb$getKeyModifier().isActive(mkb$getKeyConflictContext());
    }

    /**
     * Returns true when one of the bindings' key codes conflicts with the other's modifier.
     */
    default boolean mkb$hasKeyCodeModifierConflict(KeyBinding other) {
        final IKeyBinding extended = (IKeyBinding) other;
        if (mkb$getKeyConflictContext().conflicts(extended.mkb$getKeyConflictContext()) || extended.mkb$getKeyConflictContext().conflicts(mkb$getKeyConflictContext())) {
            return mkb$getKeyModifier().matches(extended.mkb$getKey()) || extended.mkb$getKeyModifier().matches(mkb$getKey());
        }
        return false;
    }

    /**
     * Checks that the key conflict context and modifier are active, and that the keyCode matches this binding.
     */
    default boolean mkb$isActiveAndMatches(InputUtil.Key keyCode) {
        return keyCode != InputUtil.UNKNOWN_KEY && keyCode.equals(mkb$getKey()) && mkb$getKeyConflictContext().isActive() && mkb$getKeyModifier().isActive(mkb$getKeyConflictContext());
    }

    default void mkb$setToDefault() {
        mkb$setKeyModifierAndCode(mkb$getKeyModifierDefault(), mkb$getKeyBinding().getDefaultKey());
    }

    default KeyBinding mkb$getKeyBinding() {
        return (KeyBinding) this;
    }
}
