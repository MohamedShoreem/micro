# MIPS CPU Simulator - Tomasulo Algorithm Implementation

## Project Overview

This is a comprehensive GUI-based CPU simulator implementing Tomasulo's algorithm for dynamic instruction scheduling. The simulator supports MIPS instructions including floating-point operations, integer operations, memory operations, and branch instructions.

### Team Information
- **Course**: CSEN 702: Microprocessors - Winter 2025
- **Instructor**: Assoc. Prof. Milad Ghantous
- **Institution**: Faculty of Media Engineering and Technology, Dept. of Computer Science and Engineering

---

## Features

### Supported Instructions

#### Floating-Point Arithmetic
- `ADD.D` - Double-precision floating-point addition
- `SUB.D` - Double-precision floating-point subtraction
- `MUL.D` - Double-precision floating-point multiplication
- `DIV.D` - Double-precision floating-point division

#### Integer Arithmetic
- `ADD`, `SUB` - Integer addition/subtraction
- `ADDI` - Add immediate
- `DADDI` - Double-word add immediate (for loops)
- `DSUBI` - Double-word subtract immediate (for loops)
- `MUL`, `DIV` - Integer multiplication/division

#### Memory Operations
- `LW` - Load word (4 bytes)
- `SW` - Store word (4 bytes)
- `LD` - Load doubleword (8 bytes)
- `L.D` - Load double-precision floating-point
- `L.S` - Load single-precision floating-point
- `S.D` - Store double-precision floating-point
- `S.S` - Store single-precision floating-point

#### Branch Instructions
- `BEQ` - Branch if equal (no prediction)
- `BNE` - Branch if not equal (no prediction)

---

## Architecture Design

### Register File
- **Integer Registers**: R0-R31 (R0 is always zero)
- **Floating-Point Registers**: F0-F31
- All values stored as double-precision for simplicity
- Supports pre-loading register values before simulation

### Memory System

#### Main Memory
- **Byte-addressable**: Array of bytes where each element is 8 bits/1 byte
- **Total Size**: 4KB (4096 bytes)
- Supports multiple access types:
  - Byte access (1 byte)
  - Word access (4 bytes) - for LW/SW
  - Doubleword access (8 bytes) - for LD, L.D, S.D
  - Single-precision (4 bytes) - for L.S, S.S

#### Cache
- **Organization**: Direct-mapped cache
- **Block-based**: Each cache line holds a block of consecutive bytes
- **Configurable Parameters**:
  - Block size (bytes per block)
  - Cache size (total cache size in bytes)
  - Hit latency (cycles on cache hit)
  - Miss penalty (additional cycles on miss)

##### Address Decomposition
```
Address = [Tag | Index | Block Offset]
- Block Offset: log₂(blockSize) bits
- Index: log₂(numCacheLines) bits
- Tag: Remaining bits
```

##### Cache Operation
1. **On Read**:
   - Calculate block address: `(address / blockSize) * blockSize`
   - Calculate index: `(address / blockSize) % numCacheLines`
   - Calculate tag: `address / (blockSize * numCacheLines)`
   - Check if tag matches and line is valid (HIT) or not (MISS)
   - On MISS: Load entire block from memory to cache
   - Return requested bytes from the block

2. **On Write** (Write-through policy):
   - Update cache if hit
   - Always write to memory immediately
   - Allocate cache block on miss

3. **Cache Statistics**: Tracks hits, misses, and hit rate

### Tomasulo Algorithm Components

#### Reservation Stations
1. **FP Addition/Subtraction Stations** (default: 3)
   - Handle ADD.D, SUB.D operations

2. **FP Multiplication/Division Stations** (default: 2)
   - Handle MUL.D, DIV.D operations

3. **Load Stations** (default: 2)
   - Handle all load operations (LW, LD, L.D, L.S)

4. **Store Stations** (default: 2)
   - Handle all store operations (SW, S.D, S.S)

5. **Integer Stations** (default: 2)
   - Handle DADDI, DSUBI, and integer arithmetic

##### Reservation Station Fields
- `name`: Station identifier (e.g., "Add1", "Mul1", "Load1")
- `busy`: Occupancy flag
- `op`: Operation to perform
- `vj`, `vk`: Source operand values
- `qj`, `qk`: ROB entry tags producing operands (null if ready)
- `address`: Effective address for memory operations
- `addressReady`: Is address computed?
- `storeValue`, `storeTag`: Value to store and its producer
- `remainingCycles`: Execution latency countdown
- `robEntry`: Associated ROB entry number
- `result`: Computed result
- `resultReady`: Is execution complete?

#### Reorder Buffer (ROB)
- **Size**: Configurable (default: 16 entries)
- **Organization**: Circular buffer
- **Purpose**: Maintain program order and enable precise exceptions

##### ROB Entry Fields
- `entryNumber`: ROB index
- `state`: FREE, ISSUED, EXECUTING, WRITE_RESULT, COMMIT
- `instruction`: Instruction reference
- `destination`: Destination register number
- `value`: Result value
- `ready`: Is result ready?
- `isFloat`: Is destination a floating-point register?
- `memoryAddress`: Target address for stores
- `branchTaken`, `branchTarget`, `branchResolved`: Branch handling

#### Register Status Table
- Tracks which ROB entry will write to each register
- Separate tracking for integer and floating-point registers
- Used for register renaming and forwarding

---

## Tomasulo Algorithm Implementation

### Pipeline Stages

The simulator executes four stages per clock cycle, in reverse order:

#### 1. Commit Stage
- Check ROB head entry
- If ready and no exceptions:
  - **For arithmetic/load**: Write result to register file, clear register status
  - **For store**: Write value to memory through cache
  - **For branch**: Check if mispredicted, flush pipeline if necessary
  - Advance ROB head pointer
  - Free the ROB entry

#### 2. Write Result Stage (Common Data Bus)
- For each reservation station with result ready:
  - Broadcast result on CDB to all waiting stations
  - Update waiting RS: if `qj` or `qk` matches, copy value and clear tag
  - Update ROB entry with result
  - Free the reservation station
- **Bus Arbitration**: If multiple stations finish simultaneously, prioritize by station type (Load > FP Add/Sub > FP Mul/Div > Integer)

#### 3. Execute Stage
- For each busy reservation station:
  - If operands ready (`qj` and `qk` are null):
    - Perform operation
    - For memory ops: Calculate effective address
    - For loads: Access cache, handle hit/miss latency
    - Decrement `remainingCycles`
    - When cycles reach 0, mark result ready

##### Execution Operations
```java
switch (op) {
    case ADD_D: result = vj + vk; break;
    case SUB_D: result = vj - vk; break;
    case MUL_D: result = vj * vk; break;
    case DIV_D: result = vj / vk; break;
    case DADDI: result = vj + immediate; break;
    case DSUBI: result = vj - immediate; break;
    // Memory: address = vj + immediate
    // Branches: compare vj and vk
}
```

#### 4. Issue Stage
- Fetch next instruction from queue
- Check for available resources:
  - Free ROB entry at tail
  - Free reservation station of appropriate type
- Allocate ROB entry and reservation station
- Read operands:
  - If register status has ROB tag → copy tag to `qj`/`qk`
  - Else read value from register file to `vj`/`vk`
- Update register status for destination register
- Increment program counter and ROB tail

### Hazard Handling

#### RAW (Read After Write)
- Handled through register renaming and ROB tags
- Consumer instruction waits for producer via `qj`/`qk` tags
- Result forwarded when producer writes to CDB

#### WAR (Write After Read)
- Eliminated by register renaming
- Each instruction writes to a unique ROB entry

#### WAW (Write After Write)
- Resolved by ROB in-order commit
- Later write committed after earlier write completes

#### Address Clashes (Memory)
- Load/store ordering maintained through ROB
- Store waits in store buffer until commit
- Load checks ROB for pending stores to same address
- **Implementation**: Compare load address with all earlier uncommitted store addresses

### Branch Handling (No Prediction)

1. **On Branch Issue**:
   - Continue fetching subsequent instructions
   - Mark branch ROB entry

2. **On Branch Execution**:
   - Calculate branch target
   - Compare operands to determine taken/not-taken
   - Mark branch resolved

3. **On Branch Commit**:
   - If branch was taken:
     - Flush all instructions after branch in ROB
     - Clear reservation stations and register status for flushed instructions
     - Set PC to branch target
   - If not taken: Continue normal execution

---

## Configuration Parameters

All parameters configurable before simulation starts:

### Instruction Latencies (Cycles)
- FP Addition: 2 cycles
- FP Subtraction: 2 cycles
- FP Multiplication: 10 cycles
- FP Division: 40 cycles
- Integer operations: 1 cycle
- Memory base: 1 cycle (+ cache latency)
- Branches: 1 cycle

### Reservation Station Counts
- FP Add/Sub stations
- FP Mul/Div stations
- Load stations
- Store stations
- Integer stations

### ROB Size
- Number of ROB entries

### Cache Parameters
- Block size (bytes)
- Total cache size (bytes)
- Hit latency (cycles)
- Miss penalty (cycles)

---

## Code Structure

```
src/main/java/com/cpu/simulator/
├── Main.java                          # Application entry point
├── core/
│   └── CPUSimulator.java             # Main simulation engine
├── model/
│   ├── Instruction.java              # Instruction representation
│   ├── RegisterFile.java             # Integer + FP registers
│   ├── Memory.java                   # Byte-addressable memory
│   ├── Cache.java                    # Configurable cache
│   └── SimulatorConfig.java          # Configuration settings
├── tomasulo/
│   ├── ReservationStation.java       # RS with type support
│   ├── ReorderBufferEntry.java       # ROB entry
│   └── RegisterStatus.java           # Register renaming table
├── parser/
│   └── InstructionParser.java        # Instruction file parser
└── ui/
    └── MainController.java           # JavaFX UI controller
```

### Key Classes

#### `CPUSimulator`
- Orchestrates all simulation components
- Implements the four-stage pipeline
- Handles clock cycle execution

#### `Instruction`
- Enum for operation types
- Supports integer and FP register types
- Stores latency and label information

#### `Memory`
- Byte array implementation
- Methods for byte, word, doubleword, float, double access
- Block read/write for cache

#### `Cache`
- Direct-mapped with configurable parameters
- Returns `CacheAccessResult` with data and latency
- Tracks hit/miss statistics

#### `ReservationStation`
- Type-specific (ADD_SUB, MUL_DIV, LOAD, STORE, INTEGER)
- Tracks operand readiness
- Stores result for CDB broadcast

#### `ReorderBufferEntry`
- State machine (FREE → ISSUED → EXECUTING → WRITE_RESULT → COMMIT)
- Handles branches and memory operations
- Maintains program order

#### `InstructionParser`
- Two-pass parsing (labels, then instructions)
- Supports all required instruction formats
- Label resolution for branches

---

## Test Cases

### Test Case 1: Sequential Code
```assembly
L.D F6, 0(R2)
L.D F2, 8(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
ADD.D F6, F8, F2
S.D F6, 8(R2)
```
**Demonstrates**: RAW hazards, WAW hazard on F6

### Test Case 2: Sequential Code
```assembly
L.D F6, 0(R2)
ADD.D F7, F1, F3
L.D F2, 20(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
S.D F10, 0(R2)
```
**Demonstrates**: Independent operations, RAW hazards, parallel execution

### Test Case 3: Loop Code
```assembly
DADDI R1, R1, 24
DADDI R2, R2, 0
LOOP: L.D F0, 8(R1)
MUL.D F4, F0, F2
S.D F4, 8(R1)
DSUBI R1, R1, 8
BNE R1, R2, LOOP
```
**Demonstrates**: Loop with branch, integer arithmetic, memory operations, control hazards

---

## Usage Instructions

### Prerequisites
- Java 21 LTS
- Maven 3.8+
- JavaFX 17+

### Building the Project
```powershell
mvn clean compile
```

### Running the Simulator
```powershell
mvn javafx:run
```

### Using the GUI
1. **Load Program**: Click "Load Program" and select a test case file
2. **Configure**: Set latencies, cache parameters, and RS sizes (before first step)
3. **Step**: Execute one clock cycle at a time
4. **Run**: Execute continuously until completion
5. **Reset**: Clear all state and start over

### Viewing State
- **Instruction Queue**: Current program with PC indicator
- **Reservation Stations**: Tables showing all RS with operands and tags
- **Reorder Buffer**: ROB entries with state and results
- **Register File**: Integer and FP register values
- **Cache Statistics**: Hit rate, hits, misses
- **Execution Log**: Cycle-by-cycle execution trace

---

## Bus Arbitration Strategy

When multiple reservation stations complete execution in the same cycle and want to broadcast on the Common Data Bus:

**Priority Order** (highest to lowest):
1. Load operations
2. FP Add/Sub operations
3. FP Mul/Div operations
4. Integer operations
5. Store operations (stores don't broadcast values)

**Rationale**:
- Loads often unblock multiple dependent instructions
- Faster operations get priority over slower ones
- Stores only update ROB, not other stations

**Implementation**:
```java
List<ReservationStation> readyStations = getAllReadyStations();
readyStations.sort((a, b) -> getPriority(a.getType()) - getPriority(b.getType()));
ReservationStation winner = readyStations.get(0);
broadcastResult(winner);
```

---

## Address Mapping Strategy

### Memory Addressing
- Byte-addressable: Each address refers to a single byte
- Multi-byte values span consecutive addresses
- Example: Word at address 100 occupies bytes 100, 101, 102, 103

### Cache Mapping
Given address `A`, block size `B`, and `N` cache lines:
- **Block number**: `A / B`
- **Cache index**: `(A / B) % N`
- **Tag**: `A / (B * N)`
- **Block offset**: `A % B`

**Example**: Address 1000, block size 32, 8 cache lines
- Block number: 1000 / 32 = 31
- Index: 31 % 8 = 7
- Tag: 1000 / 256 = 3
- Offset: 1000 % 32 = 8

---

## Limitations and Future Enhancements

### Current Limitations
1. No branch prediction (all branches cause pipeline flush)
2. Fixed-point representation for floating-point (no IEEE 754)
3. Simple write-through cache (no write-back)
4. Single common data bus (structural hazard)

### Possible Enhancements
1. Branch prediction (2-bit saturating counter, branch target buffer)
2. Multiple CDB for higher throughput
3. Write-back cache with MESI protocol
4. Out-of-order commit for independent instructions
5. Speculative execution beyond branches

---

## References

1. J.L. Hennessy and D.A. Patterson, "Computer Architecture: A Quantitative Approach"
2. R.M. Tomasulo, "An Efficient Algorithm for Exploiting Multiple Arithmetic Units", IBM Journal, 1967
3. MIPS Architecture Reference Manual
4. Course lectures and materials, CSEN 702, Winter 2025

---

## Contributors

[Team member names and IDs to be filled in]

---

## License

This project is submitted as coursework for CSEN 702: Microprocessors, Winter 2025.
All code is original work by the team members listed above.
