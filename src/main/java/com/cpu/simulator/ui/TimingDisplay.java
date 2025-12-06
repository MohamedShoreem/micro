package com.cpu.simulator.ui;

import javafx.beans.property.SimpleStringProperty;

public class TimingDisplay {
    private final SimpleStringProperty instruction;
    private final SimpleStringProperty issue;
    private final SimpleStringProperty executionComplete;
    private final SimpleStringProperty writeResult;
    
    public TimingDisplay(String instruction, String issue, String executionComplete, String writeResult) {
        this.instruction = new SimpleStringProperty(instruction);
        this.issue = new SimpleStringProperty(issue);
        this.executionComplete = new SimpleStringProperty(executionComplete);
        this.writeResult = new SimpleStringProperty(writeResult);
    }
    
    public String getInstruction() { return instruction.get(); }
    public void setInstruction(String value) { instruction.set(value); }
    public SimpleStringProperty instructionProperty() { return instruction; }
    
    public String getIssue() { return issue.get(); }
    public void setIssue(String value) { issue.set(value); }
    public SimpleStringProperty issueProperty() { return issue; }
    
    public String getExecutionComplete() { return executionComplete.get(); }
    public void setExecutionComplete(String value) { executionComplete.set(value); }
    public SimpleStringProperty executionCompleteProperty() { return executionComplete; }
    
    public String getWriteResult() { return writeResult.get(); }
    public void setWriteResult(String value) { writeResult.set(value); }
    public SimpleStringProperty writeResultProperty() { return writeResult; }
}
