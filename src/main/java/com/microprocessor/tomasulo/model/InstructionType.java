package com.microprocessor.tomasulo.model;

public enum InstructionType {
    // Floating Point
    ADD_D("ADD.D", OperationType.FP_ADD),
    SUB_D("SUB.D", OperationType.FP_ADD),
    MUL_D("MUL.D", OperationType.FP_MUL),
    DIV_D("DIV.D", OperationType.FP_DIV),

    // Integer
    ADDI("ADDI", OperationType.INTEGER),
    SUBI("SUBI", OperationType.INTEGER),

    // Load
    LW("LW", OperationType.LOAD),
    LD("LD", OperationType.LOAD),
    L_S("L.S", OperationType.LOAD),
    L_D("L.D", OperationType.LOAD),

    // Store
    SW("SW", OperationType.STORE),
    S_D("S.D", OperationType.STORE),

    // Branch
    BEQ("BEQ", OperationType.BRANCH),
    BNE("BNE", OperationType.BRANCH);

    private final String mnemonic;
    private final OperationType operationType;

    InstructionType(String mnemonic, OperationType operationType) {
        this.mnemonic = mnemonic;
        this.operationType = operationType;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public static InstructionType fromString(String mnemonic) {
        for (InstructionType type : values()) {
            if (type.mnemonic.equalsIgnoreCase(mnemonic)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown instruction: " + mnemonic);
    }
}
