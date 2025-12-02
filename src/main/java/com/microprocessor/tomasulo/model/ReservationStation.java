package com.microprocessor.tomasulo.model;

public class ReservationStation {
    private final String name;
    private final OperationType type;
    private boolean busy;
    private String op;
    private double vj;
    private double vk;
    private String qj;
    private String qk;
    private int timeRemaining;
    private Instruction instruction;
    private int robEntry;

    public ReservationStation(String name, OperationType type) {
        this.name = name;
        this.type = type;
        this.busy = false;
    }

    public void clear() {
        this.busy = false;
        this.op = null;
        this.vj = 0;
        this.vk = 0;
        this.qj = null;
        this.qk = null;
        this.timeRemaining = 0;
        this.instruction = null;
        this.robEntry = -1;
    }

    public boolean isReady() {
        return busy && qj == null && qk == null;
    }

    public void decrementTime() {
        if (timeRemaining > 0) {
            timeRemaining--;
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public OperationType getType() {
        return type;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public double getVj() {
        return vj;
    }

    public void setVj(double vj) {
        this.vj = vj;
    }

    public double getVk() {
        return vk;
    }

    public void setVk(double vk) {
        this.vk = vk;
    }

    public String getQj() {
        return qj;
    }

    public void setQj(String qj) {
        this.qj = qj;
    }

    public String getQk() {
        return qk;
    }

    public void setQk(String qk) {
        this.qk = qk;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int time) {
        this.timeRemaining = time;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public int getRobEntry() {
        return robEntry;
    }

    public void setRobEntry(int robEntry) {
        this.robEntry = robEntry;
    }
}
