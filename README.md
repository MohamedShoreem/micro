# MIPS CPU Simulator - Tomasulo Algorithm

A comprehensive GUI-based CPU simulator implementing Tomasulo's algorithm for dynamic instruction scheduling with support for floating-point operations, integer operations, memory operations with cache, and branch handling.

**Course**: CSEN 702: Microprocessors - Winter 2025  
**Instructor**: Assoc. Prof. Milad Ghantous  
**Institution**: Faculty of Media Engineering and Technology

---

## 📋 Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Test Cases](#test-cases)
- [Implementation Status](#implementation-status)
- [Building and Running](#building-and-running)
- [Contributors](#contributors)

---

## ✨ Features

### Supported Instructions
- **Floating-Point**: ADD.D, SUB.D, MUL.D, DIV.D
- **Integer Arithmetic**: ADD, SUB, ADDI, DADDI, DSUBI, MUL, DIV
- **Memory Operations**: LW, SW, LD, L.D, L.S, S.D, S.S
- **Branches**: BEQ, BNE (no prediction)

### Architecture Components
- **Register Files**: 32 integer registers (R0-R31), 32 FP registers (F0-F31)
- **Memory**: 4KB byte-addressable memory
- **Cache**: Configurable direct-mapped cache with block-based organization
- **Reservation Stations**: Separate stations for FP Add/Sub, Mul/Div, Load, Store, Integer
- **Reorder Buffer**: Maintains program order and enables precise exceptions
- **Register Renaming**: Eliminates WAR and WAW hazards

### Configurable Parameters
- Instruction latencies for each operation type
- Number of reservation stations of each type
- ROB size
- Cache block size, cache size, hit latency, miss penalty
- Initial register values

---

## 🚀 Quick Start

### Prerequisites
- Java 21 LTS
- Apache Maven 3.8+
- JavaFX 17+ (auto-downloaded by Maven)

### Build and Run
```powershell
# Navigate to project directory
cd "c:\Users\Mohamed Hany\Documents\GitHub\micro"

# Compile the project
mvn clean compile

# Run the simulator
mvn javafx:run
```

### Using the Simulator
1. Click **Load Program** and select a test case file
2. Configure parameters (latencies, cache, etc.)
3. Click **Step** to execute one clock cycle, or **Run** for continuous execution
4. View execution in Reservation Stations, ROB, and Register File panels
5. Click **Reset** to start over

For detailed instructions, see [QUICKSTART.md](QUICKSTART.md)

---

## 📁 Project Structure

```
micro/
├── src/main/java/com/cpu/simulator/
│   ├── Main.java                      # Application entry point
│   ├── core/
│   │   └── CPUSimulator.java         # Main simulation engine
│   ├── model/
│   │   ├── Instruction.java          # Instruction representation
│   │   ├── RegisterFile.java         # Integer + FP registers
│   │   ├── Memory.java               # Byte-addressable memory
│   │   ├── Cache.java                # Configurable cache
│   │   └── SimulatorConfig.java      # Configuration settings
│   ├── tomasulo/
│   │   ├── ReservationStation.java   # RS with type support
│   │   ├── ReorderBufferEntry.java   # ROB entry
│   │   └── RegisterStatus.java       # Register renaming table
│   ├── parser/
│   │   └── InstructionParser.java    # Instruction file parser
│   └── ui/
│       └── MainController.java       # JavaFX UI controller
├── src/main/resources/fxml/
│   └── main.fxml                      # UI layout definition
├── test_case1.txt                     # Sequential code test
├── test_case2.txt                     # Mixed dependencies test
├── test_case3.txt                     # Loop with branch test
├── pom.xml                            # Maven configuration
├── PROJECT_DOCUMENTATION.md           # Comprehensive documentation
├── IMPLEMENTATION_STATUS.md           # Current status and TODOs
├── QUICKSTART.md                      # Quick start guide
└── README.md                          # This file
```

---

## 📖 Documentation

Comprehensive documentation is available in the following files:

### [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)
Complete technical documentation covering:
- Architecture design (registers, memory, cache)
- Tomasulo algorithm implementation details
- Pipeline stages (Issue, Execute, Write Result, Commit)
- Hazard handling (RAW, WAR, WAW)
- Branch handling (no prediction, pipeline flush)
- Memory addressing and cache mapping strategies
- Bus arbitration for simultaneous results
- Code structure and class descriptions

### [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)
Current implementation status:
- ✅ Completed components (data structures, parsers, test cases)
- ⚠️ Pending components (core simulator logic, UI configuration)
- Code templates for implementing pending features
- Development tips and debugging guidance

### [QUICKSTART.md](QUICKSTART.md)
Quick start guide with:
- Prerequisites and installation
- Build and run instructions
- UI usage guide
- Test case descriptions
- Troubleshooting tips

---

## 🧪 Test Cases

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
**Demonstrates**: Independent operations, parallel execution

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
**Demonstrates**: Loop execution, branch handling, control hazards

---

## 📊 Implementation Status

### ✅ Completed (85%)
- All data structures (Instruction, RegisterFile, Memory, Cache, RS, ROB, RegisterStatus)
- Instruction parser with label support
- Configuration management system
- Test case files
- Comprehensive documentation
- Basic UI layout

### ⚠️ In Progress (15%)
- CPUSimulator core logic (Issue, Execute, Write, Commit stages)
- UI configuration dialog and table displays
- Integration testing

See [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) for detailed breakdown.

---

## 🔧 Building and Running

### Build from Source
```powershell
# Clean and compile
mvn clean compile

# Package as JAR
mvn package

# Run the application
mvn javafx:run
```

### Troubleshooting
If you encounter issues:
1. Verify Java 21: `java -version`
2. Verify Maven: `mvn -version`
3. Clean Maven cache: `mvn clean install -U`
4. See [QUICKSTART.md](QUICKSTART.md) for more help

---

## 👥 Contributors

**Team Members** (to be filled in):
- [Name] - [ID] - [Tutorial]
- [Name] - [ID] - [Tutorial]
- [Name] - [ID] - [Tutorial]
- [Name] - [ID] - [Tutorial]
- [Name] - [ID] - [Tutorial]
- [Name] - [ID] - [Tutorial]

---

## 📚 References

1. J.L. Hennessy and D.A. Patterson, "Computer Architecture: A Quantitative Approach"
2. R.M. Tomasulo, "An Efficient Algorithm for Exploiting Multiple Arithmetic Units", IBM Journal, 1967
3. Course materials, CSEN 702, Winter 2025

---

## 📝 License

This project is submitted as coursework for CSEN 702: Microprocessors, Winter 2025.  
All code is original work by the team members listed above.

---

## ⚠️ Academic Integrity Notice

Copying code from the internet or other teams will result in a zero grade for all parties involved.

---

## 📅 Project Timeline

- **Submission Deadline**: Friday, December 5, 2025
- **Evaluations Start**: Saturday, December 6, 2025
- **Submission Link**: https://forms.gle/vSUpe9mUuC2V15Uu9

---

For detailed information:
- **Architecture & Algorithm**: [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)
- **Implementation Guide**: [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)
- **Getting Started**: [QUICKSTART.md](QUICKSTART.md)

**Happy Coding! 🚀**
