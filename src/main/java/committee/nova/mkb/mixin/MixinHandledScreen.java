package committee.nova.mkb.mixin;

import committee.nova.mkb.api.IKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    @Shadow
    protected abstract boolean handleHotbarKeyPressed(KeyInput input);

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Shadow
    public abstract void close();

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(Lnet/minecraft/client/gui/Click;)Z"))
    public boolean redirect$mouseClicked(KeyBinding instance, Click click) {
        return ((IKeyBinding) instance).mkb$isActiveAndMatches(InputUtil.Type.MOUSE.createFromCode(click.getKeycode()));
    }

    @Redirect(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(Lnet/minecraft/client/gui/Click;)Z"))
    public boolean redirect$mouseReleased(KeyBinding instance, Click click) {
        return ((IKeyBinding) instance).mkb$isActiveAndMatches(InputUtil.Type.MOUSE.createFromCode(click.getKeycode()));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void inject$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        final InputUtil.Key mouseKey = InputUtil.fromKeyCode(input);
        if (super.keyPressed(input)) {
            cir.setReturnValue(true);
            return;
        }
        if (client != null && ((IKeyBinding) client.options.inventoryKey).mkb$isActiveAndMatches(mouseKey)) {
            this.close();
            cir.setReturnValue(true);
            return;
        }
        boolean handled = this.handleHotbarKeyPressed(input);
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            if (((IKeyBinding) this.client.options.pickItemKey).mkb$isActiveAndMatches(mouseKey)) {
                this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.CLONE);
                handled = true;
            } else if (((IKeyBinding) this.client.options.dropKey).mkb$isActiveAndMatches(mouseKey)) {
                this.onMouseClick(this.focusedSlot, this.focusedSlot.id, MinecraftClient.getInstance().isCtrlPressed() ? 1 : 0, SlotActionType.THROW);
                handled = true;
            }
        } else if (((IKeyBinding) this.client.options.dropKey).mkb$isActiveAndMatches(mouseKey)) {
            handled = true;
            // From Forge MC-146650: Emulate MC bug, so we don't drop from hotbar when pressing drop without hovering over an item.
        }
        cir.setReturnValue(handled);
    }

//    @Redirect(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(Lnet/minecraft/client/input/KeyInput;)Z"))
//    public boolean redirect$handleHotbarKeyPressed(KeyBinding instance, KeyInput keyInput) {
//        return ((IKeyBinding) instance).mkb$isActiveAndMatches(InputUtil.fromKeyCode(keyInput));
//    }

//    @Inject(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(Lnet/minecraft/client/input/KeyInput;)Z"),cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
//    public void inject$handleHotbarKeyPressed_0(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
//        cir.setReturnValue(((IKeyBinding)             ).mkb$isActiveAndMatches(InputUtil.fromKeyCode(keyInput)));
//        return;
//    }



}
