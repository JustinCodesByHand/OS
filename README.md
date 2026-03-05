# Java OS Simulator

A simulated operating system kernel built in Java that implements core OS concepts including process scheduling, virtual memory with paging and swapping, a virtual file system, and inter-process communication.

---

## Features

| Subsystem | What's Implemented |
|---|---|
| **Process Management** | PCB, multi-level priority queues, quantum-based preemption, priority demotion |
| **Scheduler** | Three-tier round-robin (realtime → interactive → background) with probabilistic queue selection |
| **Virtual Memory** | 1 MB physical memory, 2-entry TLB, demand paging, page eviction, swap file |
| **Virtual File System** | `Device` interface, `RandomDevice`, `FakeFileSystem` (via `RandomAccessFile`) |
| **IPC** | Kernel message passing (`SendMessage` / `WaitForMessage`) |
| **System Calls** | `CreateProcess`, `Exit`, `Sleep`, `GetPID`, `Open/Close/Read/Write/Seek`, `AllocateMemory`, `FreeMemory`, `GetMapping` |

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                  Userland Processes                  │
│  (HelloWorld, Piggy, Ping/Pong, TestPaging, …)       │
│              extend UserlandProcess                  │
└──────────────────────┬───────────────────────────────┘
                       │  OS.* system call API
┌──────────────────────▼───────────────────────────────┐
│                      OS (API layer)                  │
│  Marshals call type + parameters, signals Kernel     │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│                   Kernel (extends Process)           │
│  Dispatches system calls, manages virtual memory,   │
│  owns the VFS and Scheduler                         │
│                                                      │
│  ┌─────────────┐   ┌──────────────────────────────┐  │
│  │  Scheduler  │   │  Virtual Memory Manager      │  │
│  │  realtime   │   │  freeListofPages[1024]       │  │
│  │  interactive│   │  swap file via VFS           │  │
│  │  background │   │  PCB page tables             │  │
│  └─────────────┘   └──────────────────────────────┘  │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │  VFS                                         │    │
│  │  RandomDevice          FakeFileSystem        │    │
│  └──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│                  Hardware (static)                   │
│  byte[1MB] physical memory   int[2][2] TLB           │
│  Read(virtualAddr)  Write(virtualAddr, value)        │
│  UpdateTLB(vPage, pPage)   ClearTLB()                │
└──────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

### Cooperative + Preemptive Scheduling
Each process runs on its own Java thread and is blocked/unblocked via a `Semaphore`. A `Timer` fires every 250 ms to request a quantum expiry; processes check via `cooperate()`. Repeated preemptions demote a process to the next lower priority level (realtime → interactive → background).

### Virtual Memory & TLB
Physical memory is modeled as a `byte[1,048,576]` array split into 1,024 pages of 1 KB each. Every process owns a 100-entry virtual page table (`VirtualToPhysicalMapping[]`). On a TLB miss, `OS.GetMapping()` resolves the mapping; if the page is on disk it is swapped in, evicting a random victim page if physical memory is full.

### Virtual File System
The `Device` interface (`Open`, `Close`, `Read`, `Write`, `Seek`) is implemented by `RandomDevice` and `FakeFileSystem`. `VFS` acts as a multiplexer, routing calls to the correct underlying device based on the device-spec prefix (`"random"` or `"file …"`).

### Inter-Process Communication
`KernelMessage` carries a sender PID, target PID, an integer `what` field, and an optional byte payload. The kernel delivers messages to per-process queues; `WaitForMessage` blocks the caller until a message arrives.

---

## Project Structure

```
OS/
├── Main.java                  # Entry point – boots the OS
├── OS.java                    # System call API (userland-facing)
├── Kernel.java                # Kernel – dispatches system calls, owns VM & VFS
├── Scheduler.java             # Multi-level priority scheduler
├── PCB.java                   # Process Control Block
├── Process.java               # Abstract base – thread + semaphore lifecycle
├── UserlandProcess.java       # Userland base class (wraps PCB reference)
├── Hardware.java              # Physical memory array and TLB simulation
├── VirtualToPhysicalMapping.java  # Virtual → physical / disk page mapping
├── VFS.java                   # Virtual File System multiplexer
├── Device.java                # Device interface
├── RandomDevice.java          # /dev/random-style device
├── FakeFileSystem.java        # File-backed random-access device
├── KernelMessage.java         # IPC message struct
│
├── Init.java                  # Root process – spawns the initial process tree
├── IdleProcess.java           # Background idle loop
├── HelloWorld.java            # Simple demo process (prints "Hello World")
├── GoodbyeWorld.java          # Simple demo process (prints "Goodbye World")
├── SleeperProcess.java        # Demonstrates OS.Sleep()
├── Piggy.java                 # Memory-intensive process that triggers swapping
├── Ping.java                  # IPC demo – sends incrementing messages to Pong
├── Pong.java                  # IPC demo – echoes messages back to Ping
│
├── TestPaging.java            # Allocate / write / read / free memory tests
├── TestPagingIsolation.java   # Verifies two processes cannot read each other's pages
├── SimplePagingTest.java      # Minimal single-page allocation sanity check
├── TestSegFault.java          # Confirms illegal memory access kills the process
├── TestRandomDevice.java      # Reads random bytes via VFS
├── TestMultipleDevices.java   # Opens multiple VFS devices concurrently
└── TestFileDevice.java        # Reads and writes files via VFS
```

---

## How to Build and Run

### Prerequisites
- Java 11 or later

### Compile (command line)
```bash
# From the project root
javac *.java
```

### Run
```bash
java Main
```

The default `Main` boots the OS with `Init`, which creates 15 memory-heavy `Piggy` processes to demonstrate demand paging and swap.

### Run a specific test scenario
Swap out the process passed to `OS.Startup()` in `Main.java`:

```java
// IPC demo
OS.Startup(new Init());      // default – virtual memory stress test

// Swap these into Main.java to run other scenarios:
// OS.Startup(new TestPaging());         // paging unit tests
// OS.Startup(new TestPagingIsolation());// memory isolation test
// OS.Startup(new TestSegFault());       // segfault handling
// OS.Startup(new TestRandomDevice());   // random device I/O
// OS.Startup(new TestFileDevice());     // file device I/O
```

### Run with IntelliJ IDEA
Open the project root as an IntelliJ project and run the `Main` class directly.

---

## Concepts Demonstrated

- **Thread synchronization** – semaphores, `synchronized` blocks, `wait`/`notifyAll`
- **Demand paging** – pages allocated lazily; TLB miss triggers `GetMapping` system call
- **Page swapping** – random victim page selection evicts a physical page from another process when memory is full
- **Priority scheduling** – weighted random selection across three queues; demotion on repeated timeouts
- **Device abstraction** – uniform `Device` interface hides whether I/O goes to a random number generator or a real file
- **IPC** – message-passing without shared memory; sender never blocks
