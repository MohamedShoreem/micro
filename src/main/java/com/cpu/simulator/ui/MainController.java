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
    // @FXML private TableView<ROBDisplay> robTable;  // Removed - no ROB in architecture
    @FXML private ListView<String> instructionList;
    @FXML private TableView<TimingDisplay> timingTable;
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
        
        // Timing Table
        setupColumns(timingTable, "instruction", "issue", "executionComplete", "writeResult");
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
            
            String fileName = file.getName().toLowerCase();
            
            if (fileName.contains("test_case3") || fileName.contains("loop")) {
                // Initialize for Test Case 3 (Loop) - Branch taken test
                simulator.getRegisterFile().writeInt(1, 24); // R1 = 24 (DADDI->48, DSUBI->40, then counts down)
                simulator.getRegisterFile().writeInt(2, 0);  // R2 = 0 (loop until R1==0)
                simulator.getRegisterFile().writeFloat(2, 2.0); // F2 = 2.0 (multiplier)
                
                // Initialize memory array
                simulator.getMemory().writeDouble(8, 1.0);   // Memory[8] = 1.0
                simulator.getMemory().writeDouble(16, 2.0);  // Memory[16] = 2.0
                simulator.getMemory().writeDouble(24, 3.0);  // Memory[24] = 3.0
                simulator.getMemory().writeDouble(32, 4.0);  // Memory[32] = 4.0
                simulator.getMemory().writeDouble(40, 5.0);  // Memory[40] = 5.0
                simulator.getMemory().writeDouble(48, 6.0);  // Memory[48] = 6.0
                
                logMessage("Loaded program: " + file.getName());
                logMessage("Initial values set for LOOP test:");
                logMessage("  R1 = 24 (DADDI->48, then counts down by 8 each iteration)");
                logMessage("  R2 = 0 (loop exits when R1==R2)");
                logMessage("  F2 = 2.0 (multiplier)");
                logMessage("  Memory[8-48] = 1.0 to 6.0");
            } else {
                // Initialize for Test Case 1 & 2
                simulator.getRegisterFile().writeInt(2, 0); // R2 = 0 (base address for memory access)
                simulator.getRegisterFile().writeFloat(4, 5.0); // F4 = 5.0
                
                // Initialize memory with test values
                simulator.getMemory().writeDouble(0, 10.0);  // Memory[0] = 10.0
                simulator.getMemory().writeDouble(8, 20.0);  // Memory[8] = 20.0
                
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
            }
            
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
        
        // Execution latencies
        TextField addLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.ADD_D)));
        TextField mulLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.MUL_D)));
        TextField divLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.DIV_D)));
        TextField loadLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.L_D)));
        TextField branchLat = new TextField(String.valueOf(simulator.getConfig().getLatency(Instruction.Operation.BEQ)));
        
        // Cache parameters
        TextField cacheSize = new TextField(String.valueOf(simulator.getConfig().getCacheSize()));
        TextField blockSize = new TextField(String.valueOf(simulator.getConfig().getCacheBlockSize()));
        TextField cacheHitLat = new TextField(String.valueOf(simulator.getConfig().getCacheHitLatency()));
        TextField cacheMissPen = new TextField(String.valueOf(simulator.getConfig().getCacheMissPenalty()));
        
        // Reservation station sizes
        TextField numAddSub = new TextField(String.valueOf(simulator.getConfig().getNumAddSubStations()));
        TextField numMulDiv = new TextField(String.valueOf(simulator.getConfig().getNumMulDivStations()));
        TextField numLoad = new TextField(String.valueOf(simulator.getConfig().getNumLoadStations()));
        TextField numStore = new TextField(String.valueOf(simulator.getConfig().getNumStoreStations()));
        TextField numInteger = new TextField(String.valueOf(simulator.getConfig().getNumIntegerStations()));
        
        int row = 0;
        grid.add(new Label("ADD/SUB Execution Cycles:"), 0, row); grid.add(addLat, 1, row++);
        grid.add(new Label("MUL Execution Cycles:"), 0, row); grid.add(mulLat, 1, row++);
        grid.add(new Label("DIV Execution Cycles:"), 0, row); grid.add(divLat, 1, row++);
        grid.add(new Label("LOAD/STORE Execution Cycles:"), 0, row); grid.add(loadLat, 1, row++);
        grid.add(new Label("BRANCH Execution Cycles:"), 0, row); grid.add(branchLat, 1, row++);
        grid.add(new Label("Cache Size (bytes):"), 0, row); grid.add(cacheSize, 1, row++);
        grid.add(new Label("Block Size (4 or 8 bytes):"), 0, row); grid.add(blockSize, 1, row++);
        grid.add(new Label("Cache Hit Latency (cycles):"), 0, row); grid.add(cacheHitLat, 1, row++);
        grid.add(new Label("Cache Miss Penalty (cycles):"), 0, row); grid.add(cacheMissPen, 1, row++);
        grid.add(new Label("Add/Sub Stations:"), 0, row); grid.add(numAddSub, 1, row++);
        grid.add(new Label("Mul/Div Stations:"), 0, row); grid.add(numMulDiv, 1, row++);
        grid.add(new Label("Load Stations:"), 0, row); grid.add(numLoad, 1, row++);
        grid.add(new Label("Store Stations:"), 0, row); grid.add(numStore, 1, row++);
        grid.add(new Label("Integer Stations:"), 0, row); grid.add(numInteger, 1, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                SimulatorConfig config = simulator.getConfig();
                try {
                    // Set latencies for all operations
                    int addSubCycles = Integer.parseInt(addLat.getText());
                    config.setLatency(Instruction.Operation.ADD_D, addSubCycles);
                    config.setLatency(Instruction.Operation.SUB_D, addSubCycles);
                    config.setLatency(Instruction.Operation.ADD_S, addSubCycles);
                    config.setLatency(Instruction.Operation.SUB_S, addSubCycles);
                    
                    int mulCycles = Integer.parseInt(mulLat.getText());
                    config.setLatency(Instruction.Operation.MUL_D, mulCycles);
                    config.setLatency(Instruction.Operation.MUL_S, mulCycles);
                    
                    int divCycles = Integer.parseInt(divLat.getText());
                    config.setLatency(Instruction.Operation.DIV_D, divCycles);
                    config.setLatency(Instruction.Operation.DIV_S, divCycles);
                    
                    int loadStoreCycles = Integer.parseInt(loadLat.getText());
                    config.setLatency(Instruction.Operation.LW, loadStoreCycles);
                    config.setLatency(Instruction.Operation.SW, loadStoreCycles);
                    config.setLatency(Instruction.Operation.LD, loadStoreCycles);
                    config.setLatency(Instruction.Operation.SD, loadStoreCycles);
                    config.setLatency(Instruction.Operation.L_D, loadStoreCycles);
                    config.setLatency(Instruction.Operation.L_S, loadStoreCycles);
                    config.setLatency(Instruction.Operation.S_D, loadStoreCycles);
                    config.setLatency(Instruction.Operation.S_S, loadStoreCycles);
                    
                    int branchCycles = Integer.parseInt(branchLat.getText());
                    config.setLatency(Instruction.Operation.BEQ, branchCycles);
                    config.setLatency(Instruction.Operation.BNE, branchCycles);
                    
                    config.setLatency(Instruction.Operation.DADDI, 1);
                    config.setLatency(Instruction.Operation.DSUBI, 1);
                    
                    // Cache configuration
                    config.setCacheSize(Integer.parseInt(cacheSize.getText()));
                    config.setCacheBlockSize(Integer.parseInt(blockSize.getText()));
                    config.setCacheHitLatency(Integer.parseInt(cacheHitLat.getText()));
                    config.setCacheMissPenalty(Integer.parseInt(cacheMissPen.getText()));
                    
                    // Reservation station sizes
                    config.setNumAddSubStations(Integer.parseInt(numAddSub.getText()));
                    config.setNumMulDivStations(Integer.parseInt(numMulDiv.getText()));
                    config.setNumLoadStations(Integer.parseInt(numLoad.getText()));
                    config.setNumStoreStations(Integer.parseInt(numStore.getText()));
                    config.setNumIntegerStations(Integer.parseInt(numInteger.getText()));
                    
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
            String status = simulator.getRegisterStatus().getFloatStatus(i);
            regs.add(new RegisterDisplay("F" + i, val, status == null ? "" : status));
        }
        
        // Add Integer Registers (R0-R31)
        for (int i = 0; i < 32; i++) {
            double val = simulator.getRegisterFile().readInt(i);
            String status = simulator.getRegisterStatus().getIntStatus(i);
            regs.add(new RegisterDisplay("R" + i, val, status == null ? "" : status));
        }
        registerTable.setItems(regs);
        
        // Update RS Tables
        updateRSTable(addSubTable, simulator.getAddSubStations());
        updateRSTable(mulDivTable, simulator.getMulDivStations());
        updateRSTable(loadTable, simulator.getLoadStations());
        updateRSTable(storeTable, simulator.getStoreStations());
        if (integerTable != null) updateRSTable(integerTable, simulator.getIntegerStations());
        
        updateInstructionList();
        updateTimingTable();
    }
    
    private void updateRSTable(TableView<RSDisplay> table, java.util.List<ReservationStation> stations) {
        if (table == null || stations == null) return;
        
        ObservableList<RSDisplay> list = FXCollections.observableArrayList();
        
        // Always show all reservation stations, even when not busy
        for (ReservationStation rs : stations) {
            String dest = "";
            if (rs.isBusy() && rs.getDestination() >= 0) {
                dest = (rs.isDestFloat() ? "F" : "R") + rs.getDestination();
            }
            
            list.add(new RSDisplay(
                rs.getName(),
                rs.isBusy() ? "Yes" : "No",
                rs.getOp() != null ? rs.getOp().toString() : "",
                rs.isBusy() ? String.format("%.2f", rs.getVj()) : "0.00",
                rs.isBusy() ? String.format("%.2f", rs.getVk()) : "0.00",
                rs.getQj() != null ? rs.getQj() : "",
                rs.getQk() != null ? rs.getQk() : "",
                dest,
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
    
    private void updateTimingTable() {
        if (timingTable == null) return;
        
        ObservableList<TimingDisplay> list = FXCollections.observableArrayList();
        java.util.List<Instruction> timingInstructions = simulator.getTimingInstructions();
        java.util.List<int[]> timing = simulator.getInstructionTiming();
        
        if (timingInstructions == null || timing == null) {
            timingTable.setItems(list);
            return;
        }
        
        // Show all timing entries (includes re-executed instructions from branches)
        for (int i = 0; i < timing.size(); i++) {
            Instruction inst = timingInstructions.get(i);
            int[] times = timing.get(i);
            
            String instStr = inst.getRawInstruction();
            String issue = times[0] >= 0 ? String.valueOf(times[0]) : "";
            String execComplete = "";
            String writeRes = times[3] >= 0 ? String.valueOf(times[3]) : "";
            
            // Format execution as "start..end" or just "start" if same
            // times[1] = execution start, times[2] = execution complete
            if (times[1] >= 0 && times[2] >= 0) {
                // Both start and end recorded
                if (times[1] == times[2]) {
                    execComplete = String.valueOf(times[1]);
                } else {
                    execComplete = times[1] + ".." + times[2];
                }
            } else if (times[1] >= 0) {
                // Started but not complete yet
                execComplete = times[1] + "..";
            }
            
            list.add(new TimingDisplay(instStr, issue, execComplete, writeRes));
        }
        
        timingTable.setItems(list);
    }
}
