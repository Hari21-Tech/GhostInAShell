package ghost;

import java.awt.Robot;
import java.awt.event.InputEvent;

public class ClickStep implements Step {

    private final int x;
    private final int y;
    private final int button;

    public ClickStep(int x, int y, int button) {
        this.x = x;
        this.y = y;
        this.button = button;
    }

    public String getType() {
        return "CLICK";
    }

    public String getData() {
        return x + "," + y + "," + button;
    }

    public void play(Robot robot) {
        int mask = InputEvent.getMaskForButton(button);
        robot.mouseMove(x, y);
        robot.delay(30);
        robot.mousePress(mask);
        robot.delay(30);
        robot.mouseRelease(mask);
    }

    public String toString() {
        String name = "Left";
        if (button == 3) {
            name = "Right";
        } else if (button == 2) {
            name = "Middle";
        }
        return name + " click at (" + x + ", " + y + ")";
    }
}
