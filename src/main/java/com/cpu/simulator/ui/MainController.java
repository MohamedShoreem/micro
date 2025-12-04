package com.cpu.simulator.ui;

import com.cpu.simulator.core.CPUSimulator;
import com.cpu.simulator.model.Instruction;
import com.cpu.simulator.model.SimulatorConfig;
import com.cpu.simulator.tomasulo.ReorderBufferEntry;
import com.cpu.simulator.tomasulo.ReservationStation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.io.File;
import java.util.Optional;

/**
 * MainController handles the JavaFX UI interactions
 */
public class MainController {
    
    @FXML private Button loadButton;
    @FXML private Button stepButton;
    @FXML private Button runButton;
    @FXML private Button resetButton;
    @FXML private Button configButton; // New button
    @FXML private Label clockLabel;
    @FXML private Label pcLabel;
    
    // Tables for display
    @FXML private TableView<RegisterDisplay> registerTable;
    @FXML private TableView<RSDisplay> addSubTable;
    @FXML private TableView<RSDisplay> mulDivTable;
    @FXML private TableView<RSDisplay> loadTable;
    @FXML private TableView<RSDisplay> storeTable;
    @FXML private TableView<RSDisplay> integerTable; // New table
    @FXML private TableView<ROBDisplay> robTable;
    @FXML private ListView<String> instructionList;
    @FXML private TextArea logArea;
    
    private CPUSimulator simulator;
    private Timeline timeline;
    
    public MainController() {
        simulator = new CPUSimulator();
    }
    
    /**
     * Initialize the controller (called automatically by JavaFX)
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        
        // Setup timeline for continuous run
        timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (simulator.isRunning()) {
                handleStep();
            } else {
                timeline.stop();
                runButton.setText("Run");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        logMessage("CPU Simulator initialized");
        updateDisplay();
    }
    
    private void setupTableColumns() {
        // Register Table
        setupColumns(registerTable, "name", "value", "status");
        
        // RS Tables
        String[] rsCols = {"name", "busy", "op", "vj", "vk", "qj", "qk", "dest", "addr"};
        setupColumns(addSubTable, rsCols);
        setupColumns(mulDivTable, rsCols);
        setupColumns(loadTable, rsCols);
        setupColumns(storeTable, rsCols);
        if (integerTable != null) setupColumns(integerTable, rsCols);
        
        // ROB Table
        setupColumns(robTable, "entry", "state", "instruction", "destination", "value");
    }
    
    private <T> void setupColumns(TableView<T> table, String... properties) {
        if (table == null) return;
        table.getColumns().clear();
        for (String prop : properties) {
            TableColumn<T, String> col = new TableColumn<>(prop.toUpperCase());
            col.setCellValueFactory(new PropertyValueFactory<>(prop));
            table.getColumns().add(col);
        }
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
            
            // Initialize Integer Registers (R registers)
            simulator.getRegisterFile().writeInt(2, 0); // R2 = 0 (base address for memory access)
            
            // Initialize Floating-Point Registers (F registers)
            simulator.getRegisterFile().writeFloat(4, 5.0); // F4 = 5.0
            
            // Initialize memory with test values
            simulator.getMemory().writeDouble(0, 10.0);  // Memory[R2+0] = Memory[0] = 10.0
            simulator.getMemory().writeDouble(8, 20.0);  // Memory[R2+8] = Memory[8] = 20.0
            
            logMessage("Loaded program: " + file.getName());
            logMessage("Initial values set:");
            logMessage("  R2 = 0 (base address)");
            logMessage("  F4 = 5.0");
            logMessage("  Memory[0] = 10.0");
            logMessage("  Memory[8] = 20.0");
            logMessage("");
            logMessage("Expected final values:");
            logMessage("  F6 = 10.0 (from L.D F6, 0(R2))");
            logMessage("  F2 = 20.0 (from L.D F2, 8(R2))");
            logMessage("  F0 = 100.0 (20.0 * 5.0)");
            logMessage("  F8 = 10.0 (20.0 - 10.0)");
            logMessage("  F10 = 10.0 (100.0 / 10.0)");
            logMessage("  F6 = 30.0 (10.0 + 20.0) [overwrites initial F6]");
            logMessage("  Memory[8] = 30.0 (from S.D F6, 8(R2))");
            
            updateDisplay();
        }
    }
    
    @FXML
    private void handleConfigure() {
        Dialog<SimulatorConfig> dialog = new Dialog<>();
        dialog.setTitle("Configuration");
        dialog.setHeaderText("Configure Simulator Parameters");
        
        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField addLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.ADD_D)));
        TextField mulLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.MUL_D)));
        TextField divLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.DIV_D)));
        TextField loadLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.L_D)));
        
        TextField robSize = new TextField(String.valueOf(simulator.getConfig().getRobSize()));
        TextField cacheSize = new TextField(String.valueOf(simulator.getConfig().getCacheSize()));
        TextField blockSize = new TextField(String.valueOf(simulator.getConfig().getCacheBlockSize()));
        
        grid.add(new Label("ADD/SUB Execution Cycles:"), 0, 0); grid.add(addLat, 1, 0);
        grid.add(new Label("MUL Execution Cycles:"), 0, 1); grid.add(mulLat, 1, 1);
        grid.add(new Label("DIV Execution Cycles:"), 0, 2); grid.add(divLat, 1, 2);
        grid.add(new Label("LOAD/STORE Execution Cycles:"), 0, 3); grid.add(loadLat, 1, 3);
        
        grid.add(new Label("ROB Size:"), 0, 4); grid.add(robSize, 1, 4);
        grid.add(new Label("Cache Size (bytes):"), 0, 5); grid.add(cacheSize, 1, 5);
        grid.add(new Label("Block Size (bytes):"), 0, 6); grid.add(blockSize, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                SimulatorConfig config = simulator.getConfig();
                try {
                    config.setLatency(Instruction.Operation.ADD_D, Integer.parseInt(addLat.getText()));
                    config.setLatency(Instruction.Operation.SUB_D, Integer.parseInt(addLat.getText()));
                    config.setLatency(Instruction.Operation.MUL_D, Integer.parseInt(mulLat.getText()));
                    config.setLatency(Instruction.Operation.DIV_D, Integer.parseInt(divLat.getText()));
                    config.setLatency(Instruction.Operation.L_D, Integer.parseInt(loadLat.getText()));
                    
                    config.setRobSize(Integer.parseInt(robSize.getText()));
                    config.setCacheSize(Integer.parseInt(cacheSize.getText()));
                    config.setCacheBlockSize(Integer.parseInt(blockSize.getText()));
                    return config;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<SimulatorConfig> result = dialog.showAndWait();
        result.ifPresent(config -> {
            simulator.configure(config);
            logMessage("Configuration updated");
            updateDisplay();
        });
    }
    
    /**
     * Execute one clock cycle
     */
    @FXML
    private void handleStep() {
        simulator.executeCycle();
        // logMessage("Executed clock cycle " + simulator.getClockCycle());
        updateDisplay();
    }
    
    /**
     * Run simulation continuously
     */
    @FXML
    private void handleRun() {
        if (timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.stop();
            runButton.setText("Run");
            simulator.setRunning(false);
        } else {
            simulator.setRunning(true);
            timeline.play();
            runButton.setText("Pause");
        }
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
        
        // Update Registers
        ObservableList<RegisterDisplay> regs = FXCollections.observableArrayList();
        
        // Add Floating Point Registers (F0-F31)
        for (int i = 0; i < 32; i++) {
            double val = simulator.getRegisterFile().readFloat(i);
            Integer status = simulator.getRegisterStatus().getFloatStatus(i);
            regs.add(new RegisterDisplay("F" + i, val, status == null ? "" : "ROB" + status));
        }
        
        // Add Integer Registers (R0-R31)
        for (int i = 0; i < 32; i++) {
            double val = simulator.getRegisterFile().readInt(i);
            Integer status = simulator.getRegisterStatus().getIntStatus(i);
            regs.add(new RegisterDisplay("R" + i, val, status == null ? "" : "ROB" + status));
        }
        registerTable.setItems(regs);
        
        // Update RS Tables
        updateRSTable(addSubTable, simulator.getAddSubStations());
        updateRSTable(mulDivTable, simulator.getMulDivStations());
        updateRSTable(loadTable, simulator.getLoadStations());
        updateRSTable(storeTable, simulator.getStoreStations());
        if (integerTable != null) updateRSTable(integerTable, simulator.getIntegerStations());
        
        // Update ROB Table
        ObservableList<ROBDisplay> robs = FXCollections.observableArrayList();
        for (ReorderBufferEntry entry : simulator.getReorderBuffer()) {
            robs.add(new ROBDisplay(
                String.valueOf(entry.getEntryNumber()),
                entry.getState().toString(),
                entry.getInstruction() != null ? entry.getInstruction().toString() : "",
                entry.getInstruction() != null ? (entry.isFloat() ? "F" : "R") + entry.getDestination() : "",
                entry.isReady() ? String.format("%.2f", entry.getValue()) : ""
            ));
        }
        robTable.setItems(robs);
        
        updateInstructionList();
    }
    
    private void updateRSTable(TableView<RSDisplay> table, java.util.List<ReservationStation> stations) {
        if (table == null || stations == null) return;
        
        ObservableList<RSDisplay> list = FXCollections.observableArrayList();
        
        // Always show all reservation stations, even when not busy
        for (ReservationStation rs : stations) {
            list.add(new RSDisplay(
                rs.getName(),
                rs.isBusy() ? "Yes" : "No",
                rs.getOp() != null ? rs.getOp().toString() : "",
                rs.isBusy() ? String.format("%.2f", rs.getVj()) : "0.00",
                rs.isBusy() ? String.format("%.2f", rs.getVk()) : "0.00",
                rs.getQj() != null ? "ROB" + rs.getQj() : "",
                rs.getQk() != null ? "ROB" + rs.getQk() : "",
                rs.isBusy() ? String.valueOf(rs.getRobEntry()) : "",
                rs.isAddressReady() ? String.valueOf(rs.getAddress()) : ""
            ));
        }
        table.setItems(list);
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
        private double value;
        private String status;
        
        public RegisterDisplay(String name, double value, String status) {
            this.name = name;
            this.value = value;
            this.status = status;
        }
        
        public String getName() { return name; }
        public double getValue() { return value; }
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
        private String dest;
        private String addr;
        
        public RSDisplay(String name, String busy, String op, 
                        String vj, String vk, String qj, String qk, String dest, String addr) {
            this.name = name;
            this.busy = busy;
            this.op = op;
            this.vj = vj;
            this.vk = vk;
            this.qj = qj;
            this.qk = qk;
            this.dest = dest;
            this.addr = addr;
        }
        
        public String getName() { return name; }
        public String getBusy() { return busy; }
        public String getOp() { return op; }
        public String getVj() { return vj; }
        public String getVk() { return vk; }
        public String getQj() { return qj; }
        public String getQk() { return qk; }
        public String getDest() { return dest; }
        public String getAddr() { return addr; }
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
