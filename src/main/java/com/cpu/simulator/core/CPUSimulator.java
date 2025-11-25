package com.cpu.simulator.core;

import com.cpu.simulator.model.*;
import com.cpu.simulator.tomasulo.*;
import com.cpu.simulator.parser.InstructionParser;
import java.util.ArrayList;
import java.util.List;

/**
 * CPUSimulator is the main class that coordinates the CPU simulation
 * using Tomasulo's algorithm. It manages clock cycles and pipeline stages.
 */
public class CPUSimulator {
    // Hardware components
    private RegisterFile registerFile;
    private Memory memory;
    private Cache cache;
    
    // Tomasulo components
    private List<ReservationStation> addSubStations;  // Addition/Subtraction units
    private List<ReservationStation> mulDivStations;  // Multiplication/Division units
    private List<ReservationStation> loadStations;    // Load units
    private List<ReservationStation> storeStations;   // Store units
    
    private List<ReorderBufferEntry> reorderBuffer;   // ROB
    private RegisterStatus registerStatus;
    
    // Instruction queue
    private List<Instruction> instructionQueue;
    private int programCounter;
    
    // Simulation state
    private int clockCycle;
    private boolean running;
    
    // Configuration
    private static final int NUM_ADD_SUB_STATIONS = 3;
    private static final int NUM_MUL_DIV_STATIONS = 2;
    private static final int NUM_LOAD_STATIONS = 2;
    private static final int NUM_STORE_STATIONS = 2;
    private static final int ROB_SIZE = 16;
    
    private int robHead;  // Points to the next entry to commit
    private int robTail;  // Points to the next free entry
    
    public CPUSimulator() {
        // Initialize hardware
        registerFile = new RegisterFile();
        memory = new Memory();
        cache = new Cache(memory);
        
        // Initialize Tomasulo structures
        addSubStations = new ArrayList<>();
        for (int i = 0; i < NUM_ADD_SUB_STATIONS; i++) {
            addSubStations.add(new ReservationStation("Add" + (i + 1)));
        }
        
        mulDivStations = new ArrayList<>();
        for (int i = 0; i < NUM_MUL_DIV_STATIONS; i++) {
            mulDivStations.add(new ReservationStation("Mul" + (i + 1)));
        }
        
        loadStations = new ArrayList<>();
        for (int i = 0; i < NUM_LOAD_STATIONS; i++) {
            loadStations.add(new ReservationStation("Load" + (i + 1)));
        }
        
        storeStations = new ArrayList<>();
        for (int i = 0; i < NUM_STORE_STATIONS; i++) {
            storeStations.add(new ReservationStation("Store" + (i + 1)));
        }
        
        reorderBuffer = new ArrayList<>();
        for (int i = 0; i < ROB_SIZE; i++) {
            reorderBuffer.add(new ReorderBufferEntry(i));
        }
        
        registerStatus = new RegisterStatus();
        
        instructionQueue = new ArrayList<>();
        programCounter = 0;
        clockCycle = 0;
        running = false;
        robHead = 0;
        robTail = 0;
    }
    
    /**
     * Load a program from a file
     * @param filePath Path to the instruction file
     */
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
    
    /**
     * Reset the CPU to initial state
     */
    public void reset() {
        registerFile.reset();
        memory.reset();
        cache.reset();
        
        for (ReservationStation rs : addSubStations) rs.reset();
        for (ReservationStation rs : mulDivStations) rs.reset();
        for (ReservationStation rs : loadStations) rs.reset();
        for (ReservationStation rs : storeStations) rs.reset();
        for (ReorderBufferEntry entry : reorderBuffer) entry.reset();
        
        registerStatus.reset();
        
        programCounter = 0;
        clockCycle = 0;
        running = false;
        robHead = 0;
        robTail = 0;
    }
    
    /**
     * Execute one clock cycle
     * Stages: Commit -> Write Result -> Execute -> Issue
     */
    public void executeCycle() {
        clockCycle++;
        
        // Stage 1: Commit (from ROB head)
        commitStage();
        
        // Stage 2: Write Result (broadcast results)
        writeResultStage();
        
        // Stage 3: Execute (process ready instructions)
        executeStage();
        
        // Stage 4: Issue (dispatch new instruction)
        issueStage();
        
        // Check if simulation should stop
        checkCompletion();
    }
    
    /**
     * Issue stage: Fetch and decode next instruction, dispatch to RS
     */
    private void issueStage() {
        // TODO: Implement instruction issue logic
        // 1. Check if there's an instruction to fetch
        // 2. Check if there's a free ROB entry
        // 3. Check if there's a free reservation station for this instruction type
        // 4. Allocate RS and ROB entry
        // 5. Read operands or tags from register file/register status
        System.out.println("Clock " + clockCycle + ": Issue stage (to be implemented)");
    }
    
    /**
     * Execute stage: Execute instructions in reservation stations
     */
    private void executeStage() {
        // TODO: Implement execution logic
        // 1. For each reservation station that is ready (operands available)
        // 2. Perform the operation
        // 3. Decrement remaining cycles
        // 4. When done, prepare to write result
        System.out.println("Clock " + clockCycle + ": Execute stage (to be implemented)");
    }
    
    /**
     * Write Result stage: Broadcast results on the common data bus
     */
    private void writeResultStage() {
        // TODO: Implement write result logic
        // 1. For each station that finished execution
        // 2. Broadcast result to waiting stations (update Vj/Vk, clear Qj/Qk)
        // 3. Update ROB entry with result
        // 4. Free the reservation station
        System.out.println("Clock " + clockCycle + ": Write Result stage (to be implemented)");
    }
    
    /**
     * Commit stage: Commit instruction at ROB head
     */
    private void commitStage() {
        // TODO: Implement commit logic
        // 1. Check if ROB head entry is ready
        // 2. If it's a branch, check if mispredicted
        // 3. Write result to register file or memory
        // 4. Free ROB entry
        // 5. Advance ROB head
        System.out.println("Clock " + clockCycle + ": Commit stage (to be implemented)");
    }
    
    /**
     * Check if simulation is complete
     */
    private void checkCompletion() {
        // Check if all instructions are committed
        if (programCounter >= instructionQueue.size() && isROBEmpty()) {
            running = false;
            System.out.println("Simulation complete at clock cycle " + clockCycle);
        }
    }
    
    /**
     * Check if ROB is empty
     * @return true if empty
     */
    private boolean isROBEmpty() {
        return robHead == robTail && reorderBuffer.get(robHead).isEmpty();
    }
    
    // Getters for UI access
    public RegisterFile getRegisterFile() { return registerFile; }
    public Memory getMemory() { return memory; }
    public Cache getCache() { return cache; }
    public List<ReservationStation> getAddSubStations() { return addSubStations; }
    public List<ReservationStation> getMulDivStations() { return mulDivStations; }
    public List<ReservationStation> getLoadStations() { return loadStations; }
    public List<ReservationStation> getStoreStations() { return storeStations; }
    public List<ReorderBufferEntry> getReorderBuffer() { return reorderBuffer; }
    public RegisterStatus getRegisterStatus() { return registerStatus; }
    public int getClockCycle() { return clockCycle; }
    public int getProgramCounter() { return programCounter; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public List<Instruction> getInstructionQueue() { return instructionQueue; }
}
