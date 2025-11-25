package com.cpu.simulator.tomasulo;

import com.cpu.simulator.model.Instruction;

/**
 * ReorderBufferEntry represents a single entry in the Reorder Buffer (ROB).
 * ROB maintains program order and handles precise exceptions.
 */
public class ReorderBufferEntry {
    // Entry state
    public enum State {
        FREE,        // Entry is available
        ISSUED,      // Instruction issued to RS
        EXECUTING,   // Currently executing
        WRITE_RESULT, // Result ready, written back
        COMMIT       // Ready to commit
    }
    
    private int entryNumber;  // ROB entry number
    private State state;
    private Instruction instruction;
    
    // Result information
    private int destination;  // Destination register number
    private double value;     // Result value
    private boolean ready;    // Is result ready?
    private boolean isFloat;  // Is destination a FP register?
    
    // For store instructions
    private int memoryAddress;  // Target memory address for stores
    
    // For branch instructions
    private boolean branchTaken;     // Was branch taken?
    private int branchTarget;        // Branch target PC
    private boolean branchResolved;  // Has branch been resolved?
    
    // Exception handling
    private boolean exception;
    
    public ReorderBufferEntry(int entryNumber) {
        this.entryNumber = entryNumber;
        this.state = State.FREE;
        reset();
    }
    
    /**
     * Reset this ROB entry to empty state
     */
    public void reset() {
        this.state = State.FREE;
        this.instruction = null;
        this.destination = -1;
        this.value = 0;
        this.ready = false;
        this.isFloat = false;
        this.memoryAddress = 0;
        this.branchTaken = false;
        this.branchTarget = 0;
        this.branchResolved = false;
        this.exception = false;
    }
    
    /**
     * Check if this entry is empty/free
     * @return true if free
     */
    public boolean isEmpty() {
        return state == State.FREE;
    }
    
    /**
     * Check if this entry is ready to commit
     * @return true if ready
     */
    public boolean canCommit() {
        return state == State.WRITE_RESULT || (ready && state == State.EXECUTING);
    }
    
    // Getters and setters
    public int getEntryNumber() { return entryNumber; }
    
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    
    public Instruction getInstruction() { return instruction; }
    public void setInstruction(Instruction instruction) { this.instruction = instruction; }
    
    public int getDestination() { return destination; }
    public void setDestination(int destination) { this.destination = destination; }
    
    public double getValue() { return value; }
    public void setValue(double value) { 
        this.value = value;
        this.ready = true;
    }
    
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    
    public boolean isFloat() { return isFloat; }
    public void setFloat(boolean isFloat) { this.isFloat = isFloat; }
    
    public int getMemoryAddress() { return memoryAddress; }
    public void setMemoryAddress(int address) { this.memoryAddress = address; }
    
    public boolean isBranchTaken() { return branchTaken; }
    public void setBranchTaken(boolean taken) { this.branchTaken = taken; }
    
    public int getBranchTarget() { return branchTarget; }
    public void setBranchTarget(int target) { this.branchTarget = target; }
    
    public boolean isBranchResolved() { return branchResolved; }
    public void setBranchResolved(boolean resolved) { this.branchResolved = resolved; }
    
    public boolean hasException() { return exception; }
    public void setException(boolean exception) { this.exception = exception; }
    
    @Override
    public String toString() {
        if (isEmpty()) return "ROB" + entryNumber + ": [Free]";
        String regType = isFloat ? "F" : "R";
        return String.format("ROB%d: %s, State=%s, Dest=%s%d, Value=%.2f, Ready=%b", 
                           entryNumber, instruction != null ? instruction.getOperation() : "null", state, 
                           regType, destination, value, ready);
    }
}
