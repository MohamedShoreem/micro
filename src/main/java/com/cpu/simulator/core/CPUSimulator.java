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
    
    private List<ReorderBufferEntry> reorderBuffer;   // ROB
    private RegisterStatus registerStatus;
    
    // Instruction queue
    private List<Instruction> instructionQueue;
    private int programCounter;
    
    // Simulation state
    private int clockCycle;
    private boolean running;
    
    private int robHead;  // Points to the next entry to commit
    private int robTail;  // Points to the next free entry
    
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
        
        reorderBuffer = new ArrayList<>();
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
        
        // Re-initialize ROB
        reorderBuffer.clear();
        for (int i = 0; i < config.getRobSize(); i++) reorderBuffer.add(new ReorderBufferEntry(i));
        
        // Configure Cache
        cache.configure(config.getCacheBlockSize(), config.getCacheSize(), config.getCacheHitLatency(), config.getCacheMissPenalty());
    }
    
    public void loadProgram(String filePath) {
        try {
            InstructionParser parser = new InstructionParser();
            instructionQueue = parser.loadInstructions(filePath);
            programCounter = 0;
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
        for (ReorderBufferEntry entry : reorderBuffer) entry.reset();
        
        registerStatus.reset();
        
        programCounter = 0;
        clockCycle = 0;
        running = false;
        robHead = 0;
        robTail = 0;
    }
    
    public void executeCycle() {
        if (!running && programCounter >= instructionQueue.size() && isROBEmpty()) return;
        
        running = true;
        clockCycle++;
        
        // Pipeline Stages
        // Order: Commit -> Execute -> Write Result -> Issue
        // This ensures that results written in this cycle are not used for execution until the next cycle
        commitStage();
        executeStage();
        writeResultStage();
        issueStage();
        
        checkCompletion();
    }
    
    private void issueStage() {
        if (programCounter >= instructionQueue.size()) return;
        
        // Check if ROB is full
        int nextRob = (robTail + 1) % config.getRobSize();
        if (nextRob == robHead && !reorderBuffer.get(robHead).isEmpty()) return; // ROB Full
        
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
        
        // Allocate ROB
        ReorderBufferEntry robEntry = reorderBuffer.get(robTail);
        robEntry.reset();
        robEntry.setState(ReorderBufferEntry.State.ISSUED);
        robEntry.setInstruction(inst);
        robEntry.setDestination(inst.getRd());
        robEntry.setFloat(inst.isFloatingPoint());
        
        // Allocate RS
        freeRs.reset();
        freeRs.setBusy(true);
        freeRs.setInstruction(inst);
        freeRs.setOp(inst.getOperation());
        freeRs.setRobEntry(robTail);
        // Set remaining cycles to execution cycles (latency value represents execution cycles)
        freeRs.setRemainingCycles(config.getLatency(inst.getOperation()));
        
        // Handle Operands
        handleOperands(freeRs, inst);
        
        // Update Register Status (Rename)
        if (!inst.isStore() && !inst.isBranch()) {
            boolean isFloat = inst.getDestRegType() == Instruction.RegisterType.FLOATING;
            // For Loads, destination is Rt (e.g., L.D F0, 0(R1) -> F0 is Rt)
            // For R-type, destination is Rd
            int destReg = inst.isLoad() ? inst.getRt() : inst.getRd();
            
            registerStatus.setStatus(destReg, robTail, isFloat);
            robEntry.setDestination(destReg);
        }
        
        // Advance
        robTail = nextRob;
        programCounter++;
    }
    
    private void handleOperands(ReservationStation rs, Instruction inst) {
        // Source 1 (Rs) - For Load/Store this is the base address register
        int rsReg = inst.getRs();
        // Use explicit register type from parser
        boolean rsIsFloat = inst.getSrc1RegType() == Instruction.RegisterType.FLOATING;
        
        Integer qj = registerStatus.getStatus(rsReg, rsIsFloat);
        if (qj != null) {
            // Check if the ROB entry has the value ready
            if (reorderBuffer.get(qj).isReady()) {
                rs.setVj(reorderBuffer.get(qj).getValue());
                rs.setQj(null);
            } else {
                // Set dependency tag
                rs.setQj(String.valueOf(qj));
                rs.setVj(0); // Clear value
            }
        } else {
            // Read directly from register file
            rs.setVj(registerFile.read(rsReg, rsIsFloat));
            rs.setQj(null);
        }
        
        // Source 2 (Rt) - For R-type, Store value, or Branch
        if (inst.getType() == Instruction.InstructionType.R_TYPE || inst.isStore() || inst.isBranch()) {
            int rtReg = inst.getRt();
            // Use explicit register type from parser
            boolean rtIsFloat = inst.getSrc2RegType() == Instruction.RegisterType.FLOATING;
            
            Integer qk = registerStatus.getStatus(rtReg, rtIsFloat);
            if (qk != null) {
                // Check if the ROB entry has the value ready
                if (reorderBuffer.get(qk).isReady()) {
                    rs.setVk(reorderBuffer.get(qk).getValue());
                    rs.setQk(null);
                } else {
                    // Set dependency tag
                    rs.setQk(String.valueOf(qk));
                    rs.setVk(0); // Clear value
                }
            } else {
                // Read directly from register file
                rs.setVk(registerFile.read(rtReg, rtIsFloat));
                rs.setQk(null);
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
                // CRITICAL: Check if operands are ready (Qj=0, Qk=0)
                if (rs.getQj() == null && rs.getQk() == null) {
                    
                    // Mark as executing on first cycle
                    if (rs.getRemainingCycles() == config.getLatency(rs.getInstruction().getOperation())) {
                        reorderBuffer.get(rs.getRobEntry()).setState(ReorderBufferEntry.State.EXECUTING);
                    }
                    
                    // Special handling for Load/Store address calculation
                    if (rs.getInstruction().isMemoryOp() && !rs.isAddressReady()) {
                        // Calculate effective address: Vj (Base) + Address (Offset)
                        int effectiveAddr = (int)rs.getVj() + rs.getInstruction().getImmediate();
                        rs.setAddress(effectiveAddr);
                        rs.setAddressReady(true);
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
                                    if (!access.isHit()) {
                                        // Miss penalty handled by cache latency config?
                                        // We should ideally add penalty cycles here if it's a miss
                                        // But for now we proceed.
                                    }
                                    result = getMemoryValue(rs.getAddress(), rs.getInstruction().getOperation());
                                }
                                break;
                                
                            case SW: case SD: case S_D: case S_S:
                                // Store: Result is the address (for ROB)
                                // Value to store is in Vk
                                result = rs.getAddress();
                                break;
                                
                            case BEQ: result = (rs.getVj() == rs.getVk()) ? 1.0 : 0.0; break;
                            case BNE: result = (rs.getVj() != rs.getVk()) ? 1.0 : 0.0; break;
                        }
                        
                        if (ready) {
                            rs.setResult(result);
                            rs.setResultReady(true);
                            // Don't update ROB state here - it's already EXECUTING
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
        
        // Broadcast
        double result = winner.getResult();
        int robTag = winner.getRobEntry();
        
        // Broadcast on CDB: Update waiting RSs
        for (ReservationStation rs : getAllStations()) {
            if (rs.isBusy() && !rs.isResultReady()) {
                if (rs.getQj() != null && rs.getQj().equals(String.valueOf(robTag))) {
                    rs.setVj(result);
                    rs.setQj(null);
                }
                if (rs.getQk() != null && rs.getQk().equals(String.valueOf(robTag))) {
                    rs.setVk(result);
                    rs.setQk(null);
                }
            }
        }
        
        // Update ROB
        ReorderBufferEntry robEntry = reorderBuffer.get(robTag);
        robEntry.setValue(result);
        robEntry.setReady(true);
        robEntry.setState(ReorderBufferEntry.State.WRITE_RESULT);
        
        // For Store, we also need to pass the value to store (Vk)
        if (winner.getType() == ReservationStation.Type.STORE) {
            robEntry.setMemoryAddress((int)result); // Result of Store exec is address
            robEntry.setValue(winner.getVk());      // Value to store
        }
        
        // Free RS
        winner.reset();
    }
    
    private void commitStage() {
        ReorderBufferEntry head = reorderBuffer.get(robHead);
        
        if (head.getState() == ReorderBufferEntry.State.WRITE_RESULT && head.isReady()) {
            Instruction inst = head.getInstruction();
            
            // Handle Store
            if (inst.isStore()) {
                int addr = head.getMemoryAddress();
                double val = head.getValue();
                
                switch (inst.getOperation()) {
                    case SW: memory.writeWord(addr, (int)val); break;
                    case SD: memory.writeDoubleWord(addr, (long)val); break;
                    case S_D: memory.writeDouble(addr, val); break;
                    case S_S: memory.writeSingle(addr, (float)val); break;
                }
            }
            // Handle Branch
            else if (inst.isBranch()) {
                boolean taken = head.getValue() == 1.0;
                if (taken) {
                    // Branch taken - Flush and jump
                    String label = inst.getLabel();
                    int targetIndex = findLabelIndex(label);
                    if (targetIndex != -1) {
                        programCounter = targetIndex;
                        flushPipeline();
                        return; 
                    }
                }
            }
            // Handle Register Write
            else {
                int dest = head.getDestination();
                boolean isFloat = head.isFloat();
                
                registerFile.write(dest, head.getValue(), isFloat);
                
                // Update Register Status if this ROB entry is still the latest
                Integer currentStatus = registerStatus.getStatus(dest, isFloat);
                if (currentStatus != null && currentStatus == robHead) {
                    registerStatus.clearStatus(dest, isFloat);
                }
            }
            
            head.setState(ReorderBufferEntry.State.COMMIT);
            head.reset(); // Free ROB entry
            robHead = (robHead + 1) % config.getRobSize();
        }
    }
    
    private void flushPipeline() {
        for (ReservationStation rs : getAllStations()) rs.reset();
        for (ReorderBufferEntry entry : reorderBuffer) entry.reset();
        registerStatus.reset();
        robHead = 0;
        robTail = 0;
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
        int loadRob = loadRs.getRobEntry();
        int addr = loadRs.getAddress();
        
        int curr = robHead;
        while (curr != loadRob) {
            ReorderBufferEntry entry = reorderBuffer.get(curr);
            if (!entry.isEmpty() && entry.getInstruction().isStore()) {
                if (entry.getMemoryAddress() == addr) {
                    return true; // Conflict
                }
            }
            curr = (curr + 1) % config.getRobSize();
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
        if (programCounter >= instructionQueue.size() && isROBEmpty()) {
            running = false;
            System.out.println("Simulation complete at clock cycle " + clockCycle);
        }
    }
    
    private boolean isROBEmpty() {
        return robHead == robTail && reorderBuffer.get(robHead).isEmpty();
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
    public List<ReorderBufferEntry> getReorderBuffer() { return reorderBuffer; }
    public RegisterStatus getRegisterStatus() { return registerStatus; }
    public int getClockCycle() { return clockCycle; }
    public int getProgramCounter() { return programCounter; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public List<Instruction> getInstructionQueue() { return instructionQueue; }
    public SimulatorConfig getConfig() { return config; }
}
