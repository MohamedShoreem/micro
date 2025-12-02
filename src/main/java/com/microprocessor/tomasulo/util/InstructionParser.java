package com.microprocessor.tomasulo.util;

import com.microprocessor.tomasulo.model.Instruction;
import com.microprocessor.tomasulo.model.InstructionType;
import java.io.*;
import java.util.*;

public class InstructionParser {

    public static List<Instruction> parseFile(String filePath) throws IOException {
        List<Instruction> instructions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                Instruction inst = parseLine(line, id++);
                if (inst != null) {
                    instructions.add(inst);
                }
            }
        }

        return instructions;
    }

    public static Instruction parseLine(String line, int id) {
        line = line.trim().replaceAll("\\s+", " ");
        String[] parts = line.split("\\s+", 2);

        if (parts.length < 1) {
            return null;
        }

        String opcode = parts[0].toUpperCase();
        InstructionType type;

        try {
            type = InstructionType.fromString(opcode);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown instruction: " + opcode);
            return null;
        }

        if (parts.length < 2) {
            return null;
        }

        String operands = parts[1].replace(",", " ").trim();
        String[] ops = operands.split("\\s+");

        Instruction.Builder builder = new Instruction.Builder()
                .id(id)
                .type(type);

        switch (type.getOperationType()) {
            case FP_ADD:
            case FP_MUL:
            case FP_DIV:
                // Format: ADD.D F0, F2, F4
                if (ops.length >= 3) {
                    builder.destination(ops[0])
                            .source1(ops[1])
                            .source2(ops[2]);
                }
                break;

            case INTEGER:
                // Format: ADDI R1, R2, 100
                if (ops.length >= 3) {
                    builder.destination(ops[0])
                            .source1(ops[1])
                            .immediate(parseInteger(ops[2]));
                }
                break;

            case LOAD:
                // Format: L.D F6, 0(R1) or L.D F6, 100(R2)
                if (ops.length >= 2) {
                    builder.destination(ops[0]);

                    String addrStr = ops[1];
                    int openParen = addrStr.indexOf('(');
                    int closeParen = addrStr.indexOf(')');

                    if (openParen != -1 && closeParen != -1) {
                        String offset = addrStr.substring(0, openParen);
                        String base = addrStr.substring(openParen + 1, closeParen);

                        builder.immediate(offset.isEmpty() ? 0 : parseInteger(offset))
                                .source1(base);
                    }
                }
                break;

            case STORE:
                // Format: S.D F6, 0(R1)
                if (ops.length >= 2) {
                    builder.source2(ops[0]); // value to store

                    String addrStr = ops[1];
                    int openParen = addrStr.indexOf('(');
                    int closeParen = addrStr.indexOf(')');

                    if (openParen != -1 && closeParen != -1) {
                        String offset = addrStr.substring(0, openParen);
                        String base = addrStr.substring(openParen + 1, closeParen);

                        builder.immediate(offset.isEmpty() ? 0 : parseInteger(offset))
                                .source1(base);
                    }
                }
                break;

            case BRANCH:
                // Format: BEQ R1, R2, 100
                if (ops.length >= 3) {
                    builder.source1(ops[0])
                            .source2(ops[1])
                            .address(parseInteger(ops[2]));
                }
                break;
        }

        return builder.build();
    }

    private static int parseInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
