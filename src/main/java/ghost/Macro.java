package ghost;

import java.util.ArrayList;
import java.util.List;

public class Macro {

    private String name;
    private List<Step> steps;

    public Macro(String name) {
        this.name = name;
        this.steps = new ArrayList<>();
    }

    public Macro(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Step> getSteps() {
        return steps;
    }
}
