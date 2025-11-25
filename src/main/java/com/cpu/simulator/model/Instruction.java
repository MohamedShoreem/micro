package com.cpu.simulator.model;

/**
 * Represents a single MIPS instruction.
 * Supports basic MIPS instruction types: R-type, I-type, and J-type.
 */
public class Instruction {
    // Instruction types
    public enum InstructionType {
        R_TYPE,  // Register type (ADD, SUB, MUL, DIV, etc.)
        I_TYPE,  // Immediate type (ADDI, LW, SW, BEQ, BNE, etc.)
        J_TYPE   // Jump type (J, JAL, etc.)
    }
    
    // Operation types
    public enum Operation {
        // Integer Arithmetic
        ADD, SUB, ADDI, MUL, DIV,
        DADDI, DSUBI,  // Double-word integer operations
        // Floating Point Arithmetic (Double Precision)
        ADD_D, SUB_D, MUL_D, DIV_D,
        // Logical
        AND, OR, XOR,
        // Integer Memory Operations
        LW, SW, LD,  // Load Word, Store Word, Load Doubleword
        // Floating Point Memory Operations
        L_D, L_S, S_D, S_S,  // Load/Store Double, Load/Store Single
        // Branch
        BEQ, BNE,
        // Jump
        J, JAL,
        // No operation
        NOP
    }
    
    // Register types
    public enum RegisterType {
        INTEGER,   // Integer registers (R0-R31)
        FLOATING   // Floating point registers (F0-F31)
    }
    
    private Operation operation;
    private InstructionType type;
    
    // Operands
    private int rd;  // Destination register
    private int rs;  // Source register 1
    private int rt;  // Source register 2
    private int immediate;  // Immediate value (for I-type)
    private int address;    // Address (for J-type)
    
    private String rawInstruction; // Original instruction string
    
    // Register type indicator
    private RegisterType destRegType;  // Type of destination register
    private RegisterType src1RegType;  // Type of source register 1
    private RegisterType src2RegType;  // Type of source register 2
    
    // For branch instructions - label support
    private String label;  // Branch target label
    
    // Execution tracking
    private int latency;  // Execution latency for this instruction
    
    public Instruction(Operation operation, InstructionType type) {
        this.operation = operation;
        this.type = type;
        this.rd = 0;
        this.rs = 0;
        this.rt = 0;
        this.immediate = 0;
        this.address = 0;
        this.latency = 1;  // Default latency
        this.destRegType = RegisterType.INTEGER;
        this.src1RegType = RegisterType.INTEGER;
        this.src2RegType = RegisterType.INTEGER;
        this.label = null;
    }
    
    /**
     * Check if this instruction uses floating point registers
     * @return true if it's a floating point instruction
     */
    public boolean isFloatingPoint() {
        return operation == Operation.ADD_D || operation == Operation.SUB_D ||
               operation == Operation.MUL_D || operation == Operation.DIV_D ||
               operation == Operation.L_D || operation == Operation.L_S ||
               operation == Operation.S_D || operation == Operation.S_S;
    }
    
    /**
     * Check if this is a memory operation
     * @return true if load or store
     */
    public boolean isMemoryOp() {
        return operation == Operation.LW || operation == Operation.SW ||
               operation == Operation.LD || operation == Operation.L_D ||
               operation == Operation.L_S || operation == Operation.S_D ||
               operation == Operation.S_S;
    }
    
    /**
     * Check if this is a branch instruction
     * @return true if branch
     */
    public boolean isBranch() {
        return operation == Operation.BEQ || operation == Operation.BNE;
    }
    
    /**
     * Check if this is a store operation
     * @return true if store
     */
    public boolean isStore() {
        return operation == Operation.SW || operation == Operation.S_D || operation == Operation.S_S;
    }
    
    /**
     * Check if this is a load operation
     * @return true if load
     */
    public boolean isLoad() {
        return operation == Operation.LW || operation == Operation.LD ||
               operation == Operation.L_D || operation == Operation.L_S;
    }
    
    // Getters and setters
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
    
    public InstructionType getType() { return type; }
    public void setType(InstructionType type) { this.type = type; }
    
    public int getRd() { return rd; }
    public void setRd(int rd) { this.rd = rd; }
    
    public int getRs() { return rs; }
    public void setRs(int rs) { this.rs = rs; }
    
    public int getRt() { return rt; }
    public void setRt(int rt) { this.rt = rt; }
    
    public int getImmediate() { return immediate; }
    public void setImmediate(int immediate) { this.immediate = immediate; }
    
    public int getAddress() { return address; }
    public void setAddress(int address) { this.address = address; }
    
    public String getRawInstruction() { return rawInstruction; }
    public void setRawInstruction(String rawInstruction) { this.rawInstruction = rawInstruction; }
    
    public RegisterType getDestRegType() { return destRegType; }
    public void setDestRegType(RegisterType destRegType) { this.destRegType = destRegType; }
    
    public RegisterType getSrc1RegType() { return src1RegType; }
    public void setSrc1RegType(RegisterType src1RegType) { this.src1RegType = src1RegType; }
    
    public RegisterType getSrc2RegType() { return src2RegType; }
    public void setSrc2RegType(RegisterType src2RegType) { this.src2RegType = src2RegType; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public int getLatency() { return latency; }
    public void setLatency(int latency) { this.latency = latency; }
    
    @Override
    public String toString() {
        return rawInstruction != null ? rawInstruction : operation.toString();
    }
}
