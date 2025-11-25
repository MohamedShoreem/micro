package com.cpu.simulator.ui;

import com.cpu.simulator.core.CPUSimulator;
import com.cpu.simulator.model.Instruction;
import com.cpu.simulator.tomasulo.ReorderBufferEntry;
import com.cpu.simulator.tomasulo.ReservationStation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;

/**
 * MainController handles the JavaFX UI interactions
 */
public class MainController {
    
    @FXML private Button loadButton;
    @FXML private Button stepButton;
    @FXML private Button runButton;
    @FXML private Button resetButton;
    @FXML private Label clockLabel;
    @FXML private Label pcLabel;
    
    // Tables for display
    @FXML private TableView<RegisterDisplay> registerTable;
    @FXML private TableView<RSDisplay> addSubTable;
    @FXML private TableView<RSDisplay> mulDivTable;
    @FXML private TableView<RSDisplay> loadTable;
    @FXML private TableView<RSDisplay> storeTable;
    @FXML private TableView<ROBDisplay> robTable;
    @FXML private ListView<String> instructionList;
    @FXML private TextArea logArea;
    
    private CPUSimulator simulator;
    
    public MainController() {
        simulator = new CPUSimulator();
    }
    
    /**
     * Initialize the controller (called automatically by JavaFX)
     */
    @FXML
    public void initialize() {
        // TODO: Set up table columns and bindings
        // This will be implemented to display the simulation state
        logMessage("CPU Simulator initialized");
        updateDisplay();
    }
    
    /**
     * Load instruction file
     */
    @FXML
    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load MIPS Instructions");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.asm")
        );
        
        File file = fileChooser.showOpenDialog(loadButton.getScene().getWindow());
        if (file != null) {
            simulator.loadProgram(file.getAbsolutePath());
            logMessage("Loaded program: " + file.getName());
            updateDisplay();
        }
    }
    
    /**
     * Execute one clock cycle
     */
    @FXML
    private void handleStep() {
        simulator.executeCycle();
        logMessage("Executed clock cycle " + simulator.getClockCycle());
        updateDisplay();
    }
    
    /**
     * Run simulation continuously (to be implemented)
     */
    @FXML
    private void handleRun() {
        simulator.setRunning(true);
        logMessage("Running simulation...");
        // TODO: Implement continuous execution with animation
        // This will require a Timeline or AnimationTimer
    }
    
    /**
     * Reset the simulation
     */
    @FXML
    private void handleReset() {
        simulator.reset();
        logMessage("Simulator reset");
        updateDisplay();
    }
    
    /**
     * Update all displays with current simulation state
     */
    private void updateDisplay() {
        // Update labels
        clockLabel.setText("Clock Cycle: " + simulator.getClockCycle());
        pcLabel.setText("PC: " + simulator.getProgramCounter());
        
        // TODO: Update tables with current state
        // - Register file values
        // - Reservation station states
        // - ROB entries
        // - Instruction list with highlighting
        
        updateInstructionList();
    }
    
    /**
     * Update instruction list display
     */
    private void updateInstructionList() {
        if (instructionList != null && simulator.getInstructionQueue() != null) {
            ObservableList<String> items = FXCollections.observableArrayList();
            for (int i = 0; i < simulator.getInstructionQueue().size(); i++) {
                Instruction inst = simulator.getInstructionQueue().get(i);
                String marker = (i == simulator.getProgramCounter()) ? "→ " : "  ";
                items.add(marker + i + ": " + inst.toString());
            }
            instructionList.setItems(items);
        }
    }
    
    /**
     * Log a message to the log area
     */
    private void logMessage(String message) {
        if (logArea != null) {
            logArea.appendText(message + "\n");
        }
        System.out.println(message);
    }
    
    // Inner classes for table display (JavaFX properties)
    
    public static class RegisterDisplay {
        private String name;
        private int value;
        private String status;
        
        public RegisterDisplay(String name, int value, String status) {
            this.name = name;
            this.value = value;
            this.status = status;
        }
        
        public String getName() { return name; }
        public int getValue() { return value; }
        public String getStatus() { return status; }
    }
    
    public static class RSDisplay {
        private String name;
        private String busy;
        private String op;
        private String vj;
        private String vk;
        private String qj;
        private String qk;
        
        public RSDisplay(String name, String busy, String op, 
                        String vj, String vk, String qj, String qk) {
            this.name = name;
            this.busy = busy;
            this.op = op;
            this.vj = vj;
            this.vk = vk;
            this.qj = qj;
            this.qk = qk;
        }
        
        public String getName() { return name; }
        public String getBusy() { return busy; }
        public String getOp() { return op; }
        public String getVj() { return vj; }
        public String getVk() { return vk; }
        public String getQj() { return qj; }
        public String getQk() { return qk; }
    }
    
    public static class ROBDisplay {
        private String entry;
        private String state;
        private String instruction;
        private String destination;
        private String value;
        
        public ROBDisplay(String entry, String state, String instruction,
                         String destination, String value) {
            this.entry = entry;
            this.state = state;
            this.instruction = instruction;
            this.destination = destination;
            this.value = value;
        }
        
        public String getEntry() { return entry; }
        public String getState() { return state; }
        public String getInstruction() { return instruction; }
        public String getDestination() { return destination; }
        public String getValue() { return value; }
    }
}
