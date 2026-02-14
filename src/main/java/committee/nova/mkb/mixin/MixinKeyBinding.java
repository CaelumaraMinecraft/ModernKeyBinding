package committee.nova.mkb.mixin;

import committee.nova.mkb.ModernKeyBinding;
import committee.nova.mkb.api.IKeyBinding;
import committee.nova.mkb.api.IKeyConflictContext;
import committee.nova.mkb.keybinding.KeyBindingMap;
import committee.nova.mkb.keybinding.KeyConflictContext;
import committee.nova.mkb.keybinding.KeyModifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding implements IKeyBinding {
    @Shadow
    protected InputUtil.Key boundKey;

    @Shadow
    private int timesPressed;
    @Shadow
    private boolean pressed;
    @Shadow
    @Final
    private InputUtil.Key defaultKey;
    @Shadow
    @Final
    private static Map<String, KeyBinding> KEYS_BY_ID;

    @Unique
    private static final KeyBindingMap MAP = new KeyBindingMap();
    @Unique
    private KeyModifier keyModifierDefault;
    @Unique
    private @Nullable KeyModifier keyModifier;
    @Unique
    private IKeyConflictContext keyConflictContext;

    @Override
    public InputUtil.Key mkb$getKey() {
        return boundKey;
    }

    @Override
    public IKeyConflictContext mkb$getKeyConflictContext() {
        return keyConflictContext == null ? KeyConflictContext.UNIVERSAL : keyConflictContext;
    }

    @Override
    public KeyModifier mkb$getKeyModifierDefault() {
        return keyModifierDefault == null ? KeyModifier.NONE : keyModifierDefault;
    }

    @Override
    public KeyModifier mkb$getKeyModifier() {
        return keyModifier == null ? KeyModifier.NONE : keyModifier;
    }

    @Override
    public void mkb$setKeyConflictContext(IKeyConflictContext keyConflictContext) {
        this.keyConflictContext = keyConflictContext;
    }

    @Override
    public void mkb$setKeyModifierAndCode(KeyModifier keyModifier, InputUtil.Key keyCode) {
        this.boundKey = keyCode;
        if (keyModifier.matches(keyCode))
            keyModifier = KeyModifier.NONE;
        MAP.removeKey((KeyBinding) (Object) this);
        this.keyModifier = keyModifier;
        MAP.addKey(keyCode, (KeyBinding) (Object) this);
    }

    @Override
    public void mkb$press() {
        ++timesPressed;
    }

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/util/InputUtil$Type;ILnet/minecraft/client/option/KeyBinding$Category;I)V", at = @At("RETURN"))
    public void inject$init(String id, InputUtil.Type type, int code, KeyBinding.Category category, int i, CallbackInfo ci) {
        MAP.addKey(this.boundKey, (KeyBinding) (Object) this);
    }

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void inject$onKeyPressed(InputUtil.Key key, CallbackInfo ci) {
        ci.cancel();
        if (ModernKeyBinding.nonConflictKeys()) {
            MAP.lookupActives(key).forEach(k -> ((IKeyBinding) k).mkb$press());
            return;
        }
        KeyBinding keyBinding = MAP.lookupActive(key);
        if (keyBinding == null) return;
        ((IKeyBinding) keyBinding).mkb$press();
    }

    @Inject(method = "setKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void inject$setKeyPressed(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        ci.cancel();
        if (ModernKeyBinding.nonConflictKeys()) {
            MAP.lookupActives(key).forEach(k -> k.setPressed(pressed));
            return;
        }
        KeyBinding keyBinding = MAP.lookupActive(key);
        if (keyBinding == null) return;
        keyBinding.setPressed(pressed);
    }

    @Inject(method = "updateKeysByCode", at = @At("HEAD"), cancellable = true)
    private static void updateKeysByCode(CallbackInfo ci) {
        ci.cancel();
        MAP.clearMap();
        for (KeyBinding keybinding : KEYS_BY_ID.values())
            MAP.addKey(((IKeyBinding) keybinding).mkb$getKey(), keybinding);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void inject$isPressed(CallbackInfoReturnable<Boolean> cir) {
        if (((KeyBinding) (Object) this) instanceof StickyKeyBinding) {
            final StickyKeyBinding sticky = (StickyKeyBinding) (Object) this;
            cir.setReturnValue(pressed && (mkb$isConflictContextAndModifierActive() || ((AccessorStickyKeyBinding) sticky).getToggleGetter().getAsBoolean()));
            return;
        }
        cir.setReturnValue(pressed && mkb$isConflictContextAndModifierActive());
    }

    @Inject(method = "isDefault", at = @At("RETURN"), cancellable = true)
    private void inject$isDefault(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(boundKey.equals(defaultKey) && mkb$getKeyModifier() == mkb$getKeyModifierDefault());
    }

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void inject$equals(KeyBinding other, CallbackInfoReturnable<Boolean> cir) {
        final IKeyBinding extended = (IKeyBinding) other;
        if (!mkb$getKeyConflictContext().conflicts(extended.mkb$getKeyConflictContext()) && !extended.mkb$getKeyConflictContext().conflicts(mkb$getKeyConflictContext()))
            return;
        KeyModifier keyModifier = mkb$getKeyModifier();
        KeyModifier otherKeyModifier = extended.mkb$getKeyModifier();
        if (keyModifier.matches(extended.mkb$getKey()) || otherKeyModifier.matches(mkb$getKey())) {
            cir.setReturnValue(true);
        } else if (mkb$getKey().equals(extended.mkb$getKey())) {
            // IN_GAME key contexts have a conflict when at least one modifier is NONE.
            // For example: If you hold shift to crouch, you can still press E to open your inventory. This means that a Shift+E hotkey is in conflict with E.
            // GUI and other key contexts do not have this limitation.
            cir.setReturnValue(keyModifier == otherKeyModifier ||
                    (mkb$getKeyConflictContext().conflicts(KeyConflictContext.IN_GAME) &&
                            (keyModifier == KeyModifier.NONE || otherKeyModifier == KeyModifier.NONE)));
        }
    }

    @Inject(method = "getBoundKeyLocalizedText", at = @At("RETURN"), cancellable = true)
    private void inject$getBoundKeyLocalizedText(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(mkb$getKeyModifier().getCombinedName(boundKey, () -> this.boundKey.getLocalizedText()));
    }
}
