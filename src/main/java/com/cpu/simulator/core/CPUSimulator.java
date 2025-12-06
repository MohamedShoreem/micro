package com.cpu.simulator.core;

import com.cpu.simulator.model.*;
import com.cpu.simulator.tomasulo.*;
import com.cpu.simulator.parser.InstructionParser;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CPUSimulator is the main class that coordinates the CPU simulation
 * using Tomasulo's algorithm. It manages clock cycles and pipeline stages.
 */
public class CPUSimulator {
    // Hardware components
    private RegisterFile registerFile;
    private Memory memory;
    private Cache cache;
    private SimulatorConfig config;
    
    // Tomasulo components
    private List<ReservationStation> addSubStations;  // Addition/Subtraction units
    private List<ReservationStation> mulDivStations;  // Multiplication/Division units
    private List<ReservationStation> loadStations;    // Load units
    private List<ReservationStation> storeStations;   // Store units
    private List<ReservationStation> integerStations; // Integer units (DADDI, DSUBI, etc.)
    
    private RegisterStatus registerStatus;
    
    // Instruction queue
    private List<Instruction> instructionQueue;
    private int programCounter;
    
    // Simulation state
    private int clockCycle;
    private boolean running;
    private boolean branchPending;  // True when a branch is executing, blocks issue
    
    // Instruction timing tracking: [issue, executionStart, executionEnd, writeResult]
    private List<int[]> instructionTiming;
    private List<Instruction> timingInstructions; // Maps timing entries to instructions
    
    public CPUSimulator() {
        registerFile = new RegisterFile();
        memory = new Memory();
        cache = new Cache(memory);
        
        // Initialize Tomasulo structures
        addSubStations = new ArrayList<>();
        mulDivStations = new ArrayList<>();
        loadStations = new ArrayList<>();
        storeStations = new ArrayList<>();
        integerStations = new ArrayList<>();
        
        registerStatus = new RegisterStatus();
        instructionQueue = new ArrayList<>();
        
        // Ensure config is initialized before reset
        config = new SimulatorConfig();
        
        reset();
    }
    
    public void configure(SimulatorConfig config) {
        this.config = config;
        
        // Re-initialize stations based on config
        addSubStations.clear();
        for (int i = 0; i < config.getNumAddSubStations(); i++) addSubStations.add(new ReservationStation("Add" + (i + 1), ReservationStation.Type.ADD_SUB));
        
        mulDivStations.clear();
        for (int i = 0; i < config.getNumMulDivStations(); i++) mulDivStations.add(new ReservationStation("Mul" + (i + 1), ReservationStation.Type.MUL_DIV));
        
        loadStations.clear();
        for (int i = 0; i < config.getNumLoadStations(); i++) loadStations.add(new ReservationStation("Load" + (i + 1), ReservationStation.Type.LOAD));
        
        storeStations.clear();
        for (int i = 0; i < config.getNumStoreStations(); i++) storeStations.add(new ReservationStation("Store" + (i + 1), ReservationStation.Type.STORE));
        
        integerStations.clear();
        for (int i = 0; i < config.getNumIntegerStations(); i++) integerStations.add(new ReservationStation("Int" + (i + 1), ReservationStation.Type.INTEGER));
        
        // Configure Cache
        cache.configure(config.getCacheBlockSize(), config.getCacheSize(), config.getCacheHitLatency(), config.getCacheMissPenalty());
    }
    
    public void loadProgram(String filePath) {
        try {
            InstructionParser parser = new InstructionParser();
            instructionQueue = parser.loadInstructions(filePath);
            programCounter = 0;
            
            // Initialize timing tracking: [issue, executionStart, executionEnd, writeResult]
            instructionTiming = new ArrayList<>();
            timingInstructions = new ArrayList<>();
            // Don't pre-populate - timing entries are created when instructions are issued
            
            System.out.println("Loaded " + instructionQueue.size() + " instructions");
        } catch (Exception e) {
            System.err.println("Error loading program: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void reset() {
        registerFile.reset();
        memory.reset();
        cache.reset();
        
        // Use default config if not set, otherwise re-configure to reset lists
        if (config == null) config = new SimulatorConfig();
        configure(config);
        
        // Reset stations (configure() already clears and adds new ones, but we reset state)
        for (ReservationStation rs : addSubStations) rs.reset();
        for (ReservationStation rs : mulDivStations) rs.reset();
        for (ReservationStation rs : loadStations) rs.reset();
        for (ReservationStation rs : storeStations) rs.reset();
        for (ReservationStation rs : integerStations) rs.reset();
        
        registerStatus.reset();
        
        programCounter = 0;
        clockCycle = 0;
        running = false;
        branchPending = false;
    }
    
    public void executeCycle() {
        if (!running && programCounter >= instructionQueue.size() && allStationsEmpty()) return;
        
        running = true;
        clockCycle++;
        
        // Pipeline Stages (No ROB - direct execution)
        // Order: Write Result -> Execute -> Issue
        // Write-back happens directly from reservation stations
        writeResultStage();
        executeStage();
        issueStage();
        
        checkCompletion();
    }
    
    private void issueStage() {
        if (programCounter >= instructionQueue.size()) return;
        
        // No branch prediction: block issue if branch is pending
        if (branchPending) return;
        
        Instruction inst = instructionQueue.get(programCounter);
        
        // Determine RS list
        List<ReservationStation> targetStations = getStationsForInstruction(inst);
        if (targetStations == null) return; // Should not happen
        
        // Find free RS
        ReservationStation freeRs = null;
        for (ReservationStation rs : targetStations) {
            if (!rs.isBusy()) {
                freeRs = rs;
                break;
            }
        }
        
        if (freeRs == null) return; // No free RS
        
        // Allocate RS
        freeRs.reset();
        freeRs.setBusy(true);
        freeRs.setInstruction(inst);
        freeRs.setOp(inst.getOperation());
        // Set remaining cycles to execution cycles (latency value represents execution cycles)
        freeRs.setRemainingCycles(config.getLatency(inst.getOperation()));
        
        // Track issue time - create new timing entry for this execution
        int timingIndex = instructionTiming.size();
        instructionTiming.add(new int[]{clockCycle, -1, -1, -1}); // [issue, execStart, execEnd, writeResult]
        timingInstructions.add(inst); // Track which instruction this timing entry represents
        freeRs.setTimingIndex(timingIndex);
        
        // Handle Operands FIRST (before setting destination to avoid circular dependency)
        handleOperands(freeRs, inst);
        
        // Set destination register for write-back AFTER reading operands
        if (!inst.isStore() && !inst.isBranch()) {
            boolean isFloat = inst.getDestRegType() == Instruction.RegisterType.FLOATING;
            // For I-type (Loads, ADDI, DADDI, etc.), destination is Rt
            // For R-type (ADD, SUB, MUL, etc.), destination is Rd
            int destReg;
            if (inst.getType() == Instruction.InstructionType.I_TYPE) {
                destReg = inst.getRt();
            } else {
                destReg = inst.getRd();
            }
            
            freeRs.setDestination(destReg);
            freeRs.setDestFloat(isFloat);
            
            // Update Register Status (Rename) - use station name instead of ROB number
            registerStatus.setStatus(destReg, freeRs.getName(), isFloat);
            System.out.println("Issued " + inst.getOperation() + " to " + freeRs.getName() + ", dest=R" + destReg);
        }
        
        // If this is a branch, set pending flag
        if (inst.isBranch()) {
            branchPending = true;
        }
        
        // Advance
        programCounter++;
    }
    
    private void handleOperands(ReservationStation rs, Instruction inst) {
        // Source 1 (Rs) - For Loads: base address; For Stores: base address
        int rsReg = inst.getRs();
        // Use explicit register type from parser
        // For stores: Rs is base address (Src2Type), Rt is value to store (Src1Type)
        boolean rsIsFloat = inst.isStore() ? 
            (inst.getSrc2RegType() == Instruction.RegisterType.FLOATING) :
            (inst.getSrc1RegType() == Instruction.RegisterType.FLOATING);
        
        String qj = registerStatus.getStatus(rsReg, rsIsFloat);
        if (qj != null) {
            // Check if the station has the value ready
            ReservationStation producerStation = findStationByName(qj);
            if (producerStation != null && producerStation.isResultReady()) {
                double value = producerStation.getResult();
                rs.setVj(value);
                rs.setQj(null);
                System.out.println("Operand Rs: R" + rsReg + " has dependency " + qj + " (ready), value=" + value);
            } else {
                // Set dependency tag (station name)
                rs.setQj(qj);
                rs.setVj(0); // Clear value
                System.out.println("Operand Rs: R" + rsReg + " has dependency " + qj + " (not ready)");
            }
        } else {
            // Read directly from register file
            double value = registerFile.read(rsReg, rsIsFloat);
            rs.setVj(value);
            rs.setQj(null);
            System.out.println("Operand Rs: R" + rsReg + " read from register file, value=" + value);
        }
        
        // Source 2 (Rt) - For R-type, Store value, or Branch
        if (inst.getType() == Instruction.InstructionType.R_TYPE || inst.isStore() || inst.isBranch()) {
            int rtReg = inst.getRt();
            // Use explicit register type from parser
            // For stores: Rt is value to store (Src1Type), not Src2Type
            boolean rtIsFloat = inst.isStore() ? 
                (inst.getSrc1RegType() == Instruction.RegisterType.FLOATING) :
                (inst.getSrc2RegType() == Instruction.RegisterType.FLOATING);
            
            String qk = registerStatus.getStatus(rtReg, rtIsFloat);
            if (qk != null) {
                // Check if the station has the value ready
                ReservationStation producerStation = findStationByName(qk);
                if (producerStation != null && producerStation.isResultReady()) {
                    double value = producerStation.getResult();
                    rs.setVk(value);
                    rs.setQk(null);
                    System.out.println("Operand Rt: R" + rtReg + " has dependency " + qk + " (ready), value=" + value);
                } else {
                    // Set dependency tag (station name)
                    rs.setQk(qk);
                    rs.setVk(0); // Clear value
                    System.out.println("Operand Rt: R" + rtReg + " has dependency " + qk + " (not ready)");
                }
            } else {
                // Read directly from register file
                double value = registerFile.read(rtReg, rtIsFloat);
                rs.setVk(value);
                rs.setQk(null);
                System.out.println("Operand Rt: R" + rtReg + " read from register file, value=" + value);
            }
        } else {
            // Immediate for I-type (Load, ADDI)
            rs.setVk(inst.getImmediate());
            rs.setQk(null);
        }
        
        // For Store: handle the value to be stored (goes in Vk for store buffer)
        if (inst.isStore()) {
            // Rt contains the value to store, already handled above
            // Rs contains the base address
        }
    }
    
    private void executeStage() {
        List<ReservationStation> allStations = getAllStations();
        
        for (ReservationStation rs : allStations) {
            if (rs.isBusy() && !rs.isResultReady()) {
                // CRITICAL: Check if operands are ready (Qj=null, Qk=null)
                if (rs.getQj() == null && rs.getQk() == null) {
                    
                    // Special handling for Load/Store address calculation
                    if (rs.getInstruction().isMemoryOp() && !rs.isAddressReady()) {
                        // Calculate effective address: Vj (Base) + Address (Offset)
                        int effectiveAddr = (int)rs.getVj() + rs.getInstruction().getImmediate();
                        rs.setAddress(effectiveAddr);
                        rs.setAddressReady(true);
                    }
                    
                    // For stores, check memory conflicts BEFORE starting execution
                    if (rs.getInstruction().isStore() && rs.isAddressReady()) {
                        if (checkMemoryConflict(rs)) {
                            continue; // Skip this store - wait for conflicts to clear
                        }
                    }
                    
                    // Track execution start (first time we decrement)
                    int timingIndex = rs.getTimingIndex();
                    if (timingIndex >= 0 && timingIndex < instructionTiming.size()) {
                        if (instructionTiming.get(timingIndex)[1] == -1) {
                            instructionTiming.get(timingIndex)[1] = clockCycle; // Execution start cycle
                        }
                    }
                    
                    // CRITICAL: Decrement first
                    rs.decrementRemainingCycles();
                    
                    // Only complete if AFTER decrementing we reach 0 or below
                    if (rs.getRemainingCycles() <= 0) {
                        // Execution complete
                        double result = 0;
                        boolean ready = true;
                        
                        switch (rs.getInstruction().getOperation()) {
                            // Integer arithmetic
                            case DADDI: result = (long)rs.getVj() + (long)rs.getVk(); break;
                            case DSUBI: result = (long)rs.getVj() - (long)rs.getVk(); break;
                            
                            // Floating-point arithmetic - double precision
                            case ADD_D: result = rs.getVj() + rs.getVk(); break;
                            case SUB_D: result = rs.getVj() - rs.getVk(); break;
                            case MUL_D: result = rs.getVj() * rs.getVk(); break;
                            case DIV_D: result = (rs.getVk() != 0) ? rs.getVj() / rs.getVk() : 0; break;
                            
                            // Floating-point arithmetic - single precision
                            case ADD_S: result = rs.getVj() + rs.getVk(); break;
                            case SUB_S: result = rs.getVj() - rs.getVk(); break;
                            case MUL_S: result = rs.getVj() * rs.getVk(); break;
                            case DIV_S: result = (rs.getVk() != 0) ? rs.getVj() / rs.getVk() : 0; break;
                            
                            case LW: case LD: case L_D: case L_S:
                                // Memory Load
                                // Check for Store-to-Load hazard
                                if (checkStoreConflict(rs)) {
                                    ready = false; // Stall
                                } else {
                                    // Access Cache
                                    int size = getAccessSize(rs.getInstruction().getOperation());
                                    Cache.CacheAccessResult access = cache.read(rs.getAddress(), size);
                                    // Add miss penalty if cache miss
                                    if (!access.isHit()) {
                                        rs.setRemainingCycles(config.getCacheMissPenalty());
                                        ready = false; // Continue execution for miss penalty cycles
                                    } else {
                                        result = getMemoryValue(rs.getAddress(), rs.getInstruction().getOperation());
                                    }
                                }
                                break;
                                
                            case SW: case SD: case S_D: case S_S:
                                // Store: Result is the address
                                // Value to store is in Vk
                                // (Memory conflicts already checked before execution starts)
                                result = rs.getAddress();
                                break;
                                
                            case BEQ: 
                                result = (rs.getVj() == rs.getVk()) ? 1.0 : 0.0;
                                System.out.println("BEQ: Vj=" + rs.getVj() + " Vk=" + rs.getVk() + " result=" + result);
                                // Handle branch
                                handleBranchResult(rs, result == 1.0);
                                break;
                            case BNE: 
                                result = (rs.getVj() != rs.getVk()) ? 1.0 : 0.0;
                                System.out.println("BNE: Vj=" + rs.getVj() + " Vk=" + rs.getVk() + " result=" + result);
                                // Handle branch
                                handleBranchResult(rs, result == 1.0);
                                break;
                        }
                        
                        if (ready) {
                            rs.setResult(result);
                            rs.setResultReady(true);
                            
                            // Track execution complete time ONLY when truly ready (no more penalties)
                            int timingIdx = rs.getTimingIndex();
                            if (timingIdx >= 0 && timingIdx < instructionTiming.size()) {
                                instructionTiming.get(timingIdx)[2] = clockCycle; // Execution complete cycle
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void writeResultStage() {
        List<ReservationStation> readyStations = getAllStations().stream()
            .filter(rs -> rs.isBusy() && rs.isResultReady())
            .collect(Collectors.toList());
            
        if (readyStations.isEmpty()) return;
        
        // Arbitration: Load > Add/Sub/Mul/Div > Store
        ReservationStation winner = null;
        for (ReservationStation rs : readyStations) {
            if (rs.getType() == ReservationStation.Type.LOAD) {
                winner = rs;
                break;
            }
        }
        if (winner == null) winner = readyStations.get(0);
        
        // Broadcast result
        double result = winner.getResult();
        String stationName = winner.getName();
        
        // Broadcast on CDB: Update waiting RSs
        for (ReservationStation rs : getAllStations()) {
            if (rs.isBusy() && !rs.isResultReady()) {
                if (rs.getQj() != null && rs.getQj().equals(stationName)) {
                    rs.setVj(result);
                    rs.setQj(null);
                }
                if (rs.getQk() != null && rs.getQk().equals(stationName)) {
                    rs.setVk(result);
                    rs.setQk(null);
                }
            }
        }
        
        // Track write result time
        int timingIndex = winner.getTimingIndex();
        if (timingIndex >= 0 && timingIndex < instructionTiming.size()) {
            instructionTiming.get(timingIndex)[3] = clockCycle; // Write result cycle
        }
        
        // Clear branch pending flag when branch writes back
        if (winner.getInstruction().isBranch()) {
            branchPending = false;
            System.out.println("Branch completed write-back, clearing branchPending");
        }
        
        // Write back to register file (direct write-back without ROB)
        if (!winner.getInstruction().isStore() && !winner.getInstruction().isBranch()) {
            int dest = winner.getDestination();
            boolean isFloat = winner.isDestFloat();
            registerFile.write(dest, result, isFloat);
            
            // Clear register status if this station still owns it
            String currentStatus = registerStatus.getStatus(dest, isFloat);
            if (stationName.equals(currentStatus)) {
                registerStatus.clearStatus(dest, isFloat);
            }
        }
        
        // Handle Store instructions
        if (winner.getInstruction().isStore()) {
            int addr = (int)result;  // Address
            double val = winner.getVk();  // Value to store
            
            switch (winner.getInstruction().getOperation()) {
                case SW: memory.writeWord(addr, (int)val); break;
                case SD: memory.writeDoubleWord(addr, (long)val); break;
                case S_D: memory.writeDouble(addr, val); break;
                case S_S: memory.writeSingle(addr, (float)val); break;
            }
        }
        
        // Free RS
        winner.reset();
    }
    
    private void handleBranchResult(ReservationStation branchRs, boolean taken) {
        // Don't clear branchPending here - wait until write-back stage
        
        System.out.println("Branch result: taken=" + taken + ", PC=" + programCounter);
        
        if (taken) {
            // Branch taken - Jump to target without flushing
            String label = branchRs.getInstruction().getLabel();
            System.out.println("Branch target label: " + label);
            int targetIndex = findLabelIndex(label);
            System.out.println("Target index: " + targetIndex);
            if (targetIndex != -1) {
                programCounter = targetIndex;
                System.out.println("Jumped to PC=" + programCounter);
                // NO FLUSH - old instructions continue executing
            } else {
                System.out.println("ERROR: Label not found!");
            }
        }
        // If not taken, just continue with PC already incremented
    }
    
    private void flushPipeline() {
        // Reset all reservation stations and register status
        for (ReservationStation rs : getAllStations()) {
            rs.reset();
        }
        registerStatus.reset();
    }
    
    private ReservationStation findStationByName(String name) {
        for (ReservationStation rs : getAllStations()) {
            if (rs.getName().equals(name)) {
                return rs;
            }
        }
        return null;
    }
    
    private int findLabelIndex(String label) {
        for (int i = 0; i < instructionQueue.size(); i++) {
            Instruction inst = instructionQueue.get(i);
            if (label.equals(inst.getDefinedLabel())) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean checkStoreConflict(ReservationStation loadRs) {
        int addr = loadRs.getAddress();
        
        // Check all store stations for address conflicts
        for (ReservationStation rs : storeStations) {
            if (rs.isBusy() && rs.isAddressReady()) {
                if (rs.getAddress() == addr && !rs.isResultReady()) {
                    return true; // Conflict - store to same address is pending
                }
            }
        }
        return false;
    }
    
    private boolean checkMemoryConflict(ReservationStation storeRs) {
        int addr = storeRs.getAddress();
        
        // Check all load stations - store must wait for loads to same address
        for (ReservationStation rs : loadStations) {
            if (rs.isBusy() && rs.isAddressReady() && rs != storeRs) {
                if (rs.getAddress() == addr && !rs.isResultReady()) {
                    return true; // Conflict - load from same address is pending
                }
            }
        }
        
        // Check all other store stations - store must wait for earlier stores to same address
        for (ReservationStation rs : storeStations) {
            if (rs.isBusy() && rs.isAddressReady() && rs != storeRs) {
                if (rs.getAddress() == addr && !rs.isResultReady()) {
                    return true; // Conflict - another store to same address is pending
                }
            }
        }
        
        return false;
    }
    
    private List<ReservationStation> getStationsForInstruction(Instruction inst) {
        if (inst.isBranch()) return integerStations;
        switch (inst.getOperation()) {
            case ADD_D: case SUB_D: case ADD_S: case SUB_S: return addSubStations;
            case MUL_D: case DIV_D: case MUL_S: case DIV_S: return mulDivStations;
            case LW: case LD: case L_D: case L_S: return loadStations;
            case SW: case SD: case S_D: case S_S: return storeStations;
            case DADDI: case DSUBI: return integerStations;
            default: return null;
        }
    }
    
    private List<ReservationStation> getAllStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(addSubStations);
        all.addAll(mulDivStations);
        all.addAll(loadStations);
        all.addAll(storeStations);
        all.addAll(integerStations);
        return all;
    }
    
    private int findInstructionIndex(Instruction inst) {
        if (inst == null || instructionQueue == null) return -1;
        for (int i = 0; i < instructionQueue.size(); i++) {
            if (instructionQueue.get(i) == inst) {
                return i;
            }
        }
        return -1;
    }
    
    private int getAccessSize(Instruction.Operation op) {
        switch (op) {
            case LW: case SW: case L_S: case S_S: return 4;
            case LD: case SD: case L_D: case S_D: return 8;
            default: return 4;
        }
    }
    
    private double getMemoryValue(int addr, Instruction.Operation op) {
        switch (op) {
            case LW: return memory.readWord(addr);
            case LD: return memory.readDoubleWord(addr);
            case L_D: return memory.readDouble(addr);
            case L_S: return memory.readSingle(addr);
            default: return 0;
        }
    }
    
    private void checkCompletion() {
        if (programCounter >= instructionQueue.size() && allStationsEmpty()) {
            running = false;
            System.out.println("Simulation complete at clock cycle " + clockCycle);
        }
    }
    
    private boolean allStationsEmpty() {
        for (ReservationStation rs : getAllStations()) {
            if (rs.isBusy()) return false;
        }
        return true;
    }
    
    // Getters
    public RegisterFile getRegisterFile() { return registerFile; }
    public Memory getMemory() { return memory; }
    public Cache getCache() { return cache; }
    public List<ReservationStation> getAddSubStations() { return addSubStations; }
    public List<ReservationStation> getMulDivStations() { return mulDivStations; }
    public List<ReservationStation> getLoadStations() { return loadStations; }
    public List<ReservationStation> getStoreStations() { return storeStations; }
    public List<ReservationStation> getIntegerStations() { return integerStations; }
    public RegisterStatus getRegisterStatus() { return registerStatus; }
    public int getClockCycle() { return clockCycle; }
    public int getProgramCounter() { return programCounter; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public List<Instruction> getInstructionQueue() { return instructionQueue; }
    public SimulatorConfig getConfig() { return config; }
    public List<int[]> getInstructionTiming() { return instructionTiming; }
    public List<Instruction> getTimingInstructions() { return timingInstructions; }
}
