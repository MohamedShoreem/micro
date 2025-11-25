package com.cpu.simulator.model;

import java.nio.ByteBuffer;

/**
 * Memory represents the main memory of the CPU.
 * Byte-addressable memory implementation where each element is 8 bits/1 byte.
 * Supports word (4 bytes), doubleword (8 bytes), single-precision (4 bytes), 
 * and double-precision (8 bytes) access.
 */
public class Memory {
    private byte[] memory;
    private static final int MEMORY_SIZE = 4096; // 4KB byte-addressable memory
    
    public Memory() {
        memory = new byte[MEMORY_SIZE];
    }
    
    /**
     * Read a single byte from memory
     * @param address Byte address
     * @return Byte value at the address
     */
    public byte readByte(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        return memory[address];
    }
    
    /**
     * Write a single byte to memory
     * @param address Byte address
     * @param value Byte value to write
     */
    public void writeByte(int address, byte value) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        memory[address] = value;
    }
    
    /**
     * Read a word (4 bytes) from memory
     * @param address Starting byte address
     * @return 32-bit integer value
     */
    public int readWord(int address) {
        if (address < 0 || address + 3 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        // Construct 4-byte word from bytes (big-endian)
        return ByteBuffer.wrap(memory, address, 4).getInt();
    }
    
    /**
     * Write a word (4 bytes) to memory
     * @param address Starting byte address
     * @param value 32-bit integer value
     */
    public void writeWord(int address, int value) {
        if (address < 0 || address + 3 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        // Write 4 bytes (big-endian)
        ByteBuffer.allocate(4).putInt(value).array();
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        System.arraycopy(bytes, 0, memory, address, 4);
    }
    
    /**
     * Read a doubleword (8 bytes) from memory
     * @param address Starting byte address
     * @return 64-bit long value
     */
    public long readDoubleWord(int address) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        return ByteBuffer.wrap(memory, address, 8).getLong();
    }
    
    /**
     * Write a doubleword (8 bytes) to memory
     * @param address Starting byte address
     * @param value 64-bit long value
     */
    public void writeDoubleWord(int address, long value) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        byte[] bytes = ByteBuffer.allocate(8).putLong(value).array();
        System.arraycopy(bytes, 0, memory, address, 8);
    }
    
    /**
     * Read a single-precision floating point (4 bytes) from memory
     * @param address Starting byte address
     * @return Single-precision float value
     */
    public float readSingle(int address) {
        if (address < 0 || address + 3 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        return ByteBuffer.wrap(memory, address, 4).getFloat();
    }
    
    /**
     * Write a single-precision floating point (4 bytes) to memory
     * @param address Starting byte address
     * @param value Single-precision float value
     */
    public void writeSingle(int address, float value) {
        if (address < 0 || address + 3 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        byte[] bytes = ByteBuffer.allocate(4).putFloat(value).array();
        System.arraycopy(bytes, 0, memory, address, 4);
    }
    
    /**
     * Read a double-precision floating point (8 bytes) from memory
     * @param address Starting byte address
     * @return Double-precision value
     */
    public double readDouble(int address) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        return ByteBuffer.wrap(memory, address, 8).getDouble();
    }
    
    /**
     * Write a double-precision floating point (8 bytes) to memory
     * @param address Starting byte address
     * @param value Double-precision value
     */
    public void writeDouble(int address, double value) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        byte[] bytes = ByteBuffer.allocate(8).putDouble(value).array();
        System.arraycopy(bytes, 0, memory, address, 8);
    }
    
    /**
     * Read multiple bytes as a block
     * @param address Starting byte address
     * @param size Number of bytes to read
     * @return Array of bytes
     */
    public byte[] readBlock(int address, int size) {
        if (address < 0 || address + size > MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address range: " + address + " - " + (address + size));
        }
        byte[] block = new byte[size];
        System.arraycopy(memory, address, block, 0, size);
        return block;
    }
    
    /**
     * Write multiple bytes as a block
     * @param address Starting byte address
     * @param data Array of bytes to write
     */
    public void writeBlock(int address, byte[] data) {
        if (address < 0 || address + data.length > MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address range: " + address + " - " + (address + data.length));
        }
        System.arraycopy(data, 0, memory, address, data.length);
    }
    
    /**
     * Get memory size in bytes
     * @return Size of memory
     */
    public int getSize() {
        return MEMORY_SIZE;
    }
    
    /**
     * Reset all memory to 0
     */
    public void reset() {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
    }
}
