package ghost;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class KeyStep implements Step {

    private final int keyCode;
    private final int modifiers;

    public KeyStep(int keyCode, int modifiers) {
        this.keyCode = keyCode;
        this.modifiers = modifiers;
    }

    public String getType() {
        return "KEY";
    }

    public String getData() {
        return keyCode + "," + modifiers;
    }

    public void play(Robot robot) {
        List<Integer> held = new ArrayList<>();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            held.add(KeyEvent.VK_CONTROL);
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            held.add(KeyEvent.VK_SHIFT);
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            held.add(KeyEvent.VK_ALT);
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            held.add(KeyEvent.VK_META);
        }
        for (int key : held) {
            robot.keyPress(key);
        }
        robot.keyPress(keyCode);
        robot.delay(20);
        robot.keyRelease(keyCode);
        for (int key : held) {
            robot.keyRelease(key);
        }
    }

    public String toString() {
        String text = KeyEvent.getKeyText(keyCode);
        if (modifiers != 0) {
            text = InputEvent.getModifiersExText(modifiers) + "+" + text;
        }
        return "Press " + text;
    }
}
