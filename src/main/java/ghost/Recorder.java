package ghost;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.SwingKeyAdapter;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class Recorder extends SwingKeyAdapter implements NativeMouseListener {

    private final List<Step> steps = new ArrayList<>();
    private long lastTime;
    private Runnable onStop;

    public void start(Runnable onStop) {
        this.onStop = onStop;
        steps.clear();
        lastTime = System.currentTimeMillis();
        GlobalScreen.addNativeKeyListener(this);
        GlobalScreen.addNativeMouseListener(this);
    }

    public void stop() {
        GlobalScreen.removeNativeKeyListener(this);
        GlobalScreen.removeNativeMouseListener(this);
        onStop.run();
    }

    public List<Step> getSteps() {
        return new ArrayList<>(steps);
    }

    private void addDelay() {
        long now = System.currentTimeMillis();
        int gap = (int) (now - lastTime);
        if (gap > 50) {
            steps.add(new DelayStep(gap));
        }
        lastTime = now;
    }

    public void nativeMousePressed(NativeMouseEvent e) {
        int button = 1;
        if (e.getButton() == NativeMouseEvent.BUTTON2) {
            button = 3;
        } else if (e.getButton() == NativeMouseEvent.BUTTON3) {
            button = 2;
        }
        double scale = 1;
        if (System.getProperty("os.name").startsWith("Windows")) {
            scale = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().getDefaultTransform().getScaleX();
        }
        addDelay();
        steps.add(new ClickStep((int) (e.getX() / scale), (int) (e.getY() / scale), button));
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
            stop();
            return;
        }
        int keyCode = getJavaKeyEvent(e).getKeyCode();
        if (keyCode == KeyEvent.VK_UNDEFINED || keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META) {
            return;
        }
        int modifiers = 0;
        if ((e.getModifiers() & NativeInputEvent.CTRL_MASK) != 0) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((e.getModifiers() & NativeInputEvent.SHIFT_MASK) != 0) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((e.getModifiers() & NativeInputEvent.ALT_MASK) != 0) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if ((e.getModifiers() & NativeInputEvent.META_MASK) != 0) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }
        addDelay();
        steps.add(new KeyStep(keyCode, modifiers));
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    public void nativeKeyTyped(NativeKeyEvent e) {
    }
}
