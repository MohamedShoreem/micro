package com.cpu.simulator.model;

import java.util.HashMap;
import java.util.Map;

/**
 * SimulatorConfig holds all configurable parameters for the CPU simulator.
 * This includes instruction latencies, reservation station sizes, ROB size,
 * and cache parameters.
 */
public class SimulatorConfig {
    // Instruction latencies (in clock cycles)
    private Map<Instruction.Operation, Integer> latencies;
    
    // Reservation station counts
    private int numAddSubStations;
    private int numMulDivStations;
    private int numLoadStations;
    private int numStoreStations;
    private int numIntegerStations;  // For DADDI, DSUBI
    
    // Reorder Buffer size
    private int robSize;
    
    // Cache parameters
    private int cacheBlockSize;  // Bytes per block
    private int cacheSize;       // Total cache size in bytes
    private int cacheHitLatency; // Cycles for cache hit
    private int cacheMissPenalty; // Additional cycles for miss
    
    /**
     * Create configuration with default values
     */
    public SimulatorConfig() {
        // Default execution cycles (not including issue cycle)
        // Total latency = execution cycles + 1 (for issue)
        latencies = new HashMap<>();
        
        // Floating-point arithmetic - double precision (execution cycles only)
        latencies.put(Instruction.Operation.ADD_D, 2);
        latencies.put(Instruction.Operation.SUB_D, 2);
        latencies.put(Instruction.Operation.MUL_D, 10);
        latencies.put(Instruction.Operation.DIV_D, 40);
        
        // Floating-point arithmetic - single precision (execution cycles only)
        latencies.put(Instruction.Operation.ADD_S, 2);
        latencies.put(Instruction.Operation.SUB_S, 2);
        latencies.put(Instruction.Operation.MUL_S, 10);
        latencies.put(Instruction.Operation.DIV_S, 40);
        
        // Integer arithmetic
        latencies.put(Instruction.Operation.DADDI, 1);
        latencies.put(Instruction.Operation.DSUBI, 1);
        
        // Memory operations (base latency, cache will add more)
        latencies.put(Instruction.Operation.LW, 1);
        latencies.put(Instruction.Operation.SW, 1);
        latencies.put(Instruction.Operation.LD, 1);
        latencies.put(Instruction.Operation.SD, 1);
        latencies.put(Instruction.Operation.L_D, 1);
        latencies.put(Instruction.Operation.L_S, 1);
        latencies.put(Instruction.Operation.S_D, 1);
        latencies.put(Instruction.Operation.S_S, 1);
        
        // Branch operations
        latencies.put(Instruction.Operation.BEQ, 1);
        latencies.put(Instruction.Operation.BNE, 1);
        
        // Default reservation station counts (at least 2 for each type)
        numAddSubStations = 2;
        numMulDivStations = 2;
        numLoadStations = 2;
        numStoreStations = 2;
        numIntegerStations = 2;
        
        // Default ROB size
        robSize = 16;
        
        // Default cache parameters
        cacheBlockSize = 32;    // 32 bytes per block
        cacheSize = 256;        // 256 bytes total cache
        cacheHitLatency = 1;    // 1 cycle on hit
        cacheMissPenalty = 10;  // 10 additional cycles on miss
    }
    
    // Getters
    public int getLatency(Instruction.Operation op) {
        return latencies.getOrDefault(op, 1);
    }
    
    public int getNumAddSubStations() { return numAddSubStations; }
    public int getNumMulDivStations() { return numMulDivStations; }
    public int getNumLoadStations() { return numLoadStations; }
    public int getNumStoreStations() { return numStoreStations; }
    public int getNumIntegerStations() { return numIntegerStations; }
    public int getRobSize() { return robSize; }
    public int getCacheBlockSize() { return cacheBlockSize; }
    public int getCacheSize() { return cacheSize; }
    public int getCacheHitLatency() { return cacheHitLatency; }
    public int getCacheMissPenalty() { return cacheMissPenalty; }
    
    // Setters
    public void setLatency(Instruction.Operation op, int cycles) {
        latencies.put(op, cycles);
    }
    
    public void setNumAddSubStations(int num) { this.numAddSubStations = num; }
    public void setNumMulDivStations(int num) { this.numMulDivStations = num; }
    public void setNumLoadStations(int num) { this.numLoadStations = num; }
    public void setNumStoreStations(int num) { this.numStoreStations = num; }
    public void setNumIntegerStations(int num) { this.numIntegerStations = num; }
    public void setRobSize(int size) { this.robSize = size; }
    
    public void setCacheBlockSize(int size) { this.cacheBlockSize = size; }
    public void setCacheSize(int size) { this.cacheSize = size; }
    public void setCacheHitLatency(int latency) { this.cacheHitLatency = latency; }
    public void setCacheMissPenalty(int penalty) { this.cacheMissPenalty = penalty; }
}
