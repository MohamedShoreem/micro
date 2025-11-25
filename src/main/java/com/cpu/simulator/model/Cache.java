package com.cpu.simulator.model;

/**
 * Cache represents a direct-mapped cache with configurable parameters.
 * Implements block-based caching with configurable block size, cache size,
 * hit latency, and miss penalty.
 * 
 * Cache Organization:
 * - Direct-mapped: Each memory block maps to exactly one cache line
 * - Block-based: Each cache line holds a block of consecutive bytes
 * - Address decomposition: [Tag | Index | Block Offset]
 *   - Block Offset: log2(blockSize) bits
 *   - Index: log2(numCacheLines) bits
 *   - Tag: Remaining bits
 */
public class Cache {
    private CacheLine[] cacheLines;
    private Memory memory;
    
    // Configurable parameters
    private int blockSize;      // Bytes per block (e.g., 4, 8, 16, 32)
    private int cacheSize;      // Total cache size in bytes
    private int numCacheLines;  // Number of cache lines = cacheSize / blockSize
    private int hitLatency;     // Cycles for cache hit
    private int missPenalty;    // Additional cycles for cache miss
    
    // Statistics
    private int hits;
    private int misses;
    
    /**
     * Create cache with default parameters
     * @param memory Reference to main memory
     */
    public Cache(Memory memory) {
        this(memory, 32, 256, 1, 10); // Default: 32-byte blocks, 256-byte cache
    }
    
    /**
     * Create cache with custom parameters
     * @param memory Reference to main memory
     * @param blockSize Size of each cache block in bytes
     * @param cacheSize Total cache size in bytes
     * @param hitLatency Cycles to access on cache hit
     * @param missPenalty Additional cycles on cache miss
     */
    public Cache(Memory memory, int blockSize, int cacheSize, int hitLatency, int missPenalty) {
        this.memory = memory;
        this.blockSize = blockSize;
        this.cacheSize = cacheSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.numCacheLines = cacheSize / blockSize;
        
        cacheLines = new CacheLine[numCacheLines];
        for (int i = 0; i < numCacheLines; i++) {
            cacheLines[i] = new CacheLine(blockSize);
        }
        
        hits = 0;
        misses = 0;
    }
    
    /**
     * Configure cache parameters (must be called before simulation)
     * @param blockSize Size of each cache block in bytes
     * @param cacheSize Total cache size in bytes
     * @param hitLatency Cycles to access on cache hit
     * @param missPenalty Additional cycles on cache miss
     */
    public void configure(int blockSize, int cacheSize, int hitLatency, int missPenalty) {
        this.blockSize = blockSize;
        this.cacheSize = cacheSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.numCacheLines = cacheSize / blockSize;
        
        cacheLines = new CacheLine[numCacheLines];
        for (int i = 0; i < numCacheLines; i++) {
            cacheLines[i] = new CacheLine(blockSize);
        }
        
        hits = 0;
        misses = 0;
    }
    
    /**
     * Read data from cache (loads block from memory on miss)
     * @param address Byte address in memory
     * @param size Number of bytes to read (1, 4, or 8)
     * @return Access latency in cycles
     */
    public CacheAccessResult read(int address, int size) {
        int blockAddress = (address / blockSize) * blockSize; // Start of block
        int index = (address / blockSize) % numCacheLines;
        int tag = address / (blockSize * numCacheLines);
        int offset = address % blockSize;
        
        CacheLine line = cacheLines[index];
        
        // Check for cache hit
        if (line.valid && line.tag == tag) {
            // Cache hit
            hits++;
            byte[] data = new byte[size];
            System.arraycopy(line.data, offset, data, 0, size);
            return new CacheAccessResult(true, data, hitLatency);
        } else {
            // Cache miss - load block from memory
            misses++;
            line.valid = true;
            line.tag = tag;
            line.data = memory.readBlock(blockAddress, blockSize);
            
            byte[] data = new byte[size];
            System.arraycopy(line.data, offset, data, 0, size);
            return new CacheAccessResult(false, data, hitLatency + missPenalty);
        }
    }
    
    /**
     * Write data to cache (write-through policy)
     * @param address Byte address in memory
     * @param data Data to write
     * @return Access latency in cycles
     */
    public CacheAccessResult write(int address, byte[] data) {
        int blockAddress = (address / blockSize) * blockSize;
        int index = (address / blockSize) % numCacheLines;
        int tag = address / (blockSize * numCacheLines);
        int offset = address % blockSize;
        
        CacheLine line = cacheLines[index];
        
        // Write to memory (write-through)
        memory.writeBlock(address, data);
        
        // Check for cache hit
        if (line.valid && line.tag == tag) {
            // Cache hit - update cache block
            hits++;
            System.arraycopy(data, 0, line.data, offset, data.length);
            return new CacheAccessResult(true, data, hitLatency);
        } else {
            // Cache miss - allocate block
            misses++;
            line.valid = true;
            line.tag = tag;
            line.data = memory.readBlock(blockAddress, blockSize);
            System.arraycopy(data, 0, line.data, offset, data.length);
            return new CacheAccessResult(false, data, hitLatency + missPenalty);
        }
    }
    
    /**
     * Read word (4 bytes) from cache
     * @param address Byte address
     * @return Value and latency
     */
    public CacheAccessResult readWord(int address) {
        return read(address, 4);
    }
    
    /**
     * Read double (8 bytes) from cache
     * @param address Byte address
     * @return Value and latency
     */
    public CacheAccessResult readDouble(int address) {
        return read(address, 8);
    }
    
    /**
     * Reset cache (invalidate all lines)
     */
    public void reset() {
        for (int i = 0; i < numCacheLines; i++) {
            cacheLines[i].valid = false;
            cacheLines[i].tag = 0;
        }
        hits = 0;
        misses = 0;
    }
    
    // Getters
    public int getBlockSize() { return blockSize; }
    public int getCacheSize() { return cacheSize; }
    public int getNumCacheLines() { return numCacheLines; }
    public int getHitLatency() { return hitLatency; }
    public int getMissPenalty() { return missPenalty; }
    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public double getHitRate() { 
        int total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }
    
    /**
     * Inner class representing a cache line
     */
    private static class CacheLine {
        boolean valid;
        int tag;
        byte[] data;  // Block of data
        
        CacheLine(int blockSize) {
            this.valid = false;
            this.tag = 0;
            this.data = new byte[blockSize];
        }
    }
    
    /**
     * Result of a cache access operation
     */
    public static class CacheAccessResult {
        private boolean hit;
        private byte[] data;
        private int latency;
        
        public CacheAccessResult(boolean hit, byte[] data, int latency) {
            this.hit = hit;
            this.data = data;
            this.latency = latency;
        }
        
        public boolean isHit() { return hit; }
        public byte[] getData() { return data; }
        public int getLatency() { return latency; }
    }
}
