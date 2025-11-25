package com.cpu.simulator.model;

/**
 * RegisterFile represents the CPU's register file.
 * MIPS has 32 integer registers (R0-R31) and 32 floating-point registers (F0-F31).
 * R0 is always 0, R1-R31 are general purpose.
 * All floating-point registers are general purpose.
 */
public class RegisterFile {
    private double[] integerRegisters;  // Use double to handle both int and FP uniformly
    private double[] floatRegisters;
    private static final int NUM_REGISTERS = 32;
    
    public RegisterFile() {
        integerRegisters = new double[NUM_REGISTERS];
        floatRegisters = new double[NUM_REGISTERS];
        // R0 is always 0
        integerRegisters[0] = 0;
    }
    
    /**
     * Read value from an integer register
     * @param index Register number (0-31)
     * @return Value in the register
     */
    public double readInt(int index) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
        return integerRegisters[index];
    }
    
    /**
     * Write value to an integer register
     * @param index Register number (0-31)
     * @param value Value to write
     */
    public void writeInt(int index, double value) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
        // R0 always remains 0
        if (index != 0) {
            integerRegisters[index] = value;
        }
    }
    
    /**
     * Read value from a floating-point register
     * @param index Register number (0-31)
     * @return Value in the register
     */
    public double readFloat(int index) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid FP register index: " + index);
        }
        return floatRegisters[index];
    }
    
    /**
     * Write value to a floating-point register
     * @param index Register number (0-31)
     * @param value Value to write
     */
    public void writeFloat(int index, double value) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid FP register index: " + index);
        }
        floatRegisters[index] = value;
    }
    
    /**
     * Read from appropriate register file based on type
     * @param index Register number
     * @param isFloat true for FP register, false for integer register
     * @return Value in the register
     */
    public double read(int index, boolean isFloat) {
        return isFloat ? readFloat(index) : readInt(index);
    }
    
    /**
     * Write to appropriate register file based on type
     * @param index Register number
     * @param value Value to write
     * @param isFloat true for FP register, false for integer register
     */
    public void write(int index, double value, boolean isFloat) {
        if (isFloat) {
            writeFloat(index, value);
        } else {
            writeInt(index, value);
        }
    }
    
    /**
     * Get all integer register values (for display purposes)
     * @return Array of integer register values
     */
    public double[] getAllIntRegisters() {
        return integerRegisters.clone();
    }
    
    /**
     * Get all floating-point register values (for display purposes)
     * @return Array of FP register values
     */
    public double[] getAllFloatRegisters() {
        return floatRegisters.clone();
    }
    
    /**
     * Preload integer register with value
     * @param index Register number
     * @param value Value to load
     */
    public void preloadInt(int index, double value) {
        writeInt(index, value);
    }
    
    /**
     * Preload floating-point register with value
     * @param index Register number
     * @param value Value to load
     */
    public void preloadFloat(int index, double value) {
        writeFloat(index, value);
    }
    
    /**
     * Reset all registers to 0
     */
    public void reset() {
        for (int i = 0; i < NUM_REGISTERS; i++) {
            integerRegisters[i] = 0;
            floatRegisters[i] = 0;
        }
    }
}
