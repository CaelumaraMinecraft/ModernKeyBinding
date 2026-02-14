package committee.nova.mkb.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class MixinKeyboard {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("TAIL"))
    private void inject$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        final Screen s = this.client.currentScreen;
        if (!(s instanceof KeybindsScreen kbs)) return;
        if (kbs.lastKeyCodeUpdateTime > Util.getMeasuringTimeMs() - 20L) return;
        if (!this.client.options.screenshotKey.matchesKey(input) && action == 0) kbs.selectedKeyBinding = null;
    }
}
