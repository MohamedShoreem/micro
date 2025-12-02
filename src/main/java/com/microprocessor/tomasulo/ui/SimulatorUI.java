package com.microprocessor.tomasulo.ui;

import com.microprocessor.tomasulo.core.TomasuloSimulator;
import com.microprocessor.tomasulo.model.*;
import com.microprocessor.tomasulo.util.InstructionParser;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

public class SimulatorUI extends Application {

    private TomasuloSimulator simulator;
    private SimulatorConfig config;

    // UI Components
    private Label cycleLabel;
    private TableView<InstructionRow> instructionTable;
    private TableView<RSRow> addSubTable;
    private TableView<RSRow> mulDivTable;
    private TableView<RSRow> loadBufferTable;
    private TableView<RSRow> storeBufferTable;
    private TableView<ROBRow> robTable;
    private TableView<RegisterRow> registerTable;
    private Label cdbLabel;
    private TextArea logArea;

    // Control buttons
    private Button stepButton;
    private Button runButton;
    private Button resetButton;
    private Button loadButton;

    @Override
    public void start(Stage primaryStage) {
        config = new SimulatorConfig();
        simulator = new TomasuloSimulator(config);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Title and cycle counter
        VBox topBox = createTopSection();
        root.setTop(topBox);

        // Center: Tables
        TabPane tabPane = createTablesSection();
        root.setCenter(tabPane);

        // Right: Configuration panel
        VBox configPanel = createConfigPanel();
        root.setRight(configPanel);

        // Bottom: Control buttons
        HBox controlBox = createControlSection();
        root.setBottom(controlBox);

        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setTitle("Tomasulo Algorithm Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createTopSection() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label("Tomasulo Algorithm Simulator");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 18px;");

        cdbLabel = new Label("CDB: Idle");
        cdbLabel.setStyle("-fx-font-size: 14px;");

        box.getChildren().addAll(titleLabel, cycleLabel, cdbLabel);
        return box;
    }

    private TabPane createTablesSection() {
        TabPane tabPane = new TabPane();

        // Instructions Tab
        Tab instructionsTab = new Tab("Instructions");
        instructionsTab.setClosable(false);
        instructionsTab.setContent(createInstructionTable());

        // Reservation Stations Tab
        Tab rsTab = new Tab("Reservation Stations");
        rsTab.setClosable(false);
        rsTab.setContent(createReservationStationsTables());

        // ROB Tab
        Tab robTab = new Tab("Reorder Buffer");
        robTab.setClosable(false);
        robTab.setContent(createROBTable());

        // Register File Tab
        Tab regTab = new Tab("Register File");
        regTab.setClosable(false);
        regTab.setContent(createRegisterTable());

        // Log Tab
        Tab logTab = new Tab("Log");
        logTab.setClosable(false);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logTab.setContent(logArea);

        tabPane.getTabs().addAll(instructionsTab, rsTab, robTab, regTab, logTab);

        return tabPane;
    }

    private ScrollPane createInstructionTable() {
        instructionTable = new TableView<>();

        TableColumn<InstructionRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<InstructionRow, String> instCol = new TableColumn<>("Instruction");
        instCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        instCol.setPrefWidth(200);

        TableColumn<InstructionRow, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(new PropertyValueFactory<>("issue"));
        issueCol.setPrefWidth(60);

        TableColumn<InstructionRow, String> execStartCol = new TableColumn<>("Exec Start");
        execStartCol.setCellValueFactory(new PropertyValueFactory<>("execStart"));
        execStartCol.setPrefWidth(80);

        TableColumn<InstructionRow, String> execEndCol = new TableColumn<>("Exec End");
        execEndCol.setCellValueFactory(new PropertyValueFactory<>("execEnd"));
        execEndCol.setPrefWidth(80);

        TableColumn<InstructionRow, String> writeCol = new TableColumn<>("Write");
        writeCol.setCellValueFactory(new PropertyValueFactory<>("write"));
        writeCol.setPrefWidth(60);

        TableColumn<InstructionRow, String> commitCol = new TableColumn<>("Commit");
        commitCol.setCellValueFactory(new PropertyValueFactory<>("commit"));
        commitCol.setPrefWidth(60);

        instructionTable.getColumns().addAll(idCol, instCol, issueCol, execStartCol,
                execEndCol, writeCol, commitCol);

        ScrollPane scrollPane = new ScrollPane(instructionTable);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private VBox createReservationStationsTables() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        Label addLabel = new Label("Add/Sub Reservation Stations");
        addLabel.setStyle("-fx-font-weight: bold;");
        addSubTable = createRSTable();

        Label mulLabel = new Label("Mul/Div Reservation Stations");
        mulLabel.setStyle("-fx-font-weight: bold;");
        mulDivTable = createRSTable();

        Label loadLabel = new Label("Load Buffers");
        loadLabel.setStyle("-fx-font-weight: bold;");
        loadBufferTable = createRSTable();

        Label storeLabel = new Label("Store Buffers");
        storeLabel.setStyle("-fx-font-weight: bold;");
        storeBufferTable = createRSTable();

        box.getChildren().addAll(addLabel, addSubTable, mulLabel, mulDivTable,
                loadLabel, loadBufferTable, storeLabel, storeBufferTable);

        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setFitToWidth(true);
        return box;
    }

    private TableView<RSRow> createRSTable() {
        TableView<RSRow> table = new TableView<>();
        table.setPrefHeight(150);

        TableColumn<RSRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(80);

        TableColumn<RSRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));
        busyCol.setPrefWidth(60);

        TableColumn<RSRow, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(new PropertyValueFactory<>("op"));
        opCol.setPrefWidth(80);

        TableColumn<RSRow, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(new PropertyValueFactory<>("vj"));
        vjCol.setPrefWidth(80);

        TableColumn<RSRow, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(new PropertyValueFactory<>("vk"));
        vkCol.setPrefWidth(80);

        TableColumn<RSRow, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(new PropertyValueFactory<>("qj"));
        qjCol.setPrefWidth(80);

        TableColumn<RSRow, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(new PropertyValueFactory<>("qk"));
        qkCol.setPrefWidth(80);

        TableColumn<RSRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setPrefWidth(60);

        table.getColumns().addAll(nameCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol, timeCol);

        return table;
    }

    private ScrollPane createROBTable() {
        robTable = new TableView<>();

        TableColumn<ROBRow, String> entryCol = new TableColumn<>("Entry");
        entryCol.setCellValueFactory(new PropertyValueFactory<>("entry"));
        entryCol.setPrefWidth(80);

        TableColumn<ROBRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));
        busyCol.setPrefWidth(60);

        TableColumn<ROBRow, String> instCol = new TableColumn<>("Instruction");
        instCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        instCol.setPrefWidth(100);

        TableColumn<ROBRow, String> destCol = new TableColumn<>("Destination");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        destCol.setPrefWidth(100);

        TableColumn<ROBRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(100);

        TableColumn<ROBRow, String> readyCol = new TableColumn<>("Ready");
        readyCol.setCellValueFactory(new PropertyValueFactory<>("ready"));
        readyCol.setPrefWidth(60);

        robTable.getColumns().addAll(entryCol, busyCol, instCol, destCol, valueCol, readyCol);

        ScrollPane scrollPane = new ScrollPane(robTable);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private ScrollPane createRegisterTable() {
        registerTable = new TableView<>();

        TableColumn<RegisterRow, String> regCol = new TableColumn<>("Register");
        regCol.setCellValueFactory(new PropertyValueFactory<>("register"));
        regCol.setPrefWidth(100);

        TableColumn<RegisterRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(100);

        TableColumn<RegisterRow, String> qiCol = new TableColumn<>("Qi");
        qiCol.setCellValueFactory(new PropertyValueFactory<>("qi"));
        qiCol.setPrefWidth(100);

        registerTable.getColumns().addAll(regCol, valueCol, qiCol);

        ScrollPane scrollPane = new ScrollPane(registerTable);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private VBox createConfigPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setPrefWidth(250);

        Label configLabel = new Label("Configuration");
        configLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Latency configuration
        Label latencyLabel = new Label("Latencies:");
        latencyLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Integer> addLatency = createSpinner(1, 100, config.getAddLatency());
        Spinner<Integer> mulLatency = createSpinner(1, 100, config.getMulLatency());
        Spinner<Integer> divLatency = createSpinner(1, 100, config.getDivLatency());
        Spinner<Integer> loadLatency = createSpinner(1, 100, config.getLoadLatency());

        Button applyButton = new Button("Apply Configuration");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(e -> {
            config.setAddLatency(addLatency.getValue());
            config.setMulLatency(mulLatency.getValue());
            config.setDivLatency(divLatency.getValue());
            config.setLoadLatency(loadLatency.getValue());

            simulator = new TomasuloSimulator(config);
            updateUI();
            log("Configuration updated");
        });

        box.getChildren().addAll(
                configLabel,
                new Separator(),
                latencyLabel,
                new Label("Add/Sub:"), addLatency,
                new Label("Mul:"), mulLatency,
                new Label("Div:"), divLatency,
                new Label("Load:"), loadLatency,
                new Separator(),
                applyButton);

        return box;
    }

    private Spinner<Integer> createSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private HBox createControlSection() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(10, 0, 0, 0));
        box.setAlignment(Pos.CENTER);

        loadButton = new Button("Load Instructions");
        loadButton.setOnAction(e -> loadInstructions());

        stepButton = new Button("Step");
        stepButton.setOnAction(e -> stepSimulation());
        stepButton.setDisable(true);

        runButton = new Button("Run");
        runButton.setOnAction(e -> runSimulation());
        runButton.setDisable(true);

        resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetSimulation());
        resetButton.setDisable(true);

        box.getChildren().addAll(loadButton, stepButton, runButton, resetButton);

        return box;
    }

    private void loadInstructions() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Instruction File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                List<Instruction> instructions = InstructionParser.parseFile(file.getAbsolutePath());
                simulator.loadInstructions(instructions);
                updateUI();

                stepButton.setDisable(false);
                runButton.setDisable(false);
                resetButton.setDisable(false);

                log("Loaded " + instructions.size() + " instructions from " + file.getName());
            } catch (Exception ex) {
                showError("Error loading file: " + ex.getMessage());
            }
        }
    }

    private void stepSimulation() {
        boolean hasMore = simulator.executeCycle();
        updateUI();

        if (!hasMore) {
            stepButton.setDisable(true);
            runButton.setDisable(true);
            log("Simulation completed!");
        }
    }

    private void runSimulation() {
        stepButton.setDisable(true);
        runButton.setDisable(true);

        new Thread(() -> {
            while (simulator.executeCycle()) {
                javafx.application.Platform.runLater(this::updateUI);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }

            javafx.application.Platform.runLater(() -> {
                updateUI();
                log("Simulation completed!");
            });
        }).start();
    }

    private void resetSimulation() {
        simulator.reset();
        updateUI();

        stepButton.setDisable(false);
        runButton.setDisable(false);

        log("Simulation reset");
    }

    private void updateUI() {
        cycleLabel.setText("Cycle: " + simulator.getCurrentCycle());

        if (simulator.getCdbValue() != null) {
            cdbLabel.setText("CDB: " + simulator.getCdbValue() + " = " +
                    String.format("%.2f", simulator.getCdbData()));
        } else {
            cdbLabel.setText("CDB: Idle");
        }

        updateInstructionTable();
        updateReservationStationTables();
        updateROBTable();
        updateRegisterTable();
    }

    private void updateInstructionTable() {
        instructionTable.getItems().clear();

        for (Instruction inst : simulator.getInstructions()) {
            instructionTable.getItems().add(new InstructionRow(inst));
        }
    }

    private void updateReservationStationTables() {
        addSubTable.getItems().clear();
        for (ReservationStation rs : simulator.getAddSubStations()) {
            addSubTable.getItems().add(new RSRow(rs));
        }

        mulDivTable.getItems().clear();
        for (ReservationStation rs : simulator.getMulDivStations()) {
            mulDivTable.getItems().add(new RSRow(rs));
        }

        loadBufferTable.getItems().clear();
        for (ReservationStation rs : simulator.getLoadBuffers()) {
            loadBufferTable.getItems().add(new RSRow(rs));
        }

        storeBufferTable.getItems().clear();
        for (ReservationStation rs : simulator.getStoreBuffers()) {
            storeBufferTable.getItems().add(new RSRow(rs));
        }
    }

    private void updateROBTable() {
        robTable.getItems().clear();

        for (ROBEntry entry : simulator.getROB()) {
            robTable.getItems().add(new ROBRow(entry));
        }
    }

    private void updateRegisterTable() {
        registerTable.getItems().clear();

        RegisterFile rf = simulator.getRegisterFile();

        // Show only first 8 of each type for brevity
        for (int i = 0; i < 8; i++) {
            String reg = "F" + i;
            registerTable.getItems().add(new RegisterRow(
                    reg,
                    String.format("%.2f", rf.getValue(reg)),
                    rf.getQi(reg) != null ? rf.getQi(reg) : "-"));
        }

        for (int i = 0; i < 8; i++) {
            String reg = "R" + i;
            registerTable.getItems().add(new RegisterRow(
                    reg,
                    String.format("%.2f", rf.getValue(reg)),
                    rf.getQi(reg) != null ? rf.getQi(reg) : "-"));
        }
    }

    private void log(String message) {
        logArea.appendText("[Cycle " + simulator.getCurrentCycle() + "] " + message + "\n");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data classes for TableView
    public static class InstructionRow {
        private final int id;
        private final String instruction;
        private final String issue;
        private final String execStart;
        private final String execEnd;
        private final String write;
        private final String commit;

        public InstructionRow(Instruction inst) {
            this.id = inst.getId();
            this.instruction = inst.toString();
            this.issue = inst.getIssueTime() >= 0 ? String.valueOf(inst.getIssueTime()) : "-";
            this.execStart = inst.getExecuteStartTime() >= 0 ? String.valueOf(inst.getExecuteStartTime()) : "-";
            this.execEnd = inst.getExecuteEndTime() >= 0 ? String.valueOf(inst.getExecuteEndTime()) : "-";
            this.write = inst.getWriteResultTime() >= 0 ? String.valueOf(inst.getWriteResultTime()) : "-";
            this.commit = inst.getCommitTime() >= 0 ? String.valueOf(inst.getCommitTime()) : "-";
        }

        public int getId() {
            return id;
        }

        public String getInstruction() {
            return instruction;
        }

        public String getIssue() {
            return issue;
        }

        public String getExecStart() {
            return execStart;
        }

        public String getExecEnd() {
            return execEnd;
        }

        public String getWrite() {
            return write;
        }

        public String getCommit() {
            return commit;
        }
    }

    public static class RSRow {
        private final String name;
        private final String busy;
        private final String op;
        private final String vj;
        private final String vk;
        private final String qj;
        private final String qk;
        private final String time;

        public RSRow(ReservationStation rs) {
            this.name = rs.getName();
            this.busy = rs.isBusy() ? "Yes" : "No";
            this.op = rs.getOp() != null ? rs.getOp() : "-";
            this.vj = rs.isBusy() && rs.getQj() == null ? String.format("%.2f", rs.getVj()) : "-";
            this.vk = rs.isBusy() && rs.getQk() == null ? String.format("%.2f", rs.getVk()) : "-";
            this.qj = rs.getQj() != null ? rs.getQj() : "-";
            this.qk = rs.getQk() != null ? rs.getQk() : "-";
            this.time = rs.isBusy() ? String.valueOf(rs.getTimeRemaining()) : "-";
        }

        public String getName() {
            return name;
        }

        public String getBusy() {
            return busy;
        }

        public String getOp() {
            return op;
        }

        public String getVj() {
            return vj;
        }

        public String getVk() {
            return vk;
        }

        public String getQj() {
            return qj;
        }

        public String getQk() {
            return qk;
        }

        public String getTime() {
            return time;
        }
    }

    public static class ROBRow {
        private final String entry;
        private final String busy;
        private final String instruction;
        private final String destination;
        private final String value;
        private final String ready;

        public ROBRow(ROBEntry rob) {
            this.entry = rob.getName();
            this.busy = rob.isBusy() ? "Yes" : "No";
            this.instruction = rob.getInstructionType() != null ? rob.getInstructionType().getMnemonic() : "-";
            this.destination = rob.getDestination() != null ? rob.getDestination() : "-";
            this.value = rob.isBusy() ? String.format("%.2f", rob.getValue()) : "-";
            this.ready = rob.isBusy() ? (rob.isReady() ? "Yes" : "No") : "-";
        }

        public String getEntry() {
            return entry;
        }

        public String getBusy() {
            return busy;
        }

        public String getInstruction() {
            return instruction;
        }

        public String getDestination() {
            return destination;
        }

        public String getValue() {
            return value;
        }

        public String getReady() {
            return ready;
        }
    }

    public static class RegisterRow {
        private final String register;
        private final String value;
        private final String qi;

        public RegisterRow(String register, String value, String qi) {
            this.register = register;
            this.value = value;
            this.qi = qi;
        }

        public String getRegister() {
            return register;
        }

        public String getValue() {
            return value;
        }

        public String getQi() {
            return qi;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
