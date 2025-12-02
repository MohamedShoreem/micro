package com.microprocessor.tomasulo.model;

import java.util.HashMap;
import java.util.Map;

public class RegisterFile {
    private final Map<String, Double> values;
    private final Map<String, String> qi; // ROB entry producing value

    public RegisterFile() {
        values = new HashMap<>();
        qi = new HashMap<>();
        initializeRegisters();
    }

    private void initializeRegisters() {
        // Integer registers
        for (int i = 0; i < 32; i++) {
            values.put("R" + i, 0.0);
            qi.put("R" + i, null);
        }

        // Floating point registers
        for (int i = 0; i < 32; i++) {
            values.put("F" + i, 0.0);
            qi.put("F" + i, null);
        }
    }

    public double getValue(String register) {
        return values.getOrDefault(register, 0.0);
    }

    public void setValue(String register, double value) {
        values.put(register, value);
    }

    public String getQi(String register) {
        return qi.get(register);
    }

    public void setQi(String register, String robEntry) {
        qi.put(register, robEntry);
    }

    public void clearQi(String register) {
        qi.put(register, null);
    }

    public void updateFromCDB(String robName, double value) {
        // Update all registers waiting for this ROB entry
        for (String register : qi.keySet()) {
            if (robName.equals(qi.get(register))) {
                qi.put(register, null);
            }
        }
    }

    public Map<String, Double> getAllValues() {
        return new HashMap<>(values);
    }

    public Map<String, String> getAllQi() {
        return new HashMap<>(qi);
    }
}
