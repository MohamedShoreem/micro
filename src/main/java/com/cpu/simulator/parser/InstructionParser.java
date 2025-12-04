package com.cpu.simulator.parser;

import com.cpu.simulator.model.Instruction;
import com.cpu.simulator.model.Instruction.Operation;
import com.cpu.simulator.model.Instruction.InstructionType;
import com.cpu.simulator.model.Instruction.RegisterType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InstructionParser reads MIPS instructions from a text file
 * and parses them into Instruction objects.
 * 
 * Expected format:
 * ADD R1, R2, R3
 * ADDI R1, R2, 10
 * LW R1, 0(R2)
 * SW R1, 4(R2)
 * BEQ R1, R2, label
 */
public class InstructionParser {
    
    private Map<String, Integer> labels;  // Label -> instruction index mapping
    
    /**
     * Load instructions from a text file
     * @param filePath Path to the instruction file
     * @return List of parsed instructions
     */
    public List<Instruction> loadInstructions(String filePath) throws IOException {
        labels = new HashMap<>();
        List<Instruction> instructions = new ArrayList<>();
        
        // First pass: Identify labels
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int instructionIndex = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                // Check for labels (format: LABEL:)
                if (line.contains(":") && !line.contains("(")) {
                    String label = line.substring(0, line.indexOf(":")).trim();
                    labels.put(label, instructionIndex);
                    line = line.substring(line.indexOf(":") + 1).trim();
                }
                
                if (!line.isEmpty()) {
                    instructionIndex++;
                }
            }
        }
        
        // Second pass: Parse instructions
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                // Remove label if present
                String definedLabel = null;
                if (line.contains(":") && !line.contains("(")) {
                    definedLabel = line.substring(0, line.indexOf(":")).trim();
                    line = line.substring(line.indexOf(":") + 1).trim();
                }
                
                if (!line.isEmpty()) {
                    Instruction instruction = parseLine(line);
                    if (instruction != null) {
                        if (definedLabel != null) {
                            instruction.setDefinedLabel(definedLabel);
                        }
                        instructions.add(instruction);
                    }
                }
            }
        }
        
        return instructions;
    }
    
    /**
     * Parse a single line of MIPS instruction
     * @param line Instruction line
     * @return Parsed Instruction object
     */
    private Instruction parseLine(String line) {
        // Remove comments from the line
        int commentIndex = line.indexOf("#");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        
        line = line.trim();
        if (line.isEmpty()) {
            return null;
        }
        
        // Split by space or comma
        String[] parts = line.split("[\\s,()]+");
        if (parts.length == 0) {
            return null;
        }
        
        String opcode = parts[0].toUpperCase();
        Instruction instruction = createInstruction(opcode);
        
        if (instruction != null) {
            instruction.setRawInstruction(line);
            parseOperands(instruction, parts);
        }
        
        return instruction;
    }
    
    /**
     * Create an instruction based on opcode
     * @param opcode Operation code string
     * @return Instruction object
     */
    private Instruction createInstruction(String opcode) {
        try {
            Instruction inst;
            switch (opcode) {
                // Floating-point arithmetic - Double Precision (R-type)
                case "ADD.D":
                    inst = new Instruction(Operation.ADD_D, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "SUB.D":
                    inst = new Instruction(Operation.SUB_D, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "MUL.D":
                    inst = new Instruction(Operation.MUL_D, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "DIV.D":
                    inst = new Instruction(Operation.DIV_D, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                
                // Floating-point arithmetic - Single Precision (R-type)
                case "ADD.S":
                    inst = new Instruction(Operation.ADD_S, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "SUB.S":
                    inst = new Instruction(Operation.SUB_S, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "MUL.S":
                    inst = new Instruction(Operation.MUL_S, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                case "DIV.S":
                    inst = new Instruction(Operation.DIV_S, InstructionType.R_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.FLOATING);
                    break;
                
                // Integer I-type instructions
                case "DADDI":
                    inst = new Instruction(Operation.DADDI, InstructionType.I_TYPE);
                    break;
                case "DSUBI":
                    inst = new Instruction(Operation.DSUBI, InstructionType.I_TYPE);
                    break;
                
                // Integer memory operations
                case "LW":
                    inst = new Instruction(Operation.LW, InstructionType.I_TYPE);
                    break;
                case "SW":
                    inst = new Instruction(Operation.SW, InstructionType.I_TYPE);
                    break;
                case "LD":
                    inst = new Instruction(Operation.LD, InstructionType.I_TYPE);
                    break;
                case "SD":
                    inst = new Instruction(Operation.SD, InstructionType.I_TYPE);
                    break;
                
                // Floating-point memory operations
                case "L.D":
                    inst = new Instruction(Operation.L_D, InstructionType.I_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.INTEGER);  // Base address register
                    break;
                case "L.S":
                    inst = new Instruction(Operation.L_S, InstructionType.I_TYPE);
                    inst.setDestRegType(RegisterType.FLOATING);
                    inst.setSrc1RegType(RegisterType.INTEGER);
                    break;
                case "S.D":
                    inst = new Instruction(Operation.S_D, InstructionType.I_TYPE);
                    inst.setSrc1RegType(RegisterType.FLOATING);  // Value to store
                    inst.setSrc2RegType(RegisterType.INTEGER);   // Base address register
                    break;
                case "S.S":
                    inst = new Instruction(Operation.S_S, InstructionType.I_TYPE);
                    inst.setSrc1RegType(RegisterType.FLOATING);
                    inst.setSrc2RegType(RegisterType.INTEGER);
                    break;
                
                // Branch instructions
                case "BEQ":
                    inst = new Instruction(Operation.BEQ, InstructionType.I_TYPE);
                    break;
                case "BNE":
                    inst = new Instruction(Operation.BNE, InstructionType.I_TYPE);
                    break;
                
                default:
                    System.err.println("Unknown opcode: " + opcode);
                    return null;
            }
            return inst;
        } catch (Exception e) {
            System.err.println("Error creating instruction: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse operands for the instruction
     * @param instruction Instruction to populate
     * @param parts Parsed parts of the instruction
     */
    private void parseOperands(Instruction instruction, String[] parts) {
        try {
            switch (instruction.getType()) {
                case R_TYPE:
                    // Format: ADD.D F0, F2, F4 or ADD R1, R2, R3
                    if (parts.length >= 4) {
                        instruction.setRd(parseRegister(parts[1]));
                        instruction.setDestRegType(getRegisterType(parts[1]));
                        
                        instruction.setRs(parseRegister(parts[2]));
                        instruction.setSrc1RegType(getRegisterType(parts[2]));
                        
                        instruction.setRt(parseRegister(parts[3]));
                        instruction.setSrc2RegType(getRegisterType(parts[3]));
                    }
                    break;
                
                case I_TYPE:
                    if (instruction.getOperation() == Operation.LW || 
                        instruction.getOperation() == Operation.SW ||
                        instruction.getOperation() == Operation.LD ||
                        instruction.getOperation() == Operation.L_D ||
                        instruction.getOperation() == Operation.L_S ||
                        instruction.getOperation() == Operation.S_D ||
                        instruction.getOperation() == Operation.S_S) {
                        // Format: L.D F6, 0(R2) or LW R1, 4(R2)
                        if (parts.length >= 3) {
                            instruction.setRt(parseRegister(parts[1]));  // Dest/source register
                            // For Load, Rt is Dest. For Store, Rt is Src1.
                            if (instruction.isStore()) {
                                instruction.setSrc1RegType(getRegisterType(parts[1]));
                            } else {
                                instruction.setDestRegType(getRegisterType(parts[1]));
                            }
                            
                            instruction.setImmediate(Integer.parseInt(parts[2]));  // Offset
                            
                            instruction.setRs(parseRegister(parts[3]));  // Base register
                            instruction.setSrc2RegType(getRegisterType(parts[3])); // Base is usually Src2 or Src1?
                            // In Instruction.java logic:
                            // Load: Dest=Rt, Src1=Rs (Base)
                            // Store: Src1=Rt (Value), Src2=Rs (Base)
                            // Let's align with that.
                            if (instruction.isStore()) {
                                instruction.setSrc2RegType(getRegisterType(parts[3]));
                            } else {
                                instruction.setSrc1RegType(getRegisterType(parts[3]));
                            }
                        }
                    } else if (instruction.getOperation() == Operation.BEQ ||
                               instruction.getOperation() == Operation.BNE) {
                        // Format: BEQ R1, R2, LOOP or BNE R1, R2, 100
                        if (parts.length >= 4) {
                            instruction.setRs(parseRegister(parts[1]));
                            instruction.setSrc1RegType(getRegisterType(parts[1]));
                            
                            instruction.setRt(parseRegister(parts[2]));
                            instruction.setSrc2RegType(getRegisterType(parts[2]));
                            
                            // Try to parse as number, if fails, it's a label
                            try {
                                instruction.setImmediate(Integer.parseInt(parts[3]));
                            } catch (NumberFormatException e) {
                                // It's a label - resolve to address
                                String label = parts[3];
                                instruction.setLabel(label);
                                if (labels.containsKey(label)) {
                                    instruction.setImmediate(labels.get(label));
                                } else {
                                    instruction.setImmediate(0);
                                }
                            }
                        }
                    } else {
                        // Format: ADDI R1, R2, 10 or DADDI R1, R1, 24
                        if (parts.length >= 4) {
                            instruction.setRt(parseRegister(parts[1]));  // Dest
                            instruction.setDestRegType(getRegisterType(parts[1]));
                            
                            instruction.setRs(parseRegister(parts[2]));  // Source
                            instruction.setSrc1RegType(getRegisterType(parts[2]));
                            
                            instruction.setImmediate(Integer.parseInt(parts[3]));  // Immediate
                        }
                    }
                    break;
                
                case J_TYPE:
                    // Format: J label or J 100
                    if (parts.length >= 2) {
                        try {
                            instruction.setAddress(Integer.parseInt(parts[1]));
                        } catch (NumberFormatException e) {
                            // It's a label - resolve to address
                            String label = parts[1];
                            instruction.setLabel(label);
                            if (labels.containsKey(label)) {
                                instruction.setAddress(labels.get(label));
                            } else {
                                instruction.setAddress(0);
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error parsing operands for " + instruction.getRawInstruction() + ": " + e.getMessage());
        }
    }
    
    private RegisterType getRegisterType(String regStr) {
        if (regStr.toUpperCase().startsWith("F")) {
            return RegisterType.FLOATING;
        }
        return RegisterType.INTEGER;
    }
    
    /**
     * Parse register string to register number
     * @param regStr Register string (e.g., "R1", "r1", "$1", "F2", "f2")
     * @return Register number
     */
    private int parseRegister(String regStr) {
        regStr = regStr.toUpperCase()
                       .replace("R", "")
                       .replace("F", "")
                       .replace("$", "");
        return Integer.parseInt(regStr);
    }
    
    /**
     * Get label map
     * @return Map of label names to instruction indices
     */
    public Map<String, Integer> getLabels() {
        return labels;
    }
}
