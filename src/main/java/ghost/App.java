package ghost;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class App extends Application {

    private final Database db = new Database();
    private final Recorder recorder = new Recorder();
    private final Player player = new Player();

    private Stage stage;
    private final ListView<String> macroList = new ListView<>();
    private final ListView<Step> stepList = new ListView<>();
    private final TextField nameField = new TextField();
    private final Spinner<Integer> repeatSpinner = new Spinner<>(1, 100, 1);
    private final Label status = new Label("Ready");

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        this.stage = stage;
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
        try {
            db.init();
            GlobalScreen.registerNativeHook();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (NativeHookException e) {
            showError("Could not hook keyboard/mouse. On Mac, allow this app in Settings > Privacy > Accessibility.");
        }

        Button recordBtn = new Button("Record");
        Button deleteMacroBtn = new Button("Delete");
        Button importBtn = new Button("Import");
        Button exportBtn = new Button("Export");
        Button historyBtn = new Button("History");
        HBox topBar = new HBox(8, recordBtn, deleteMacroBtn, importBtn, exportBtn, historyBtn);

        VBox left = new VBox(8, new Label("Saved Macros"), macroList);
        left.setPrefWidth(200);

        Button upBtn = new Button("Up");
        Button downBtn = new Button("Down");
        Button removeBtn = new Button("Remove");
        Button editBtn = new Button("Edit Delay");
        Button addDelayBtn = new Button("Add Delay");
        Button saveBtn = new Button("Save");
        HBox editBar = new HBox(8, upBtn, downBtn, removeBtn, editBtn, addDelayBtn, saveBtn);

        nameField.setPromptText("Macro name");
        VBox center = new VBox(8, new Label("Name"), nameField, new Label("Steps"), stepList, editBar);

        Button playBtn = new Button("Play");
        HBox bottom = new HBox(8, new Label("Repeat"), repeatSpinner, playBtn, status);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(topBar);
        root.setLeft(left);
        root.setCenter(center);
        root.setBottom(bottom);
        BorderPane.setMargin(topBar, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(left, new Insets(0, 10, 0, 0));
        BorderPane.setMargin(bottom, new Insets(10, 0, 0, 0));

        macroList.getSelectionModel().selectedItemProperty().addListener((obs, old, name) -> {
            if (name != null) {
                openMacro(name);
            }
        });

        recordBtn.setOnAction(e -> startRecording());
        deleteMacroBtn.setOnAction(e -> deleteMacro());
        importBtn.setOnAction(e -> importMacro());
        exportBtn.setOnAction(e -> exportMacro());
        historyBtn.setOnAction(e -> showHistory());
        upBtn.setOnAction(e -> moveStep(-1));
        downBtn.setOnAction(e -> moveStep(1));
        removeBtn.setOnAction(e -> removeStep());
        editBtn.setOnAction(e -> editDelay());
        addDelayBtn.setOnAction(e -> addDelay());
        saveBtn.setOnAction(e -> saveMacro());
        playBtn.setOnAction(e -> playMacro());

        refreshMacros();
        stage.setTitle("Ghost in the Shell");
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    public void stop() {
        player.stop();
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            System.out.println("Could not unhook: " + e.getMessage());
        }
    }

    private void refreshMacros() {
        try {
            macroList.getItems().setAll(db.getMacroNames());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void openMacro(String name) {
        try {
            Macro macro = db.loadMacro(name);
            nameField.setText(macro.getName());
            stepList.getItems().setAll(macro.getSteps());
        } catch (SQLException | MacroException e) {
            showError(e.getMessage());
        }
    }

    private void startRecording() {
        status.setText("Recording... press F9 to stop");
        stage.setIconified(true);
        recorder.start(() -> Platform.runLater(() -> {
            stage.setIconified(false);
            macroList.getSelectionModel().clearSelection();
            nameField.clear();
            stepList.getItems().setAll(recorder.getSteps());
            status.setText("Recorded " + stepList.getItems().size() + " steps. Enter a name and Save.");
        }));
    }

    private void saveMacro() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a macro name");
            return;
        }
        try {
            db.saveMacro(new Macro(name, stepList.getItems()));
            refreshMacros();
            status.setText("Saved " + name);
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void deleteMacro() {
        String name = macroList.getSelectionModel().getSelectedItem();
        if (name == null) {
            showError("Select a macro first");
            return;
        }
        try {
            db.deleteMacro(name);
            nameField.clear();
            stepList.getItems().clear();
            refreshMacros();
            status.setText("Deleted " + name);
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void moveStep(int direction) {
        int i = stepList.getSelectionModel().getSelectedIndex();
        int j = i + direction;
        if (i < 0 || j < 0 || j >= stepList.getItems().size()) {
            return;
        }
        Step step = stepList.getItems().remove(i);
        stepList.getItems().add(j, step);
        stepList.getSelectionModel().select(j);
    }

    private void removeStep() {
        int i = stepList.getSelectionModel().getSelectedIndex();
        if (i >= 0) {
            stepList.getItems().remove(i);
        }
    }

    private Integer askMillis(String start) {
        TextInputDialog dialog = new TextInputDialog(start);
        dialog.setHeaderText("Delay in milliseconds");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }
        try {
            int millis = Integer.parseInt(result.get().trim());
            if (millis < 0) {
                throw new NumberFormatException();
            }
            return millis;
        } catch (NumberFormatException e) {
            showError("Enter a positive whole number");
            return null;
        }
    }

    private void editDelay() {
        Step step = stepList.getSelectionModel().getSelectedItem();
        if (!(step instanceof DelayStep)) {
            showError("Select a Wait step to edit");
            return;
        }
        DelayStep delay = (DelayStep) step;
        Integer millis = askMillis(String.valueOf(delay.getMillis()));
        if (millis != null) {
            delay.setMillis(millis);
            stepList.refresh();
        }
    }

    private void addDelay() {
        Integer millis = askMillis("500");
        if (millis != null) {
            int i = stepList.getSelectionModel().getSelectedIndex();
            stepList.getItems().add(i + 1, new DelayStep(millis));
        }
    }

    private void playMacro() {
        if (stepList.getItems().isEmpty()) {
            showError("Nothing to play");
            return;
        }
        String name = nameField.getText().trim().isEmpty() ? "unsaved" : nameField.getText().trim();
        Macro macro = new Macro(name, new ArrayList<>(stepList.getItems()));
        status.setText("Playing... press ESC to stop");
        stage.setIconified(true);
        player.play(macro, repeatSpinner.getValue(), (result, done) -> Platform.runLater(() -> {
            stage.setIconified(false);
            status.setText(result + " - " + done + " steps played");
            try {
                db.logRun(name, done, result);
            } catch (SQLException e) {
                showError("Database error: " + e.getMessage());
            }
        }));
    }

    private void exportMacro() {
        if (stepList.getItems().isEmpty()) {
            showError("Nothing to export");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(nameField.getText().trim() + ".ghost");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            MacroFile.save(new Macro(nameField.getText().trim(), stepList.getItems()), file);
            status.setText("Exported to " + file.getName());
        } catch (IOException e) {
            showError("Could not write file: " + e.getMessage());
        }
    }

    private void importMacro() {
        File file = new FileChooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Macro macro = MacroFile.load(file);
            macroList.getSelectionModel().clearSelection();
            nameField.setText(macro.getName());
            stepList.getItems().setAll(macro.getSteps());
            status.setText("Imported " + macro.getName() + ". Click Save to keep it.");
        } catch (IOException | MacroException e) {
            showError("Could not import: " + e.getMessage());
        }
    }

    private void showHistory() {
        try {
            StringBuilder text = new StringBuilder("STATISTICS\n");
            for (String line : db.getStats()) {
                text.append(line).append("\n");
            }
            text.append("\nRECENT RUNS\n");
            for (String line : db.getHistory()) {
                text.append(line).append("\n");
            }
            TextArea area = new TextArea(text.toString());
            area.setEditable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("History");
            alert.setHeaderText("Execution history");
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
