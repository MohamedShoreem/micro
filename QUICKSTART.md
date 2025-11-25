# Quick Start Guide - MIPS CPU Simulator

## Prerequisites

### Required Software
1. **Java Development Kit (JDK) 21 LTS**
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   - Verify installation: `java -version` (should show version 21.x.x)

2. **Apache Maven 3.8+**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **JavaFX SDK 17+** (automatically downloaded by Maven)

### IDE Recommendations
- **IntelliJ IDEA** (Community or Ultimate)
- **Eclipse with JavaFX plugin**
- **VS Code with Java Extension Pack**

---

## Building the Project

### Step 1: Clone/Navigate to Project
```powershell
cd "c:\Users\Mohamed Hany\Documents\GitHub\micro"
```

### Step 2: Clean and Compile
```powershell
mvn clean compile
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

### Step 3: Package (Optional)
```powershell
mvn package
```

This creates an executable JAR in `target/` directory.

---

## Running the Simulator

### Method 1: Using Maven (Recommended)
```powershell
mvn javafx:run
```

### Method 2: Using Java Directly
```powershell
java --module-path "path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "target\classes" com.cpu.simulator.Main
```

### Method 3: From IDE
1. Open project in IntelliJ/Eclipse/VS Code
2. Set main class: `com.cpu.simulator.Main`
3. Ensure JavaFX is configured
4. Click "Run" button

---

## Using the Simulator

### Basic Workflow

#### 1. Configure Simulation (Before First Run)
Before loading a program, you should configure:
- **Instruction Latencies**: Cycles for each operation type
- **Reservation Station Counts**: Number of each RS type
- **ROB Size**: Number of reorder buffer entries  
- **Cache Parameters**: Block size, cache size, hit latency, miss penalty
- **Initial Register Values**: Pre-load R and F registers

*Note: Configuration UI is under development. Currently uses default values.*

#### 2. Load a Program
1. Click **"Load Program"** button
2. Navigate to one of the test case files:
   - `test_case1.txt` - Sequential code with hazards
   - `test_case2.txt` - Mixed dependencies
   - `test_case3.txt` - Loop with branch
3. Click **"Open"**

Program will appear in the Instruction Queue panel.

#### 3. Execute Program

**Option A: Step-by-Step**
- Click **"Step"** button to execute one clock cycle
- Observe changes in:
  - Reservation Stations
  - Reorder Buffer
  - Register File
  - Execution Log

**Option B: Continuous Execution**
- Click **"Run"** button to execute until completion
- Simulation runs automatically
- Click **"Pause"** to stop (if implemented)

#### 4. Reset
- Click **"Reset"** to clear all state
- Reload program and configure again

---

## Understanding the Display

### Instruction Queue (Left Panel)
- Shows all loaded instructions
- `→` marks current Program Counter
- Format: `index: instruction`

### Reservation Stations (Middle Panel)
Shows 5 types of stations:
1. **Add/Sub Stations**: FP addition/subtraction
2. **Mul/Div Stations**: FP multiplication/division  
3. **Load Stations**: All load operations
4. **Store Stations**: All store operations
5. **Integer Stations**: DADDI, DSUBI operations *(if implemented)*

Columns:
- **Name**: Station identifier (e.g., Add1, Mul1)
- **Busy**: Is occupied?
- **Op**: Operation (ADD.D, MUL.D, etc.)
- **Vj, Vk**: Operand values
- **Qj, Qk**: ROB entry tags (null if ready)
- **Cycles**: Remaining execution cycles

### Reorder Buffer (Middle Panel)
Shows all ROB entries with:
- **Entry**: ROB number (0-15 by default)
- **State**: FREE, ISSUED, EXECUTING, WRITE_RESULT, COMMIT
- **Instruction**: Operation being performed
- **Dest**: Destination register (R# or F#)
- **Value**: Result value
- **Ready**: Is result available?

### Register File (Right Panel)
Shows current register values:
- **Integer Registers**: R0-R31 (R0 always 0)
- **Floating-Point Registers**: F0-F31
- **Status**: ROB entry writing to this register (if any)

### Execution Log (Right Panel)
- Chronological trace of execution
- Shows cycle number and events
- Useful for debugging

### Cache Statistics (if displayed)
- **Hits**: Number of cache hits
- **Misses**: Number of cache misses  
- **Hit Rate**: Percentage of accesses that hit

---

## Test Cases

### Test Case 1: Sequential Code
**File**: `test_case1.txt`

```assembly
L.D F6, 0(R2)      # Load F6 from memory
L.D F2, 8(R2)      # Load F2 from memory
MUL.D F0, F2, F4   # F0 = F2 * F4 (RAW on F2)
SUB.D F8, F2, F6   # F8 = F2 - F6 (RAW on F2, F6)
DIV.D F10, F0, F6  # F10 = F0 / F6 (RAW on F0, F6)
ADD.D F6, F8, F2   # F6 = F8 + F2 (WAW on F6, RAW on F8)
S.D F6, 8(R2)      # Store F6 to memory (RAW on F6)
```

**Expected Behavior**:
- MUL.D waits for L.D F2 to complete
- SUB.D waits for both loads
- DIV.D waits for MUL.D and first load
- ADD.D experiences WAW hazard on F6 (resolved by ROB)
- S.D waits for ADD.D result

**Configuration Tip**: Set R2=100, F4=2.0 for meaningful results

---

### Test Case 2: Sequential Code  
**File**: `test_case2.txt`

```assembly
L.D F6, 0(R2)      # Load F6
ADD.D F7, F1, F3   # Independent operation
L.D F2, 20(R2)     # Independent load
MUL.D F0, F2, F4   # Depends on 2nd load
SUB.D F8, F2, F6   # Depends on both loads
DIV.D F10, F0, F6  # Depends on MUL and 1st load
S.D F10, 0(R2)     # Store result
```

**Expected Behavior**:
- ADD.D can execute in parallel with loads
- Two loads can execute in parallel (if 2 load stations)
- Demonstrates out-of-order execution

**Configuration Tip**: Set R2=100, F1=1.0, F3=2.0, F4=3.0

---

### Test Case 3: Loop Code
**File**: `test_case3.txt`

```assembly
DADDI R1, R1, 24     # Initialize R1 = 24
DADDI R2, R2, 0      # Initialize R2 = 0
LOOP: L.D F0, 8(R1)  # Load from array
MUL.D F4, F0, F2     # Multiply by F2
S.D F4, 8(R1)        # Store back
DSUBI R1, R1, 8      # Decrement pointer
BNE R1, R2, LOOP     # Branch if not equal
```

**Expected Behavior**:
- Loop iterates 3 times (24, 16, 8 → 0)
- Branch causes pipeline flush when taken
- Integer operations use Integer RS
- Memory operations access cache

**Configuration Tip**: 
- Set R1=0, R2=0, F2=2.0 initially
- Pre-load memory at addresses 8, 16, 24 with values

---

## Troubleshooting

### Issue: "JavaFX components are missing"
**Solution**: 
```powershell
mvn clean install
mvn javafx:run
```

### Issue: "Main class not found"
**Solution**: Check that `pom.xml` has:
```xml
<mainClass>com.cpu.simulator.Main</mainClass>
```

### Issue: "NullPointerException in UI"
**Cause**: UI elements not fully implemented
**Solution**: Check IMPLEMENTATION_STATUS.md for what's pending

### Issue: Program loads but nothing happens on "Step"
**Cause**: CPUSimulator stages not fully implemented
**Solution**: See IMPLEMENTATION_STATUS.md for required code

### Issue: Cache always shows 0% hit rate
**Cause**: Cache not being accessed by memory operations
**Solution**: Ensure executeStage() calls cache for loads/stores

### Issue: ROB never commits
**Cause**: commitStage() not implemented
**Solution**: Implement commit logic in CPUSimulator.java

---

## Development Notes

### Current Implementation Status
- ✅ All data structures complete
- ✅ Instruction parsing complete
- ✅ Test cases created
- ⚠️ CPUSimulator core logic incomplete (Issue/Execute/Write/Commit)
- ⚠️ UI configuration incomplete
- ⚠️ Table displays incomplete

See `IMPLEMENTATION_STATUS.md` for detailed status.

### Adding Custom Test Programs

Create a new `.txt` file with instructions:
```assembly
# Comments start with #
DADDI R1, R0, 10    # Integer immediate
ADD.D F0, F1, F2    # FP arithmetic
L.D F3, 0(R1)       # FP load
S.D F3, 8(R1)       # FP store
BEQ R1, R2, END     # Branch
END: DADDI R0, R0, 0  # Label
```

**Rules**:
- One instruction per line
- Use uppercase for opcodes
- R# for integer registers, F# for FP registers
- Labels end with `:` (e.g., `LOOP:`)
- Branch targets can be labels or offsets

---

## Default Configuration Values

When configuration UI is not available, these defaults are used:

### Instruction Latencies (cycles)
- FP Add/Sub: 2
- FP Multiply: 10  
- FP Divide: 40
- Integer ops: 1
- Memory base: 1
- Branches: 1

### Reservation Stations
- Add/Sub: 3 stations
- Mul/Div: 2 stations
- Load: 2 stations
- Store: 2 stations
- Integer: 2 stations

### ROB
- Size: 16 entries

### Cache
- Block size: 32 bytes
- Cache size: 256 bytes (8 blocks)
- Hit latency: 1 cycle
- Miss penalty: 10 cycles

---

## Next Steps for Development

1. **Implement Core Simulation Logic**
   - Complete CPUSimulator.java stages
   - Test with simple programs first

2. **Wire Up UI**
   - Connect tables to data structures
   - Implement configuration dialog

3. **Test Thoroughly**
   - Run all test cases
   - Verify hazard handling
   - Check cache behavior

4. **Polish and Document**
   - Add tooltips and help text
   - Create final report
   - Record demo video

---

## Support Resources

- **Project Documentation**: See `PROJECT_DOCUMENTATION.md`
- **Implementation Status**: See `IMPLEMENTATION_STATUS.md`
- **Course Materials**: CSEN 702 lecture slides
- **Tomasulo Paper**: IBM Journal 1967

---

## Submission Checklist

- [ ] All code compiles without errors
- [ ] All three test cases execute correctly
- [ ] UI displays all required information
- [ ] Configuration parameters work
- [ ] Cache statistics accurate
- [ ] Documentation complete
- [ ] Report written
- [ ] Demo prepared
- [ ] Code zipped with team info file
- [ ] Submitted via Google Form

---

**Good luck with your project! 🎓**
