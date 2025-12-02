package com.microprocessor.tomasulo.model;

public class SimulatorConfig {
    // Latencies
    private int addLatency = 2;
    private int mulLatency = 10;
    private int divLatency = 40;
    private int integerLatency = 1;
    private int loadLatency = 2;
    private int storeLatency = 2;
    private int branchLatency = 1;

    // Cache parameters
    private int cacheHitLatency = 1;
    private int cacheMissPenalty = 50;
    private int blockSize = 16;
    private int cacheSize = 256;

    // Reservation station sizes
    private int addRSSize = 3;
    private int mulRSSize = 2;
    private int loadBufferSize = 3;
    private int storeBufferSize = 3;

    // ROB size
    private int robSize = 6;

    // Getters and setters
    public int getAddLatency() {
        return addLatency;
    }

    public void setAddLatency(int latency) {
        this.addLatency = latency;
    }

    public int getMulLatency() {
        return mulLatency;
    }

    public void setMulLatency(int latency) {
        this.mulLatency = latency;
    }

    public int getDivLatency() {
        return divLatency;
    }

    public void setDivLatency(int latency) {
        this.divLatency = latency;
    }

    public int getIntegerLatency() {
        return integerLatency;
    }

    public void setIntegerLatency(int latency) {
        this.integerLatency = latency;
    }

    public int getLoadLatency() {
        return loadLatency;
    }

    public void setLoadLatency(int latency) {
        this.loadLatency = latency;
    }

    public int getStoreLatency() {
        return storeLatency;
    }

    public void setStoreLatency(int latency) {
        this.storeLatency = latency;
    }

    public int getBranchLatency() {
        return branchLatency;
    }

    public void setBranchLatency(int latency) {
        this.branchLatency = latency;
    }

    public int getCacheHitLatency() {
        return cacheHitLatency;
    }

    public void setCacheHitLatency(int latency) {
        this.cacheHitLatency = latency;
    }

    public int getCacheMissPenalty() {
        return cacheMissPenalty;
    }

    public void setCacheMissPenalty(int penalty) {
        this.cacheMissPenalty = penalty;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int size) {
        this.blockSize = size;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int size) {
        this.cacheSize = size;
    }

    public int getAddRSSize() {
        return addRSSize;
    }

    public void setAddRSSize(int size) {
        this.addRSSize = size;
    }

    public int getMulRSSize() {
        return mulRSSize;
    }

    public void setMulRSSize(int size) {
        this.mulRSSize = size;
    }

    public int getLoadBufferSize() {
        return loadBufferSize;
    }

    public void setLoadBufferSize(int size) {
        this.loadBufferSize = size;
    }

    public int getStoreBufferSize() {
        return storeBufferSize;
    }

    public void setStoreBufferSize(int size) {
        this.storeBufferSize = size;
    }

    public int getRobSize() {
        return robSize;
    }

    public void setRobSize(int size) {
        this.robSize = size;
    }

    public int getLatencyForType(OperationType type) {
        switch (type) {
            case FP_ADD:
                return addLatency;
            case FP_MUL:
                return mulLatency;
            case FP_DIV:
                return divLatency;
            case INTEGER:
                return integerLatency;
            case LOAD:
                return loadLatency;
            case STORE:
                return storeLatency;
            case BRANCH:
                return branchLatency;
            default:
                return 1;
        }
    }
}
