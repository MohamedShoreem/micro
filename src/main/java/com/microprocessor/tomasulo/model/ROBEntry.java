package com.microprocessor.tomasulo.model;

public class ROBEntry {
    private final int id;
    private boolean busy;
    private InstructionType instructionType;
    private String destination;
    private double value;
    private boolean ready;
    private Instruction instruction;

    public ROBEntry(int id) {
        this.id = id;
        this.busy = false;
        this.ready = false;
    }

    public void clear() {
        this.busy = false;
        this.instructionType = null;
        this.destination = null;
        this.value = 0;
        this.ready = false;
        this.instruction = null;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public InstructionType getInstructionType() {
        return instructionType;
    }

    public void setInstructionType(InstructionType type) {
        this.instructionType = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public String getName() {
        return "ROB" + id;
    }
}
