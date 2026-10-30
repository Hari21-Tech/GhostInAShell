package ghost;

import java.awt.Robot;

public interface Step {

    String getType();

    String getData();

    void play(Robot robot) throws InterruptedException;

    static Step create(String type, String data) throws MacroException {
        String[] parts = data.split(",");
        try {
            switch (type) {
                case "CLICK":
                    return new ClickStep(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "KEY":
                    return new KeyStep(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                case "DELAY":
                    return new DelayStep(Integer.parseInt(parts[0]));
                default:
                    throw new MacroException("Unknown step type: " + type);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new MacroException("Bad data for " + type + " step: " + data);
        }
    }
}
