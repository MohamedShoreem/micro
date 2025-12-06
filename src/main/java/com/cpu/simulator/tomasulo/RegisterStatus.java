package com.cpu.simulator.tomasulo;

/**
 * RegisterStatus tracks which reservation station will write to each register (register renaming).
 * Separate tracking for integer registers (R0-R31) and floating-point registers (F0-F31).
 */
public class RegisterStatus {
    private String[] intStatus;    // Station name for integer registers (e.g., "Int1", "Add1")
    private String[] floatStatus;  // Station name for FP registers
    private static final int NUM_REGISTERS = 32;
    
    public RegisterStatus() {
        intStatus = new String[NUM_REGISTERS];
        floatStatus = new String[NUM_REGISTERS];
        reset();
    }
    
    /**
     * Get the reservation station name for an integer register
     * @param regNum Register number
     * @return Station name, null if no pending write
     */
    public String getIntStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return null;
        }
        return intStatus[regNum];
    }
    
    /**
     * Get the reservation station name for a floating-point register
     * @param regNum Register number
     * @return Station name, null if no pending write
     */
    public String getFloatStatus(int regNum) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return null;
        }
        return floatStatus[regNum];
    }
    
    /**
     * Get status based on register type
     * @param regNum Register number
     * @param isFloat true for FP register, false for integer
     * @return Station name, null if no pending write
     */
    public String getStatus(int regNum, boolean isFloat) {
        return isFloat ? getFloatStatus(regNum) : getIntStatus(regNum);
    }
    
    /**
     * Set the reservation station for an integer register
     * @param regNum Register number
     * @param stationName Station name (e.g., "Int1", "Add1")
     */
    public void setIntStatus(int regNum, String stationName) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        // R0 always has no pending writes
        if (regNum != 0) {
            intStatus[regNum] = stationName;
        }
    }
    
    /**
     * Set the reservation station for a floating-point register
     * @param regNum Register number
     * @param stationName Station name
     */
    public void setFloatStatus(int regNum, String stationName) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            return;
        }
        floatStatus[regNum] = stationName;
    }
    
    /**
     * Set status based on register type
     * @param regNum Register number
     * @param stationName Station name
     * @param isFloat true for FP register, false for integer
     */
    public void setStatus(int regNum, String stationName, boolean isFloat) {
        if (isFloat) {
            setFloatStatus(regNum, stationName);
        } else {
            setIntStatus(regNum, stationName);
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
     * @return Array of station names
     */
    public String[] getAllIntStatus() {
        return intStatus.clone();
    }
    
    /**
     * Get all FP register status for display
     * @return Array of station names
     */
    public String[] getAllFloatStatus() {
        return floatStatus.clone();
    }
}
