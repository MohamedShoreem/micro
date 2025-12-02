package com.microprocessor.tomasulo.model;

public class Instruction {
    private final int id;
    private final InstructionType type;
    private final String destination;
    private final String source1;
    private final String source2;
    private final int immediate;
    private final int address;

    // Execution tracking
    private int issueTime = -1;
    private int executeStartTime = -1;
    private int executeEndTime = -1;
    private int writeResultTime = -1;
    private int commitTime = -1;

    private Instruction(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.destination = builder.destination;
        this.source1 = builder.source1;
        this.source2 = builder.source2;
        this.immediate = builder.immediate;
        this.address = builder.address;
    }

    public int getId() {
        return id;
    }

    public InstructionType getType() {
        return type;
    }

    public String getDestination() {
        return destination;
    }

    public String getSource1() {
        return source1;
    }

    public String getSource2() {
        return source2;
    }

    public int getImmediate() {
        return immediate;
    }

    public int getAddress() {
        return address;
    }

    public int getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(int time) {
        this.issueTime = time;
    }

    public int getExecuteStartTime() {
        return executeStartTime;
    }

    public void setExecuteStartTime(int time) {
        this.executeStartTime = time;
    }

    public int getExecuteEndTime() {
        return executeEndTime;
    }

    public void setExecuteEndTime(int time) {
        this.executeEndTime = time;
    }

    public int getWriteResultTime() {
        return writeResultTime;
    }

    public void setWriteResultTime(int time) {
        this.writeResultTime = time;
    }

    public int getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(int time) {
        this.commitTime = time;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getMnemonic()).append(" ");

        switch (type.getOperationType()) {
            case FP_ADD:
            case FP_MUL:
            case FP_DIV:
                sb.append(destination).append(", ").append(source1).append(", ").append(source2);
                break;
            case INTEGER:
                sb.append(destination).append(", ").append(source1).append(", ").append(immediate);
                break;
            case LOAD:
                sb.append(destination).append(", ").append(immediate).append("(").append(source1).append(")");
                break;
            case STORE:
                sb.append(source2).append(", ").append(immediate).append("(").append(source1).append(")");
                break;
            case BRANCH:
                sb.append(source1).append(", ").append(source2).append(", ").append(address);
                break;
        }

        return sb.toString();
    }

    public static class Builder {
        private int id;
        private InstructionType type;
        private String destination = "";
        private String source1 = "";
        private String source2 = "";
        private int immediate = 0;
        private int address = 0;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder type(InstructionType type) {
            this.type = type;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder source1(String source1) {
            this.source1 = source1;
            return this;
        }

        public Builder source2(String source2) {
            this.source2 = source2;
            return this;
        }

        public Builder immediate(int immediate) {
            this.immediate = immediate;
            return this;
        }

        public Builder address(int address) {
            this.address = address;
            return this;
        }

        public Instruction build() {
            return new Instruction(this);
        }
    }
}
