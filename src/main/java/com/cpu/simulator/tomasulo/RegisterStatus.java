package com.cpu.simulator.tomasulo;

/**
 * RegisterStatus tracks which ROB entry will write to each register (register renaming).
 * Separate tracking for integer registers (R0-R31) and floating-point registers (F0-F31).
 */
public class RegisterStatus {
    private Integer[] intStatus;    // ROB entry number for integer registers
    private Integer[] floatStatus;  // ROB entry number for FP registers
    private static final int NUM_REGISTERS = 32;
    
    public RegisterStatus() {
        intStatus = new Integer[NUM_REGISTERS];
        floatStatus = new Integer[NUM_REGISTERS];
        reset();
    }
    
    /**
     * Get the ROB entry for an integer register
     * @param regNum Register number
     * @return ROB entry number, null if no pending write
     */
    public Integer getIntStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return null;
        }
        return intStatus[regNum];
    }
    
    /**
     * Get the ROB entry for a floating-point register
     * @param regNum Register number
     * @return ROB entry number, null if no pending write
     */
    public Integer getFloatStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return null;
        }
        return floatStatus[regNum];
    }
    
    /**
     * Get status based on register type
     * @param regNum Register number
     * @param isFloat true for FP register, false for integer
     * @return ROB entry number, null if no pending write
     */
    public Integer getStatus(int regNum, boolean isFloat) {
        return isFloat ? getFloatStatus(regNum) : getIntStatus(regNum);
    }
    
    /**
     * Set the ROB entry for an integer register
     * @param regNum Register number
     * @param robEntry ROB entry number
     */
    public void setIntStatus(int regNum, Integer robEntry) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        // R0 always has no pending writes
        if (regNum != 0) {
            intStatus[regNum] = robEntry;
        }
    }
    
    /**
     * Set the ROB entry for a floating-point register
     * @param regNum Register number
     * @param robEntry ROB entry number
     */
    public void setFloatStatus(int regNum, Integer robEntry) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        floatStatus[regNum] = robEntry;
    }
    
    /**
     * Set status based on register type
     * @param regNum Register number
     * @param robEntry ROB entry number
     * @param isFloat true for FP register, false for integer
     */
    public void setStatus(int regNum, Integer robEntry, boolean isFloat) {
        if (isFloat) {
            setFloatStatus(regNum, robEntry);
        } else {
            setIntStatus(regNum, robEntry);
        }
    }
    
    /**
     * Clear the status for an integer register
     * @param regNum Register number
     */
    public void clearIntStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        intStatus[regNum] = null;
    }
    
    /**
     * Clear the status for a floating-point register
     * @param regNum Register number
     */
    public void clearFloatStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        floatStatus[regNum] = null;
    }
    
    /**
     * Clear status based on register type
     * @param regNum Register number
     * @param isFloat true for FP register, false for integer
     */
    public void clearStatus(int regNum, boolean isFloat) {
        if (isFloat) {
            clearFloatStatus(regNum);
        } else {
            clearIntStatus(regNum);
        }
    }
    
    /**
     * Reset all register status
     */
    public void reset() {
        for (int i = 0; i < NUM_REGISTERS; i++) {
            intStatus[i] = null;
            floatStatus[i] = null;
        }
    }
    
    /**
     * Get all integer register status for display
     * @return Array of ROB entry numbers
     */
    public Integer[] getAllIntStatus() {
        return intStatus.clone();
    }
    
    /**
     * Get all FP register status for display
     * @return Array of ROB entry numbers
     */
    public Integer[] getAllFloatStatus() {
        return floatStatus.clone();
    }
}
