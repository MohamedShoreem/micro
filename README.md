# Tomasulo Algorithm Simulator

A complete JavaFX-based simulator implementing Tomasulo's algorithm for dynamic instruction scheduling.

## Features

- **Full Tomasulo Implementation**
  - Reservation stations for Add/Sub, Mul/Div operations
  - Load and Store buffers
  - Reorder Buffer (ROB) for in-order commit
  - Common Data Bus (CDB) for result broadcasting
- **Hazard Handling**
  - RAW (Read-After-Write) dependencies
  - WAR (Write-After-Read) prevention via register renaming
  - WAW (Write-After-Write) prevention via ROB
  - Structural hazards (limited reservation stations)
- **Memory Subsystem**
  - Configurable cache with block mapping
  - Cache hit/miss simulation
  - Byte-addressable memory
- **Instruction Support**
  - Floating Point: ADD.D, SUB.D, MUL.D, DIV.D
  - Integer: ADDI, SUBI
  - Load: LW, LD, L.S, L.D
  - Store: SW, S.D
  - Branch: BEQ, BNE
- **Configurable Parameters**
  - Execution latencies for each operation type
  - Cache parameters (size, block size, hit latency, miss penalty)
  - Number of reservation stations and buffers
  - ROB size
  - Initial register values
- **GUI Features**
  - Cycle-by-cycle execution visualization
  - Step and Run modes
  - Real-time display of all hardware state
  - Instruction timing table (Issue, Execute, Write, Commit)
  - Log output

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/microprocessor/tomasulo/
│           ├── TomasuloApp.java           # Main entry point
│           ├── model/                      # Data models
│           │   ├── Instruction.java
│           │   ├── InstructionType.java
│           │   ├── OperationType.java
│           │   ├── ReservationStation.java
│           │   ├── ROBEntry.java
│           │   ├── RegisterFile.java
│           │   ├── Cache.java
│           │   └── SimulatorConfig.java
│           ├── core/                       # Simulation engine
│           │   └── TomasuloSimulator.java
│           ├── util/                       # Utilities
│           │   └── InstructionParser.java
│           └── ui/                         # JavaFX GUI
│               └── SimulatorUI.java
└── test_programs/                          # Sample programs
    ├── test1_basic_fp.txt
    ├── test2_loop.txt
    ├── test3_hazards.txt
    └── test4_complex.txt
```

## Building and Running

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build

```bash
mvn clean package
```

### Run

```bash
mvn javafx:run
```

Or run directly:

```bash
java --module-path path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/tomasulo-simulator-1.0-SNAPSHOT.jar
```

## Usage

1. **Load Instructions**
   - Click "Load Instructions" button
   - Select a test program file (.txt)
2. **Configure Parameters** (optional)
   - Adjust latencies in the configuration panel
   - Click "Apply Configuration"
3. **Run Simulation**
   - Click "Step" to execute one cycle at a time
   - Click "Run" to execute continuously
   - Click "Reset" to restart
4. **View Results**
   - Instructions tab: Shows timing for each instruction
   - Reservation Stations tab: Shows current RS state
   - Reorder Buffer tab: Shows ROB contents
   - Register File tab: Shows register values and dependencies
   - Log tab: Shows execution log

## Instruction Format

### Floating Point Operations

```
ADD.D F0, F2, F4    # F0 = F2 + F4
SUB.D F0, F2, F4    # F0 = F2 - F4
MUL.D F0, F2, F4    # F0 = F2 * F4
DIV.D F0, F2, F4    # F0 = F2 / F4
```

### Integer Operations

```
ADDI R1, R2, 100    # R1 = R2 + 100
SUBI R1, R2, 50     # R1 = R2 - 50
```

### Load Operations

```
L.D F0, 0(R1)       # F0 = Mem[R1 + 0]
LW R1, 100(R2)      # R1 = Mem[R2 + 100]
```

### Store Operations

```
S.D F0, 0(R1)       # Mem[R1 + 0] = F0
SW R1, 100(R2)      # Mem[R2 + 100] = R1
```

### Branch Operations

```
BEQ R1, R2, 100     # if R1 == R2, jump to PC + 100
BNE R1, R2, -20     # if R1 != R2, jump to PC - 20
```

## Algorithm Details

### Execution Stages

1. **Issue**: Allocate ROB entry and reservation station, read operands or dependencies
2. **Execute**: Wait for operands, then execute when ready
3. **Write Result**: Broadcast result on CDB, update waiting instructions
4. **Commit**: Update architectural state in-order from ROB head

### CDB Conflict Resolution

When multiple instructions complete in the same cycle, the simulator selects the first one to broadcast. Others wait for the next cycle.

### Branch Handling

Branches are not predicted. All instructions enter the pipeline. Branch resolution happens at commit time.

### Cache Simulation

- Direct-mapped cache with configurable size and block size
- Compulsory misses on first access
- Hit/miss affects load/store latency

## Configuration Defaults

- Add/Sub Latency: 2 cycles
- Multiply Latency: 10 cycles
- Divide Latency: 40 cycles
- Integer Latency: 1 cycle
- Load Latency: 2 cycles (+ cache latency)
- Store Latency: 2 cycles (+ cache latency)
- Branch Latency: 1 cycle
- Cache Hit Latency: 1 cycle
- Cache Miss Penalty: 50 cycles
- Block Size: 16 bytes
- Cache Size: 256 bytes
- Add/Sub RS: 3
- Mul/Div RS: 2
- Load Buffers: 3
- Store Buffers: 3
- ROB Size: 6

## Author

Microprocessor Course Project - 2025

## License

Academic use only
