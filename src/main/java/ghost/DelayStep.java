package ghost;

import java.awt.Robot;

public class DelayStep implements Step {

    private int millis;

    public DelayStep(int millis) {
        this.millis = millis;
    }

    public int getMillis() {
        return millis;
    }

    public void setMillis(int millis) {
        this.millis = millis;
    }

    public String getType() {
        return "DELAY";
    }

    public String getData() {
        return String.valueOf(millis);
    }

    public void play(Robot robot) throws InterruptedException {
        Thread.sleep(millis);
    }

    public String toString() {
        return "Wait " + millis + " ms";
    }
}
