package com.cpu.simulator.tomasulo;

import com.cpu.simulator.model.Instruction;

/**
 * ReservationStation represents a single reservation station in Tomasulo's algorithm.
 * Each station holds an instruction waiting for operands to become available.
 */
public class ReservationStation {
    public enum Type {
        ADD_SUB,   // FP Addition/Subtraction
        MUL_DIV,   // FP Multiplication/Division
        LOAD,      // Load operations
        STORE,     // Store operations
        INTEGER    // Integer operations (DADDI, DSUBI)
    }
    
    private String name;  // e.g., "Add1", "Mul1", "Load1", "Int1"
    private Type type;    // Station type
    private boolean busy; // Is this station occupied?
    
    // Instruction information
    private Instruction instruction;
    private Instruction.Operation op;
    
    // Operand values and tags
    private double vj;  // Value of source operand 1
    private double vk;  // Value of source operand 2
    private String qj;  // Tag of ROB entry producing vj (null if ready)
    private String qk;  // Tag of ROB entry producing vk (null if ready)
    
    // For memory operations
    private int address;  // Effective address for load/store
    private boolean addressReady;  // Is address computed?
    
    // For store operations
    private double storeValue;  // Value to store
    private String storeTag;    // Tag for value to store (null if ready)
    
    // Execution tracking
    private int remainingCycles;  // Cycles left to complete execution
    
    // Destination register (for write-back without ROB)
    private int destination;      // Destination register number
    private boolean isFloat;      // Is destination a FP register?
    
    // Result
    private double result;
    private boolean resultReady;
    
    // Timing tracking for multiple executions
    private int timingIndex;  // Index in instructionTiming list for this execution
    
    public ReservationStation(String name, Type type) {
        this.name = name;
        this.type = type;
        this.busy = false;
        reset();
    }
    
    // Legacy constructor for backward compatibility
    public ReservationStation(String name) {
        this(name, Type.ADD_SUB);
    }
    
    /**
     * Reset this reservation station to empty state
     */
    public void reset() {
        this.busy = false;
        this.instruction = null;
        this.op = null;
        this.vj = 0;
        this.vk = 0;
        this.qj = null;
        this.qk = null;
        this.address = 0;
        this.addressReady = false;
        this.storeValue = 0;
        this.storeTag = null;
        this.remainingCycles = 0;
        this.destination = -1;
        this.isFloat = false;
        this.result = 0;
        this.resultReady = false;
    }
    
    /**
     * Check if both operands are ready
     * @return true if ready to execute
     */
    public boolean isReady() {
        if (!busy) return false;
        
        if (type == Type.STORE) {
            // Store needs operands, address, and value to store
            return addressReady && qj == null && storeTag == null;
        } else if (type == Type.LOAD) {
            // Load needs address ready
            return addressReady && qj == null;
        } else {
            // Arithmetic operations need both operands
            return qj == null && qk == null;
        }
    }
    
    // Getters and setters
    public String getName() { return name; }
    public Type getType() { return type; }
    
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    
    public Instruction getInstruction() { return instruction; }
    public void setInstruction(Instruction instruction) { this.instruction = instruction; }
    
    public Instruction.Operation getOp() { return op; }
    public void setOp(Instruction.Operation op) { this.op = op; }
    
    public double getVj() { return vj; }
    public void setVj(double vj) { this.vj = vj; }
    
    public double getVk() { return vk; }
    public void setVk(double vk) { this.vk = vk; }
    
    public String getQj() { return qj; }
    public void setQj(String qj) { this.qj = qj; }
    
    public String getQk() { return qk; }
    public void setQk(String qk) { this.qk = qk; }
    
    public int getAddress() { return address; }
    public void setAddress(int address) { 
        this.address = address;
        this.addressReady = true;
    }
    
    public boolean isAddressReady() { return addressReady; }
    public void setAddressReady(boolean ready) { this.addressReady = ready; }
    
    public double getStoreValue() { return storeValue; }
    public void setStoreValue(double value) { this.storeValue = value; }
    
    public String getStoreTag() { return storeTag; }
    public void setStoreTag(String tag) { this.storeTag = tag; }
    
    public int getRemainingCycles() { return remainingCycles; }
    public void setRemainingCycles(int remainingCycles) { this.remainingCycles = remainingCycles; }
    
    public void decrementRemainingCycles() {
        if (this.remainingCycles > 0) {
            this.remainingCycles--;
        }
    }

    public int getDestination() { return destination; }
    public void setDestination(int destination) { this.destination = destination; }
    
    public boolean isDestFloat() { return isFloat; }
    public void setDestFloat(boolean isFloat) { this.isFloat = isFloat; }
    
    public double getResult() { return result; }
    public void setResult(double result) { 
        this.result = result;
        this.resultReady = true;
    }
    
    public boolean isResultReady() { return resultReady; }
    public void setResultReady(boolean ready) { this.resultReady = ready; }
    
    public int getTimingIndex() { return timingIndex; }
    public void setTimingIndex(int timingIndex) { this.timingIndex = timingIndex; }
    
    @Override
    public String toString() {
        if (!busy) return name + ": [Empty]";
        return String.format("%s: Op=%s, Vj=%d, Vk=%d, Qj=%s, Qk=%s", 
                           name, op, vj, vk, qj, qk);
    }
}
