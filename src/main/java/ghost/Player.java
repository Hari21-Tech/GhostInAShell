package ghost;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.awt.AWTException;
import java.awt.Robot;
import java.util.function.BiConsumer;

public class Player implements NativeKeyListener {

    private Thread thread;
    private volatile boolean stopped;

    public void play(Macro macro, int repeat, BiConsumer<String, Integer> onFinish) {
        stopped = false;
        GlobalScreen.addNativeKeyListener(this);
        thread = new Thread(() -> {
            int done = 0;
            String status = "COMPLETED";
            try {
                Robot robot = new Robot();
                for (int i = 0; i < repeat && !stopped; i++) {
                    for (Step step : macro.getSteps()) {
                        if (stopped) {
                            break;
                        }
                        step.play(robot);
                        done++;
                    }
                }
            } catch (InterruptedException e) {
                stopped = true;
            } catch (AWTException e) {
                status = "FAILED";
            }
            if (stopped) {
                status = "STOPPED";
            }
            GlobalScreen.removeNativeKeyListener(this);
            onFinish.accept(status, done);
        });
        thread.start();
    }

    public void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            stop();
        }
    }
}
