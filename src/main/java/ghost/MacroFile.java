package ghost;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MacroFile {

    public static void save(Macro macro, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(macro.getName());
            writer.newLine();
            for (Step step : macro.getSteps()) {
                writer.write(step.getType() + "|" + step.getData());
                writer.newLine();
            }
        }
    }

    public static Macro load(File file) throws IOException, MacroException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String name = reader.readLine();
            if (name == null || name.isBlank()) {
                throw new MacroException("File is empty");
            }
            Macro macro = new Macro(name.trim());
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length != 2) {
                    throw new MacroException("Bad line: " + line);
                }
                macro.getSteps().add(Step.create(parts[0], parts[1]));
            }
            return macro;
        }
    }
}
