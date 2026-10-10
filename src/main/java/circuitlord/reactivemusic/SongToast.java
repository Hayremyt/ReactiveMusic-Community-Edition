package circuitlord.reactivemusic;

//? if >=1.20 {
/*import net.minecraft.client.gui.DrawContext;
*///?} else {
import net.minecraft.client.util.math.MatrixStack;
//?}
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

/**
 * Now-playing toast, replaces the vanilla music toast (which never fires since
 * Reactive Music bypasses the vanilla music system entirely).
 */
public class SongToast implements Toast {

    private static final int DURATION_MS = 5000;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;

    private final String title;
    private final String subtitle;

    //? if >=1.21.11 {
    /*private long shownAtMs = -1;
    *///?}

    public SongToast(String song, String packName) {
        String pretty = SongPicker.getSongName(song);
        if (pretty.length() > 38) pretty = pretty.substring(0, 38) + "...";
        this.title = pretty;
        this.subtitle = packName != null ? packName : "";
    }

    public static void show(String song, String packName) {
        if (ReactiveMusic.config == null || !ReactiveMusic.config.showNowPlayingToast) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.getToastManager().add(new SongToast(song, packName));
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    //? if >=1.21.11 {
    /*@Override
    public void update(ToastManager manager, long time) {
    }

    @Override
    public Visibility getVisibility() {
        if (shownAtMs < 0) shownAtMs = net.minecraft.util.Util.getMeasuringTimeMs();
        return (net.minecraft.util.Util.getMeasuringTimeMs() - shownAtMs >= DURATION_MS) ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void draw(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, long startTime) {
        context.fill(0, 0, WIDTH, HEIGHT, 0xCC000000);
        context.drawTextWithShadow(textRenderer, Text.literal(title), 8, 7, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(subtitle), 8, 19, 0xFFA0A0A0);
    }
    *///?} else if >=1.20 {
    /*@Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        context.fill(0, 0, WIDTH, HEIGHT, 0xCC000000);
        context.drawTextWithShadow(manager.getClient().textRenderer, Text.literal(title), 8, 7, 0xFFFFFFFF);
        context.drawTextWithShadow(manager.getClient().textRenderer, Text.literal(subtitle), 8, 19, 0xFFA0A0A0);
        return startTime >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
    }
    *///?} else {
    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        var textRenderer = manager.getClient().textRenderer;
        fill(matrices, 0, 0, WIDTH, HEIGHT, 0xCC000000);
        textRenderer.drawWithShadow(matrices, Text.literal(title), 8, 7, 0xFFFFFFFF);
        textRenderer.drawWithShadow(matrices, Text.literal(subtitle), 8, 19, 0xFFA0A0A0);
        return startTime >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
    }
    //?}

    //? if <1.20 {
    private static void fill(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        net.minecraft.client.gui.DrawableHelper.fill(matrices, x1, y1, x2, y2, color);
    }
    //?}
}
