import java.io.*;
import java.util.*;

public class MulticyclePipeline {
	static Scanner input = new Scanner(System.in);
	
	static boolean DEBUG = false; // Make sure this value is FALSE before executing the simulator
	
	enum stage { // Contains all possible statuses of an instruction in the pipeline
		idle, // Stage in which the instruction is waiting to start pipelining
		
		IF, ID, // Instruction fetch and decode, 1 stage long each for all instructions
		
		EX, // Default instruction execution, 1 stage long for all integer or branch instructions and floating point (FP) memory instructions
		
		A1, A2, // FP adder and subtracter execution, 2 stages long
		
		M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, // Multiplier execution, 10 stages long
		
		D1, D2, D3, D4, D5, D6, D7, D8, D9, D10,
		D11, D12, D13, D14, D15, D16, D17, D18, D19, D20,
		D21, D22, D23, D24, D25, D26, D27, D28, D29, D30,
		D31, D32, D33, D34, D35, D36, D37, D38, D39, D40, // Divider execution, 40 stages long
		
		MEM, MEM2, MEM3, // Memory storage, 3 stages long for L.D instructions (due to causing a miss from L1 cache) and 1 stage long for all other instructions
		
		WB, // Write back, 1 stage long for all instructions
		
		complete // Stage in which the cycle has finished pipelining; used to end the pipeline do-while loop for each instruction
	}
	
	enum operator {
		LD, SD, LI, LW, SW, // Memory operations - load FP, store FP, load immediate int, load word (int), store word (int)
		
		ADDI, ADD, SUB, // Integer arithmetic - add immediate, add int, subtract int
		
		ADD_D, SUB_D, MUL_D, DIV_D, // Floating point arithmetic - add doubleword, subtract doubleword, multiply, divide
		
		BEQ, BNE, J // Branch operations - branch if equal, branch if not equal, unconditional jump
	}
	
	static int[] memory = {45,12,0,92,10,135,254,127,18,4,55,8,2,98,13,5,233,158,167}; // Default memory values in addresses 0-18 given by project instructions
	
	// Register arrays
	static Integer[] R = new Integer[32]; // Integer registers $0 through $31
	static Float[] FP = new Float[32]; // Floating point registers F0 through F31
	
	static Vector<String> instructions = new Vector<String>(); // The RAW TEXT for each instruction in order; initialized by loadInstructions()
	
	static Vector<String> ops = new Vector<String>(); // The OPERANDS for each instruction in order; initialized by sortInstructions()
	
	static Vector<operator> opTypes = new Vector<operator>(); // The OPERATORS for each instruction in order; initialized by sortInstructions()
	
	static Vector<stage> currInstruction = new Vector<stage>(); // The stages at each cycle for the instruction that is CURRENTLY being pipelined
	
	static Vector<stage> lastInstruction = new Vector<stage>(); // The full pipeline of the PREVIOUS instruction; remians empty when pipelining the first one
	
	static boolean stall = false; // Determines whether a portion of the instructions are pipelining
	
	public static void main(String[] args) {
		loadInstructions(); // Load and store raw text in instructions Vector
		sortInstructions(); // Sort each String in instructions Vector with delimiters and store them in ops Vector
		
		if (DEBUG) {
			for (int i = 0; i < ops.size(); i++) {
				System.out.println(opTypes.get(i)+"; "+ops.get(i));
			}
			System.out.println();
		}
		
		// First instruction is separate because it starts at IF instead of idle
		stage currStage = stage.IF;
		stage lastInstStage;
		System.out.printf("%-25s", instructions.get(0));
		int currCycle = 0;
		currInstruction = new Vector<stage>();
		do {
			
			if (currCycle >= lastInstruction.size()) {
				lastInstStage = stage.complete;
			} else {
				lastInstStage = lastInstruction.get(currCycle);
			}
			displayStage(currStage);
			currInstruction.add(currStage);
			if (DEBUG)
				System.out.print("Cycle "+currCycle+" - current stage: "+currStage+"; last stage at this cycle: "+lastInstStage);
			currStage = updateStage(currStage, lastInstStage, opTypes.get(0), currCycle, 0);
			if (DEBUG)
				System.out.println("; updated cycle: "+currStage+"; stall="+stall);
			currCycle++;
			
		} while (currStage != stage.complete);
		System.out.println();
		
		lastInstruction = new Vector<stage>();
		for (int j = 0; j < currInstruction.size(); j++) {
			lastInstruction.add(currInstruction.get(j));
		}
		
		if (DEBUG) {
			System.out.print("lastInstruction: ");
			for (int k = 0; k < lastInstruction.size(); k++) {
				System.out.printf("%-10s", lastInstruction.get(k).toString());
			}
			System.out.println();
			System.out.println("Next i value: 1\n");
		}
		
		int i = 1;
		while (i < instructions.size()) {
			System.out.printf("%-25s", instructions.get(i));
			currStage = stage.idle;
			currCycle = 0;
			currInstruction = new Vector<stage>();
			do {
				
				if (currCycle >= lastInstruction.size())
					lastInstStage = stage.complete;
				else
					lastInstStage = lastInstruction.get(currCycle);
				
				// Just using different displays for debugging vs. running the program
				if (DEBUG) {
					System.out.print("Cycle "+currCycle+" - current stage: "+currStage+"; last stage at this cycle: "+lastInstStage);
				} else {
					displayStage(currStage);
				}
				
				currInstruction.add(currStage);
				
				currStage = updateStage(currStage, lastInstStage, opTypes.get(i), currCycle, i);
				if (DEBUG)
					System.out.println("; updated cycle: "+currStage+"; stall="+stall);
				currCycle++;
					
			} while (currStage != stage.complete);
			System.out.println();
			
			// lastInstruction becomes a copy of currInstruction after each instruction has been pipelined
			lastInstruction = new Vector<stage>();
			for (int j = 0; j < currInstruction.size(); j++) {
				lastInstruction.add(currInstruction.get(j));
			}
			
			if (taken(opTypes.get(i), ops.get(i))) {
				i = branchIndex(i);
			} else {
				i++;
			}
			
			// Verify that lastInstruction has updated properly when debugging
			if (DEBUG) {
				System.out.print("lastInstruction: ");
				for (int k = 0; k < lastInstruction.size(); k++) {
					System.out.printf("%-10s", lastInstruction.get(k).toString());
				}
				System.out.println();
				System.out.println("Next i value: "+(i+1)+"\n");
			}
		}
		
		System.out.println();
		printResults();
	}
	
	// loadInstructions()
	// Given user input for the name of a text file, initializes the instructions vector by loading each line of that text file using I/O.
	static void loadInstructions() {
		// User inputs the name of the file with instructions
		System.out.print("Enter the name of your text file (INCLUDE THE \".txt\" SUFFIX) ");
		String filename = input.nextLine();
		Scanner infile = null;
		
		// Input validation, close the program if not valid
		try { infile = new Scanner(new FileReader(filename)); }
		catch (FileNotFoundException e) {
			System.out.println("File not found. Try running the simulator again.");
			System.exit(0); // Exit the program
		}
		
		// Load each line in the file and convert it to an MIPS instruction
		while (infile.hasNextLine()) {
			instructions.add(infile.nextLine());
		}
		System.out.println("Instructions loaded successfully.\n");
	}
	
	
	// sortInstructions()
	// Initializes the ops and opTypes vectors based on the layout of each loaded MIPS instruction.
	static void sortInstructions() {
		for (int i = 0; i < instructions.size(); i++) {
			String line = instructions.get(i);
			if (line.startsWith("Loop: ")) {
				line = line.substring(6);
			}
			StringTokenizer delimiter = new StringTokenizer(line);
			// Load instruction w/ register
			if (line.startsWith("L.D ") || line.startsWith("LW ")) {
				String op = delimiter.nextToken(" ");
				String dest = delimiter.nextToken(", ");
				String s = delimiter.nextToken("(").substring(2);
				String t = delimiter.nextToken(")").substring(1);
				ops.add(dest+"|"+s+"|"+t);
				
				if (line.startsWith("L.D "))
					opTypes.add(operator.LD);
				else if (line.startsWith("LW "))
					opTypes.add(operator.LW);
			}
			// Store instruction
			if (line.startsWith("S.D ") || line.startsWith("SW ")) {
				String op = delimiter.nextToken(" ");
				String s = delimiter.nextToken(", ");
				String offset = delimiter.nextToken("(").substring(2);
				String adder = delimiter.nextToken(")").substring(1);
				ops.add(s+"|"+offset+"|"+adder);
				
				if (line.startsWith("S.D "))
					opTypes.add(operator.SD);
				else if (line.startsWith("SW "))
					opTypes.add(operator.SW);
			}
			// Load instruction w/ immediate
			if (line.startsWith("LI")) {
				String op = delimiter.nextToken(" ");
				String dest = delimiter.nextToken(", ");
				int imm = Integer.parseInt(delimiter.nextToken());
				ops.add(dest+"|"+imm);
				opTypes.add(operator.LI);
			}
			// ALU instructions
			if (line.startsWith("ADD ") || line.startsWith("ADD.D ") || line.startsWith("SUB ") || line.startsWith("SUB.D ")
					|| line.startsWith("MUL.D ") || line.startsWith("DIV.D ") || line.startsWith("ADDI ")) {
				String op = delimiter.nextToken(" ");
				String dest = delimiter.nextToken(", ");
				String s = delimiter.nextToken(", ");
				String t = delimiter.nextToken();
				ops.add(dest+"|"+s+"|"+t);
				
				if (line.startsWith("ADD "))
					opTypes.add(operator.ADD);
				else if (line.startsWith("ADD.D "))
					opTypes.add(operator.ADD_D);
				else if (line.startsWith("SUB "))
					opTypes.add(operator.SUB);
				else if (line.startsWith("SUB.D "))
					opTypes.add(operator.SUB_D);
				else if (line.startsWith("MUL.D "))
					opTypes.add(operator.MUL_D);
				else if (line.startsWith("DIV.D "))
					opTypes.add(operator.DIV_D);
				else opTypes.add(operator.ADDI);
			}
			
			if (line.startsWith("BEQ ") || line.startsWith("BNE ")) {
				String op = delimiter.nextToken(" ");
				String s = delimiter.nextToken(", ");
				String t = delimiter.nextToken(", ");
				String offset = delimiter.nextToken();
				ops.add(s+"|"+t+"|"+offset);
				
				if (line.startsWith("BEQ "))
					opTypes.add(operator.BEQ);
				else if (line.startsWith("BNE "))
					opTypes.add(operator.BNE);
			}
			// Unconditional branch
			if (line.startsWith("J")) {
				String op = delimiter.nextToken(" ");
				ops.add(delimiter.nextToken());
				opTypes.add(operator.J);
			}
		}
	}
	
	// displayStage()
	// Given the stage of an instruction at the current cycle, displays a certain string based on the status of a pipeline.
	// Displays nothing if it is idle, "stall" if the current cycle is found to be stalling, or the name of the current stage otherwise.
	static void displayStage(stage s) {
		if (s == stage.idle || s == stage.complete)
			System.out.printf("%-10s", "");
		else if (stall)
			System.out.printf("%-10s", "stall");
		else if (s == stage.MEM || s == stage.MEM2 || s == stage.MEM3) // MEM2 and MEM3 are simply extra parts of the MEM stage for L.D instructions
			System.out.printf("%-10s", "MEM");
		else
			System.out.printf("%-10s", s.toString());
	}
	
	// updateStage()
	// Given the current instruction's stage; location and operator; the current cycle number; and the LAST instruction's stage at that operator,
	// checks for a stall and returns the current instruction's NEXT stage in the pipeline.
	
	static stage updateStage(stage s, stage sLastInst, operator o, int currCycle, int i) {
		stage nextStageLastInst = null;
		if (i > 0) {
			if (currCycle < lastInstruction.size()-1) {
				nextStageLastInst = lastInstruction.get(currCycle+1);
			}
		}
		stage newStage = null;
		switch(s) {
		case idle: // All instructions remain unused until IF is available
			if (sLastInst == stage.IF && nextStageLastInst != sLastInst)
				newStage = stage.IF;
			else
				newStage = stage.idle;
			break;
		
		case IF: // Instruction fetching stalls until ID is available
			if (nextStageLastInst == sLastInst) {
				stall = true;
				newStage = stage.IF;
			} else {
				stall = false;
				newStage = stage.ID;
			}
			break;
			
		case ID: // Instruction decoding stalls until the correct TYPE of execution is available (Integer, Mult, Add, or Div)
			if (nextStageLastInst == sLastInst) {
				stall = true;
				newStage = stage.ID;
			} else {
				stall = false;
				if (o == operator.ADD_D || o == operator.SUB_D) {
					if (sLastInst == stage.A1) { // Pipeline cannot execute 2 ADD_D instructions at once
						stall = true;
						newStage = stage.ID;
					} else
						newStage = stage.A1;
				} else if (o == operator.MUL_D) {
					if (sLastInst.ordinal() >= stage.M1.ordinal() && sLastInst.ordinal() < stage.M10.ordinal()) { // Pipeline cannot execute 2 MUL_D instructions at once
						stall = true;
						newStage = stage.ID;
					} else
						newStage = stage.M1;
				} else if (o == operator.DIV_D) {
					if (sLastInst.ordinal() >= stage.D1.ordinal() && sLastInst.ordinal() < stage.D40.ordinal()) { // Pipeline cannot execute 2 DIV_D instructions at once
						stall = true;
						newStage = stage.ID;
					} else
						newStage = stage.M1;
				} else {
					newStage = stage.EX;
				}
			}
			break;
			
		case EX: // For load/store, branch and int arithmetic operations, execution stalls until MEM is available
			if (o == operator.BEQ || o == operator.BNE || o == operator.J) {
				newStage = stage.complete;
			} else if ((sLastInst.ordinal() < stage.MEM.ordinal() && sLastInst.ordinal() < stage.MEM.ordinal()) || nextStageLastInst == stage.MEM || nextStageLastInst == sLastInst
				|| (sLastInst == stage.MEM && nextStageLastInst == stage.MEM2) || (sLastInst == stage.MEM2 && nextStageLastInst == stage.MEM3)) {
				
				stall = true;
				newStage = stage.EX;
				
			} else {
				stall = false;
				newStage = stage.MEM;
			}
			break;
		
		// Memory access does NOT stall under any circumstances
			
		case MEM: // Move to MEM2 for L.D operations, access memory and move to WB for all other operations
			stall = false;
			if (o == operator.LD)
				newStage = stage.MEM2;
			else {
				storeMem(o, ops.get(i));
				newStage = stage.WB;
			}
			break;
		
		case MEM2: // L1 cache miss, move to MEM3
			newStage = stage.MEM3;
			break;
			
		case MEM3: // After an L1 cache miss, access memory and move to WB
			storeMem(operator.LD, ops.get(i));
			newStage = stage.WB;
			break;
			
		// For all FP arithmetic operations, execution stalls on the LAST cycle until MEM is available
		
		case A1:
			newStage = stage.A2;
			break;
		
		case A2:
			if ((sLastInst.ordinal() < stage.MEM.ordinal() && sLastInst.ordinal() < stage.MEM.ordinal()) || nextStageLastInst == stage.MEM || nextStageLastInst == sLastInst
			|| (sLastInst == stage.MEM && nextStageLastInst == stage.MEM2) || (sLastInst == stage.MEM2 && nextStageLastInst == stage.MEM3)) {
				
				stall = true;
				newStage = stage.A2;
			
			} else {
				stall = false;
				newStage = stage.MEM;
			}
			break;
		
		case M1:
			newStage = stage.M2;
			break;
			
		case M2:
			newStage = stage.M3;
			break;
			
		case M3:
			newStage = stage.M4;
			break;
			
		case M4:
			newStage = stage.M5;
			break;
			
		case M5:
			newStage = stage.M6;
			break;
			
		case M6:
			newStage = stage.M7;
			break;
			
		case M7:
			newStage = stage.M8;
			break;
			
		case M8:
			newStage = stage.M9;
			break;
			
		case M9:
			newStage = stage.M10;
			break;
			
		case M10:
			if ((sLastInst.ordinal() < stage.MEM.ordinal() && sLastInst.ordinal() < stage.MEM.ordinal()) || nextStageLastInst == stage.MEM || nextStageLastInst == sLastInst
			|| (sLastInst == stage.MEM && nextStageLastInst == stage.MEM2) || (sLastInst == stage.MEM2 && nextStageLastInst == stage.MEM3)) {
				
				stall = true;
				newStage = stage.M10;
				
			} else {
				stall = false;
				newStage = stage.MEM;
			}
			break;
			
		case D1:
			newStage = stage.D2;
			break;
			
		case D2:
			newStage = stage.D3;
			break;
			
		case D3:
			newStage = stage.D4;
			break;
			
		case D4:
			newStage = stage.D5;
			break;
			
		case D5:
			newStage = stage.D6;
			break;
			
		case D6:
			newStage = stage.D7;
			break;
			
		case D7:
			newStage = stage.D8;
			break;
			
		case D8:
			newStage = stage.D9;
			break;
			
		case D9:
			newStage = stage.D10;
			break;
			
		case D10:
			newStage = stage.D11;
			break;
			
		case D11:
			newStage = stage.D12;
			break;
			
		case D12:
			newStage = stage.D13;
			break;
			
		case D13:
			newStage = stage.D14;
			break;
			
		case D14:
			newStage = stage.D15;
			break;
			
		case D15:
			newStage = stage.D16;
			break;
			
		case D16:
			newStage = stage.D17;
			break;
			
		case D17:
			newStage = stage.D18;
			break;
			
		case D18:
			newStage = stage.D19;
			break;
			
		case D19:
			newStage = stage.D20;
			break;
			
		case D20:
			newStage = stage.D21;
			break;
			
		case D21:
			newStage = stage.D22;
			break;
			
		case D22:
			newStage = stage.D23;
			break;
			
		case D23:
			newStage = stage.D24;
			break;
			
		case D24:
			newStage = stage.D25;
			break;
			
		case D25:
			newStage = stage.D26;
			break;
			
		case D26:
			newStage = stage.D27;
			break;
			
		case D27:
			newStage = stage.D28;
			break;
			
		case D28:
			newStage = stage.D29;
			break;
			
		case D29:
			newStage = stage.D30;
			break;
			
		case D30:
			newStage = stage.D31;
			break;
			
		case D31:
			newStage = stage.D32;
			break;
			
		case D32:
			newStage = stage.D33;
			break;
			
		case D33:
			newStage = stage.D34;
			break;
			
		case D34:
			newStage = stage.D35;
			break;
			
		case D35:
			newStage = stage.D36;
			break;
			
		case D36:
			newStage = stage.D37;
			break;
			
		case D37:
			newStage = stage.D38;
			break;
			
		case D38:
			newStage = stage.D39;
			break;
			
		case D39:
			newStage = stage.D40;
			break;
			
		case D40:
			if ((sLastInst.ordinal() < stage.MEM.ordinal() && sLastInst.ordinal() < stage.MEM.ordinal()) || nextStageLastInst == stage.MEM || nextStageLastInst == sLastInst
			|| (sLastInst == stage.MEM && nextStageLastInst == stage.MEM2) || (sLastInst == stage.MEM2 && nextStageLastInst == stage.MEM3)) {
				
				stall = true;
				newStage = stage.D40;
				
			} else {
				stall = false;
				newStage = stage.MEM;
			}
			break;
			
		default: // Writing back stops all stalling and completes the pipeline
			stall = false;
			newStage = stage.complete;
			break;
		}
		return newStage;
	}
	
	// storeMem()
	// Given the operator and operands of the current instruction, uses the operands to store a value in the destination.
	
	static void storeMem(operator o, String operands) {
		int d;
		Integer Is;
		Integer It;
		Float Fs;
		Float Ft;
		String addr;
		
		StringTokenizer valGetter = new StringTokenizer(operands, "|");
		Integer imm; // Immediate integer containing a memory address for LI and ADDI
		
		switch (o) {
		
		case LD:
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = Integer.parseInt(valGetter.nextToken());
			
			addr = valGetter.nextToken();
			
			if (addr.startsWith("$")) { // If addr is a register, Fd = offset + $addr
				It = memory[R[Integer.parseInt(addr.substring(1))]];
				FP[d] = (float)Is + (float)It;
			} else { // If addr is a memory address, Fd = memory[offset + addr]
				It = Integer.parseInt(addr);
				FP[d] = (float)memory[Is+It];
			}
			
			if (DEBUG) {
				System.out.print("\nMEM3 called: d = "+d+"; offset = "+Is+"; addr = "+It+"; value stored = "+FP[d]);
			}
			
			break;
			
		case SD: // For S.D, memory[offset + $addr] = Fd
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = Integer.parseInt(valGetter.nextToken());
			
			addr = valGetter.nextToken();
			
			It = R[Integer.parseInt(addr.substring(1))];
			
			memory[Is+It] = Math.round(FP[d]);
			
			break;
			
		case LI: // For LI, $d = imm
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			imm = Integer.parseInt(valGetter.nextToken());
			R[d] = imm;
			break;
			
		case LW:
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = Integer.parseInt(valGetter.nextToken());
			
			addr = valGetter.nextToken();
			
			if (addr.startsWith("$")) { // If addr is a register, $d = offset + $addr
				It = memory[R[Integer.parseInt(addr.substring(1))]];
				R[d] = Is + It;
			} else { // If addr is a memory address, $d = memory[offset + addr]
				It = Integer.parseInt(addr);
				R[d] = memory[Is+It];
			}
			
			break;
		
		case SW: // For SW, memory[offset + $addr] = $d
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = Integer.parseInt(valGetter.nextToken());
			
			addr = valGetter.nextToken();
			
			It = R[Integer.parseInt(addr.substring(1))];
			
			memory[Is + It] = R[d];
			
			break;
		
		case ADD: // For ADD, $d = $s + $t
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			It = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			R[d] = Is + It;
			break;
		
		case ADDI: // For ADDI, $d = $s + imm
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			imm = Integer.parseInt(valGetter.nextToken());
			
			R[d] = Is + imm;
			
			break;
		
		case ADD_D: // For ADD.D, Fd = Fs + Ft
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Fs = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			Ft = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			FP[d] = Fs + Ft;
			break;
		
		case SUB: // For SUB, $d = $s - $t
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Is = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			It = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			R[d] = Is - It;
			break;
		
		case SUB_D: // For SUB.D, Fd = Fs - Ft
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Fs = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			Ft = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			FP[d] = Fs - Ft;
			break;
		
		case MUL_D: // For MUL.D, Fd = Fs * Ft
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Fs = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			Ft = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			FP[d] = Fs * Ft;
			break;
			
		case DIV_D: // For DIV.D, Fd = Fs / Ft
			d = Integer.parseInt(valGetter.nextToken().substring(1));
			
			Fs = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			Ft = FP[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			FP[d] = Fs / Ft;
			break;
			
		default:
			System.out.println("/nError: Branch instructions should not store any memory. Exiting program.");
			System.exit(0);
			break;
		}
	}
	
	// taken()
	// Given the operator and operands of the current instruction, determines whether a branch is being called when that instruction's pipeline is complete.
	static boolean taken(operator o, String operands) {
		
		int Is;
		int It;
		
		StringTokenizer valGetter = new StringTokenizer(operands, "|");
		
		switch (o) {
		
		case J: // // For J (unconditional jump), branch is ALWAYS taken
			return true;
			
		case BEQ: // For BEQ, if registers $s and $t point to the same value, the branch is taken
			Is = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			It = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			if (Is == It) {
				return true;
			}
			break;
			
		case BNE: // For BNE, if registers $s and $t point to the different values, the branch is taken
			Is = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			It = R[Integer.parseInt(valGetter.nextToken().substring(1))];
			
			if (Is != It) {
				return true;
			}
			break;
		
		default:
			break;
		}
		
		return false; // For different operators or an unfulfilled BEQ/BNE condition, move onto the next instruction
	}
	
	// branchIndex()
	//
	static int branchIndex(int jumpCall) {
		for (int i = jumpCall; i >= 0; i--) {
			if (instructions.get(i).startsWith("Loop: ")) {
				return i;
			}
		}
		return 0;
	}
	
	// printResults()
	// Displays the final values in registers and 
	static void printResults() {
		System.out.println("Final values in float registers:");
		for (int i = 0; i < FP.length; i++) {
			System.out.println("F"+i+" -> "+FP[i]);
		}
		System.out.println("\nFinal values in int registers:");
		for (int i = 0; i < R.length; i++) {
			System.out.println("$"+i+" -> "+R[i]);
		}
		System.out.println("\nFinal values in memory:");
		for (int i = 0; i < memory.length; i++) {
			System.out.println("Address "+i+" -> "+memory[i]);
		}
	}
}
