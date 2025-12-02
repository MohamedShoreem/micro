package com.microprocessor.tomasulo.model;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    private final int blockSize;
    private final int cacheSize;
    private final int hitLatency;
    private final int missPenalty;
    private final int numBlocks;

    private final Map<Integer, CacheBlock> cacheBlocks;
    private final Map<Integer, byte[]> memory;

    public Cache(int blockSize, int cacheSize, int hitLatency, int missPenalty) {
        this.blockSize = blockSize;
        this.cacheSize = cacheSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.numBlocks = cacheSize / blockSize;
        this.cacheBlocks = new HashMap<>();
        this.memory = new HashMap<>();
    }

    public AccessResult access(int address, boolean isLoad) {
        int blockNumber = address / blockSize;
        int cacheIndex = blockNumber % numBlocks;

        CacheBlock block = cacheBlocks.get(cacheIndex);

        if (block != null && block.getTag() == blockNumber) {
            // Cache hit
            return new AccessResult(true, hitLatency, address);
        } else {
            // Cache miss
            loadBlockIntoCache(blockNumber, cacheIndex);
            return new AccessResult(false, hitLatency + missPenalty, address);
        }
    }

    private void loadBlockIntoCache(int blockNumber, int cacheIndex) {
        int startAddress = blockNumber * blockSize;
        byte[] data = new byte[blockSize];

        // Load from memory
        byte[] memBlock = memory.get(blockNumber);
        if (memBlock != null) {
            System.arraycopy(memBlock, 0, data, 0, blockSize);
        }

        CacheBlock block = new CacheBlock(blockNumber, data);
        cacheBlocks.put(cacheIndex, block);
    }

    public void writeMemory(int address, int value) {
        int blockNumber = address / blockSize;
        int offset = address % blockSize;

        byte[] block = memory.computeIfAbsent(blockNumber, k -> new byte[blockSize]);

        // Write 4 bytes (int/float)
        block[offset] = (byte) (value >> 24);
        block[offset + 1] = (byte) (value >> 16);
        block[offset + 2] = (byte) (value >> 8);
        block[offset + 3] = (byte) value;
    }

    public int readMemory(int address) {
        int blockNumber = address / blockSize;
        int offset = address % blockSize;

        byte[] block = memory.get(blockNumber);
        if (block == null) {
            return 0;
        }

        // Read 4 bytes
        return ((block[offset] & 0xFF) << 24) |
                ((block[offset + 1] & 0xFF) << 16) |
                ((block[offset + 2] & 0xFF) << 8) |
                (block[offset + 3] & 0xFF);
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getHitLatency() {
        return hitLatency;
    }

    public int getMissPenalty() {
        return missPenalty;
    }

    public static class AccessResult {
        private final boolean hit;
        private final int latency;
        private final int address;

        public AccessResult(boolean hit, int latency, int address) {
            this.hit = hit;
            this.latency = latency;
            this.address = address;
        }

        public boolean isHit() {
            return hit;
        }

        public int getLatency() {
            return latency;
        }

        public int getAddress() {
            return address;
        }
    }

    private static class CacheBlock {
        private final int tag;
        private final byte[] data;

        public CacheBlock(int tag, byte[] data) {
            this.tag = tag;
            this.data = data;
        }

        public int getTag() {
            return tag;
        }

        public byte[] getData() {
            return data;
        }
    }
}
