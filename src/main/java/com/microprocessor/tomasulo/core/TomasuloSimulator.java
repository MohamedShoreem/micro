package com.microprocessor.tomasulo.core;

import com.microprocessor.tomasulo.model.*;
import java.util.*;

public class TomasuloSimulator {
    private final SimulatorConfig config;
    private final RegisterFile registerFile;
    private final Cache cache;

    // Instruction queue
    private final List<Instruction> instructionQueue;
    private int pc = 0;

    // Reservation Stations
    private final List<ReservationStation> addSubStations;
    private final List<ReservationStation> mulDivStations;
    private final List<ReservationStation> loadBuffers;
    private final List<ReservationStation> storeBuffers;

    // Reorder Buffer
    private final List<ROBEntry> rob;
    private int robHead = 0;
    private int robTail = 0;

    // Cycle counter
    private int currentCycle = 0;

    // CDB
    private String cdbValue = null;
    private double cdbData = 0;

    public TomasuloSimulator(SimulatorConfig config) {
        this.config = config;
        this.registerFile = new RegisterFile();
        this.cache = new Cache(
                config.getBlockSize(),
                config.getCacheSize(),
                config.getCacheHitLatency(),
                config.getCacheMissPenalty());

        this.instructionQueue = new ArrayList<>();
        this.addSubStations = new ArrayList<>();
        this.mulDivStations = new ArrayList<>();
        this.loadBuffers = new ArrayList<>();
        this.storeBuffers = new ArrayList<>();
        this.rob = new ArrayList<>();

        initializeHardware();
    }

    private void initializeHardware() {
        // Create Add/Sub reservation stations
        for (int i = 0; i < config.getAddRSSize(); i++) {
            addSubStations.add(new ReservationStation("Add" + (i + 1), OperationType.FP_ADD));
        }

        // Create Mul/Div reservation stations
        for (int i = 0; i < config.getMulRSSize(); i++) {
            mulDivStations.add(new ReservationStation("Mul" + (i + 1), OperationType.FP_MUL));
        }

        // Create Load buffers
        for (int i = 0; i < config.getLoadBufferSize(); i++) {
            loadBuffers.add(new ReservationStation("Load" + (i + 1), OperationType.LOAD));
        }

        // Create Store buffers
        for (int i = 0; i < config.getStoreBufferSize(); i++) {
            storeBuffers.add(new ReservationStation("Store" + (i + 1), OperationType.STORE));
        }

        // Create ROB entries
        for (int i = 0; i < config.getRobSize(); i++) {
            rob.add(new ROBEntry(i + 1));
        }
    }

    public void loadInstructions(List<Instruction> instructions) {
        this.instructionQueue.clear();
        this.instructionQueue.addAll(instructions);
        reset();
    }

    public void reset() {
        currentCycle = 0;
        pc = 0;
        robHead = 0;
        robTail = 0;
        cdbValue = null;

        for (ReservationStation rs : addSubStations)
            rs.clear();
        for (ReservationStation rs : mulDivStations)
            rs.clear();
        for (ReservationStation rs : loadBuffers)
            rs.clear();
        for (ReservationStation rs : storeBuffers)
            rs.clear();
        for (ROBEntry entry : rob)
            entry.clear();
    }

    public boolean executeCycle() {
        currentCycle++;
        cdbValue = null;

        // 1. Commit stage
        commit();

        // 2. Write Result stage (CDB broadcast)
        writeResult();

        // 3. Execute stage
        execute();

        // 4. Issue stage
        issue();

        // Check if done
        return !isDone();
    }

    private void issue() {
        if (pc >= instructionQueue.size()) {
            return;
        }

        // Check if ROB has space
        if (rob.get(robTail).isBusy()) {
            return; // ROB full
        }

        Instruction inst = instructionQueue.get(pc);
        OperationType opType = inst.getType().getOperationType();

        // Find available reservation station
        ReservationStation rs = findAvailableRS(opType);
        if (rs == null) {
            return; // No available RS
        }

        // Allocate ROB entry
        ROBEntry robEntry = rob.get(robTail);
        robEntry.setBusy(true);
        robEntry.setInstructionType(inst.getType());
        robEntry.setDestination(inst.getDestination());
        robEntry.setReady(false);
        robEntry.setInstruction(inst);

        // Configure reservation station
        rs.setBusy(true);
        rs.setOp(inst.getType().getMnemonic());
        rs.setInstruction(inst);
        rs.setRobEntry(robTail);

        // Handle operands based on instruction type
        switch (opType) {
            case FP_ADD:
            case FP_MUL:
            case FP_DIV:
                setupArithmeticOperands(rs, inst);
                break;
            case INTEGER:
                setupIntegerOperands(rs, inst);
                break;
            case LOAD:
                setupLoadOperands(rs, inst);
                break;
            case STORE:
                setupStoreOperands(rs, inst);
                break;
            case BRANCH:
                setupBranchOperands(rs, inst);
                break;
        }

        // Update register file Qi for destination
        if (opType != OperationType.STORE && opType != OperationType.BRANCH) {
            registerFile.setQi(inst.getDestination(), robEntry.getName());
        }

        inst.setIssueTime(currentCycle);

        // Advance ROB tail and PC
        robTail = (robTail + 1) % config.getRobSize();
        pc++;
    }

    private void setupArithmeticOperands(ReservationStation rs, Instruction inst) {
        // Source 1
        String qi1 = registerFile.getQi(inst.getSource1());
        if (qi1 != null) {
            rs.setQj(qi1);
        } else {
            rs.setVj(registerFile.getValue(inst.getSource1()));
        }

        // Source 2
        String qi2 = registerFile.getQi(inst.getSource2());
        if (qi2 != null) {
            rs.setQk(qi2);
        } else {
            rs.setVk(registerFile.getValue(inst.getSource2()));
        }
    }

    private void setupIntegerOperands(ReservationStation rs, Instruction inst) {
        // Source register
        String qi = registerFile.getQi(inst.getSource1());
        if (qi != null) {
            rs.setQj(qi);
        } else {
            rs.setVj(registerFile.getValue(inst.getSource1()));
        }

        // Immediate value
        rs.setVk(inst.getImmediate());
    }

    private void setupLoadOperands(ReservationStation rs, Instruction inst) {
        // Base register
        String qi = registerFile.getQi(inst.getSource1());
        if (qi != null) {
            rs.setQj(qi);
        } else {
            rs.setVj(registerFile.getValue(inst.getSource1()));
        }

        // Offset
        rs.setVk(inst.getImmediate());
    }

    private void setupStoreOperands(ReservationStation rs, Instruction inst) {
        // Base register for address
        String qi1 = registerFile.getQi(inst.getSource1());
        if (qi1 != null) {
            rs.setQj(qi1);
        } else {
            rs.setVj(registerFile.getValue(inst.getSource1()));
        }

        // Value to store
        String qi2 = registerFile.getQi(inst.getSource2());
        if (qi2 != null) {
            rs.setQk(qi2);
        } else {
            rs.setVk(registerFile.getValue(inst.getSource2()));
        }
    }

    private void setupBranchOperands(ReservationStation rs, Instruction inst) {
        // Source 1
        String qi1 = registerFile.getQi(inst.getSource1());
        if (qi1 != null) {
            rs.setQj(qi1);
        } else {
            rs.setVj(registerFile.getValue(inst.getSource1()));
        }

        // Source 2
        String qi2 = registerFile.getQi(inst.getSource2());
        if (qi2 != null) {
            rs.setQk(qi2);
        } else {
            rs.setVk(registerFile.getValue(inst.getSource2()));
        }
    }

    private void execute() {
        List<ReservationStation> allStations = new ArrayList<>();
        allStations.addAll(addSubStations);
        allStations.addAll(mulDivStations);
        allStations.addAll(loadBuffers);
        allStations.addAll(storeBuffers);

        for (ReservationStation rs : allStations) {
            if (!rs.isBusy())
                continue;

            // Check if ready to execute
            if (rs.isReady() && rs.getTimeRemaining() == 0) {
                // Start execution
                Instruction inst = rs.getInstruction();
                if (inst.getExecuteStartTime() == -1) {
                    inst.setExecuteStartTime(currentCycle);

                    int latency = config.getLatencyForType(rs.getType());

                    // For loads, check cache
                    if (rs.getType() == OperationType.LOAD) {
                        int address = (int) (rs.getVj() + rs.getVk());
                        Cache.AccessResult result = cache.access(address, true);
                        latency = result.getLatency();
                    }

                    rs.setTimeRemaining(latency);
                }
            }

            // Decrement time if executing
            if (rs.getTimeRemaining() > 0) {
                rs.decrementTime();

                // Mark execution end time
                if (rs.getTimeRemaining() == 0) {
                    rs.getInstruction().setExecuteEndTime(currentCycle);
                }
            }
        }
    }

    private void writeResult() {
        // Find instruction ready to write (execution complete, not yet written)
        List<ReservationStation> allStations = new ArrayList<>();
        allStations.addAll(addSubStations);
        allStations.addAll(mulDivStations);
        allStations.addAll(loadBuffers);
        allStations.addAll(storeBuffers);

        List<ReservationStation> readyToWrite = new ArrayList<>();
        for (ReservationStation rs : allStations) {
            if (rs.isBusy() && rs.getTimeRemaining() == 0 &&
                    rs.getInstruction().getWriteResultTime() == -1) {
                readyToWrite.add(rs);
            }
        }

        if (readyToWrite.isEmpty()) {
            return;
        }

        // CDB conflict resolution: pick first one (can be customized)
        ReservationStation selected = readyToWrite.get(0);
        Instruction inst = selected.getInstruction();
        ROBEntry robEntry = rob.get(selected.getRobEntry());

        // Compute result
        double result = computeResult(selected);

        // Broadcast on CDB
        cdbValue = robEntry.getName();
        cdbData = result;

        // Update ROB
        robEntry.setValue(result);
        robEntry.setReady(true);

        // Update all waiting RS
        updateReservationStations(cdbValue, cdbData);

        // Clear the reservation station
        inst.setWriteResultTime(currentCycle);
        selected.clear();
    }

    private double computeResult(ReservationStation rs) {
        Instruction inst = rs.getInstruction();

        switch (inst.getType()) {
            case ADD_D:
                return rs.getVj() + rs.getVk();
            case SUB_D:
                return rs.getVj() - rs.getVk();
            case MUL_D:
                return rs.getVj() * rs.getVk();
            case DIV_D:
                return rs.getVj() / rs.getVk();
            case ADDI:
                return rs.getVj() + rs.getVk();
            case SUBI:
                return rs.getVj() - rs.getVk();
            case LW:
            case LD:
            case L_S:
            case L_D:
                int address = (int) (rs.getVj() + rs.getVk());
                return cache.readMemory(address);
            default:
                return 0;
        }
    }

    private void updateReservationStations(String robName, double value) {
        List<ReservationStation> allStations = new ArrayList<>();
        allStations.addAll(addSubStations);
        allStations.addAll(mulDivStations);
        allStations.addAll(loadBuffers);
        allStations.addAll(storeBuffers);

        for (ReservationStation rs : allStations) {
            if (!rs.isBusy())
                continue;

            if (robName.equals(rs.getQj())) {
                rs.setVj(value);
                rs.setQj(null);
            }

            if (robName.equals(rs.getQk())) {
                rs.setVk(value);
                rs.setQk(null);
            }
        }
    }

    private void commit() {
        if (robHead == robTail && !rob.get(robHead).isBusy()) {
            return; // ROB empty
        }

        ROBEntry entry = rob.get(robHead);

        if (!entry.isBusy() || !entry.isReady()) {
            return; // Not ready to commit
        }

        Instruction inst = entry.getInstruction();

        // Perform commit based on instruction type
        switch (entry.getInstructionType().getOperationType()) {
            case STORE:
                // Write to memory
                int address = (int) (inst.getImmediate() +
                        registerFile.getValue(inst.getSource1()));
                cache.writeMemory(address, (int) entry.getValue());
                break;

            case BRANCH:
                // Handle branch (not predicted, so already handled)
                break;

            default:
                // Write to register file
                registerFile.setValue(entry.getDestination(), entry.getValue());

                // Clear Qi if it matches this ROB entry
                if (entry.getName().equals(registerFile.getQi(entry.getDestination()))) {
                    registerFile.clearQi(entry.getDestination());
                }
                break;
        }

        inst.setCommitTime(currentCycle);

        // Free ROB entry
        entry.clear();
        robHead = (robHead + 1) % config.getRobSize();
    }

    private ReservationStation findAvailableRS(OperationType type) {
        List<ReservationStation> stations;

        switch (type) {
            case FP_ADD:
                stations = addSubStations;
                break;
            case FP_MUL:
            case FP_DIV:
                stations = mulDivStations;
                break;
            case LOAD:
                stations = loadBuffers;
                break;
            case STORE:
                stations = storeBuffers;
                break;
            case INTEGER:
                stations = addSubStations;
                break;
            case BRANCH:
                stations = addSubStations;
                break;
            default:
                return null;
        }

        for (ReservationStation rs : stations) {
            if (!rs.isBusy()) {
                return rs;
            }
        }

        return null;
    }

    private boolean isDone() {
        // Check if all instructions are committed
        for (Instruction inst : instructionQueue) {
            if (inst.getCommitTime() == -1) {
                return false;
            }
        }
        return true;
    }

    // Getters for UI
    public int getCurrentCycle() {
        return currentCycle;
    }

    public List<Instruction> getInstructions() {
        return instructionQueue;
    }

    public List<ReservationStation> getAddSubStations() {
        return addSubStations;
    }

    public List<ReservationStation> getMulDivStations() {
        return mulDivStations;
    }

    public List<ReservationStation> getLoadBuffers() {
        return loadBuffers;
    }

    public List<ReservationStation> getStoreBuffers() {
        return storeBuffers;
    }

    public List<ROBEntry> getROB() {
        return rob;
    }

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    public Cache getCache() {
        return cache;
    }

    public String getCdbValue() {
        return cdbValue;
    }

    public double getCdbData() {
        return cdbData;
    }
}
