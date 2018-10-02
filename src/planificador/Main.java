/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author OriolMiroBarcelo
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    enum UF_Types {
        ARI (2, 5),
        JMP (1, 4),
        MEM (2, 4);

        private final int quantity;
        private final int latency;
        UF_Types (int quantity, int latency) {
            this.quantity = quantity;
            this.latency = latency;
        }

        public int getQuantity() {
            return quantity;
        }
        public int getLatency() {
            return latency;
        }
    }

    // Instruction format
    static final int OPCODE = 0;
    static final int OPD = 1;
    static final int OPS1 = 2;
    static final int OPS2 = 3;

    // Special instructions
    static final String BEQ = "beq";
    static final String HALT = "halt";
    static final String NOP = "nop";

    // Instruction phases
    static final int READY = 0;
    static final int FETCH = 1;
    static final int DECODE = 2;
    static final int EXECUTION = 3;
    static final int EXECUTED = 4;
    static final int MEMORY = 5;
    static final int WRITE_BACK = 6;

    static final int LOOPS = 10;

    // Execution auxiliaries variables
    static boolean block;
    static int inMemory;
    static boolean haltReady;

    // File related constants and variables
    static final String fileInCode = "code.txt";
    static final String fileInCodeEval = "codeEval.txt";
    static final String fileOutCode = "planification.txt";
    static Scanner inCode;
    static Scanner inCodeEval;
    static BufferedWriter outCode;

    // Execution related variables
    static int lineCounter;
    static int cycle;
    static int blockFirstInstruction;
    static HashMap<Integer, Instruction> code;
    static ArrayList<Integer> ready;
    static ArrayList<Integer> execution;
    static boolean endCode;

    // Jump evaluation related variables
    static int numJmp;
    static String[][] jumps;
    static int totalJumps;
    static int hitJumps;

    // Units
    static Predictor binPred;
    static Rename renameRegisters;
    static ER er;
    static UF UFAri1;
    static UF UFAri2;
    static UF UFJmp1;
    static UF UFMem1;
    static UF UFMem2;

    public static void main(String[] args) {
        if (args.length == 2) {
            initialization(args[0], args[1]);
        } else {
            initialization(null, null);
        }

        boolean newBlock = false;
        while (!endCode) {
            getSimpleCodeBlock();
            getDependencies();
            getReadyList();

            do {
                fetch();
                decode();
                execute();
                memory();
                writeBack();
                newBlock = nextCycle();
            } while (!execution.isEmpty() && !newBlock);
            endCode = execution.isEmpty();
        }

        getJumpEvaluation();
        checkJumpEvaluation();

        try {
            outCode.write("\n" + cycle + " ciclos\n");
            outCode.write("\n" + hitJumps + "/" + totalJumps + " = " +
                    (100*hitJumps/totalJumps) + "% de aciertos\n");
            inCode.close();
            outCode.close();
        } catch (IOException ioex) {
            System.err.println("Exception closing files (main): " + ioex);
        }
    }

    public static void initialization(String fileCode, String fileEval) {
        try {
            // File
            if (fileCode == null && fileEval == null) {
                inCode = new Scanner(new File(fileInCode));
                inCodeEval = new Scanner(new File(fileInCodeEval));
            } else {
                inCode = new Scanner(new File(fileCode));
                inCodeEval = new Scanner(new File(fileEval));
            }
            outCode = new BufferedWriter(new FileWriter(new File(fileOutCode)));

            // Execution simulation
            block = false;
            inMemory = -1;
            haltReady = false;
            numJmp = 0;

            // Code assist
            lineCounter = 1;
            cycle = 0;
            code = new HashMap<Integer, Instruction>();
            ready = new ArrayList<Integer>();
            execution = new ArrayList<Integer>();
            endCode = false;

            // Units
            binPred = new Predictor(1); // 0b01
            renameRegisters = new Rename();
            er = new ER();
            UFAri1 = new UF(UF_Types.ARI.latency);
            UFAri2 = new UF(UF_Types.ARI.latency);
            UFJmp1 = new UF(UF_Types.JMP.latency);
            UFMem1 = new UF(UF_Types.MEM.latency);
            UFMem2 = new UF(UF_Types.MEM.latency);
        } catch (IOException ioex) {
            System.err.println(ioex.toString());
        }
    }

    public static void getSimpleCodeBlock() {
        // Boolean used to avoid only-loop label simple block
        boolean firstLine = true, firstLineLoop = false;
        String line = "";
        String[] subline;
        boolean endBlock = false;
        blockFirstInstruction = lineCounter;
        while (inCode.hasNextLine()) {
            line = inCode.nextLine();
            subline = line.split("[,\\s+]");

            Instruction inst;
            switch (subline.length) {
                case 1: {
                    if (subline[OPCODE].endsWith(":")) {
                        firstLineLoop = firstLine;
                        endBlock = true;
                    }
                    inst = new Instruction(subline[OPCODE], null, null, null);
                    if (subline[OPCODE].equals(HALT)) {
                        haltReady = true;
                        endBlock = true;
                    }
                    break;
                }
                case 3: {
                    inst = new Instruction(subline[OPCODE], subline[OPD], subline[OPS1], null);
                    break;
                }
                case 4: {
                    inst = new Instruction(subline[OPCODE], subline[OPD], subline[OPS1], subline[OPS2]);
                    if (subline[OPCODE].equals(BEQ)) {
                        numJmp++;
                        endBlock = true;
                    }
                    break;
                }
                default: {
                    inst = new Instruction(NOP, null, null, null);
                }
            }

            if (!subline[OPCODE].endsWith(":") && !subline[OPCODE].equals(NOP)
                    && !subline[OPCODE].equals(HALT)) {
                // Type
                if (inst.getOpcode().equals("add") || inst.getOpcode().equals("addi")
                    || inst.getOpcode().equals("nand")) {
                        inst.setType(UF_Types.ARI);
                } else if (inst.getOpcode().equals("beq")) {
                    inst.setType(UF_Types.JMP);
                } else if (inst.getOpcode().equals("lui") || inst.getOpcode().equals("sw")
                           || inst.getOpcode().equals("lw") || inst.getOpcode().equals("lli")
                           || inst.getOpcode().equals("movi") || inst.getOpcode().equals("jalr")) {
                    inst.setType(UF_Types.MEM);
                } else {
                    inst.setType(null);
                }

                code.put(lineCounter, inst);
            }
            lineCounter++;
            if (endBlock && !firstLineLoop) return;
            firstLine = false;
            if (firstLineLoop) {
                firstLineLoop = false;
                endBlock = false;
            }
        }
    }

    public static void getDependencies() {
        for (int i = blockFirstInstruction; i < lineCounter; i++) {
            if (code.get(i) != null) {
                Instruction iinst = code.get(i);
                for (int j = i+1; j < lineCounter; j++) {
                    if (code.get(j) != null) {
                        Instruction jinst = code.get(j);
                        if (iinst.getOpd().equals(jinst.getOps1()) ||
                            iinst.getOpd().equals(jinst.getOps2())) {
                                iinst.setLatency(iinst.getType().latency);
                                iinst.setDependency(j);
                                jinst.increaseDependent();
                        }
                        if (iinst.getOpd().equals(jinst.getOpd())) break;
                    }
                }
            }
        }
    }

    public static void getReadyList() {
        for (int i = blockFirstInstruction; i < lineCounter; i++) {
            if (code.get(i) != null)
                ready.add(i);
            ready = sortByLatency(ready);
        }
    }

    public static void fetch() {
        // Fetch with renaming on this phase
//        if (!block && !ready.isEmpty() && renameRegisters.thereAreRegisters()) {
//            execution.add(ready.remove(0));
//            renameRegisters.requestRegister();
//            code.get(execution.get(execution.size() - 1)).setState(FETCH);
//            try {
//                outCode.write(code.get(execution.get(execution.size() - 1)).toString() + "\n");
//            } catch (IOException ex) {
//                System.err.println("Exception writing instruction (fetch): " + ex);
//            }
//        }
        if (!block && !ready.isEmpty()) {
            execution.add(ready.remove(0));
            code.get(execution.get(execution.size() - 1)).setState(FETCH);
            try {
                outCode.write(code.get(execution.get(execution.size() - 1)).toString() + "\n");
            } catch (IOException ex) {
                System.err.println("Exception writing instruction (fetch): " + ex);
            }
        }
    }

    public static void decode() {
        execution = sortByLatency(execution);
        int maxLatency = getHighestLatency();
        for (Integer i : execution) {
            if (code.get(i).getState() == DECODE) {
                if (code.get(i).getDependentOf() == 0) {
                    if (code.get(i).getLatency() >= maxLatency) {
                        if (freeUF(i)) {
                            if (renameRegisters.thereAreRegisters()) {
                                if (er.instructionInER(i)) {
                                    er.releaseRegister(i);
                                    block = false;
                                }
                                code.get(i).setState(EXECUTION);
                                renameRegisters.requestRegister();
                                occupyUF(i);
                                maxLatency = getHighestLatency();
                            } else {
                                if (!er.instructionInER(i))
                                    block = er.requestRegister(i);
                            }
                        } else {
                            if (!er.instructionInER(i))
                                block = er.requestRegister(i);
                        }
                    } else {
                        if (!er.instructionInER(i))
                            block = er.requestRegister(i);
                    }
                } else {
                    if (!er.instructionInER(i))
                        block = er.requestRegister(i);
                }
            }
        }
    }

    public static void execute() {
        if (UFAri1.isOccupied() && UFAri1.completeCycle()) {
            code.get(UFAri1.getInstruction()).setState(EXECUTED);
        }
        if (UFAri2.isOccupied() && UFAri2.completeCycle()) {
            code.get(UFAri2.getInstruction()).setState(EXECUTED);
        }
        if (UFJmp1.isOccupied() && UFJmp1.completeCycle()) {
            code.get(UFJmp1.getInstruction()).setState(EXECUTED);
        }
        if (UFMem1.isOccupied() && UFMem1.completeCycle()) {
            code.get(UFMem1.getInstruction()).setState(EXECUTED);
        }
        if (UFMem2.isOccupied() && UFMem2.completeCycle()) {
            code.get(UFMem2.getInstruction()).setState(EXECUTED);
        }
    }

    public static void memory() {
        if (inMemory != -1) {
            for (Integer i : code.get(inMemory).getDependencies()) {
                code.get(i).decreaseDependent();
            }
            inMemory = -1;
        }
    }

    public static void writeBack() {
        for (Integer i : execution) {
            if (code.get(i).getState() == WRITE_BACK) {
                execution.remove(i);
                // Release Rename registers
                renameRegisters.releaseRegiser();
                break;
            }
        }
    }

    // Returns true if new code block needs to be read
    public static boolean nextCycle() {
        boolean first = true;
        cycle++;
        for (Integer i : execution) {
            // Beware null pointer exception
            if (code.get(i).getState() == FETCH)
                code.get(i).setState(DECODE);
            if (code.get(i).getState() == MEMORY)
                code.get(i).setState(WRITE_BACK);
            if (code.get(i).getState() == EXECUTED && first) {
                first = false;
                inMemory = i;
                code.get(i).setState(MEMORY);
                // Release UF
                if (UFAri1.getInstruction() == i) UFAri1.releaseUF();
                else if (UFAri2.getInstruction() == i) UFAri2.releaseUF();
                else if (UFJmp1.getInstruction() == i) UFJmp1.releaseUF();
                else if (UFMem1.getInstruction() == i) UFMem1.releaseUF();
                else if (UFMem2.getInstruction() == i) UFMem2.releaseUF();
            }
        }

        return (ready.isEmpty() && !haltReady);
    }

    public static void getJumpEvaluation() {
        int i = 0;
        jumps = new String[numJmp+1][LOOPS];
        while (inCodeEval.hasNextLine()) {
            String line = inCodeEval.nextLine();
            String[] subline = line.split("loop\\d+: ");
            jumps[i] = subline[1].split(",");
            i++;
        }
        for (i = 0; i < LOOPS - 1; i++) {
            jumps[numJmp][i] = "T";
        }
        jumps[numJmp][LOOPS-1] = "NT";
    }

    public static void checkJumpEvaluation() {
        totalJumps = (numJmp+1)*LOOPS;
        hitJumps = 0;

        for (int i = 0; i < LOOPS; i++) {
            for (int j = 0; j < jumps.length; j++) {
                if (jumps[j][i].equals("T")) {
                    if (binPred.isTaken()) 
                        hitJumps++;
                    binPred.setPrediction(Predictor.TAKEN);
                } else if (jumps[j][i].equals("NT")) {
                    if (!binPred.isTaken()) 
                        hitJumps++;
                    binPred.setPrediction(!Predictor.TAKEN);
                }
            }
        }
    }

    private static ArrayList<Integer> sortByLatency(ArrayList<Integer> insts) {
        Integer aux;
        for (int i = 0; i < insts.size() - 1; i++)
            for(int j = 0; j < insts.size() - i - 1; j++)
                if (code.get(insts.get(j+1)).getLatency() >
                    code.get(insts.get(j)).getLatency()) {
                        aux = insts.get(j+1);
                        insts.remove(j+1);
                        insts.add(j+1, insts.get(j));
                        insts.remove(j);
                        insts.add(j, aux);
                }

        return insts;
    }

    private static boolean freeUF(int inst) {
        switch (code.get(inst).getType()) {
            case ARI: {
                if (!UFAri1.isOccupied() || !UFAri2.isOccupied()) return true;
                break;
            }
            case JMP: {
                if (!UFJmp1.isOccupied()) return true;
                break;
            }
            case MEM: {
                if (!UFMem1.isOccupied() || !UFMem2.isOccupied()) return true;
                break;
            }
        }

        return false;
    }

    private static void occupyUF(int inst) {
        switch (code.get(inst).getType()) {
            case ARI: {
                if (!UFAri1.isOccupied()) UFAri1.requestUF(inst);
                else if (!UFAri2.isOccupied()) UFAri2.requestUF(inst);
                break;
            }
            case JMP: {
                if (!UFJmp1.isOccupied()) UFJmp1.requestUF(inst);
                break;
            }
            case MEM: {
                if (!UFMem1.isOccupied()) UFMem1.requestUF(inst);
                else if (!UFMem2.isOccupied()) UFMem2.requestUF(inst);
                break;
            }
        }
    }

    private static int getHighestLatency() {
        int maxLatency = 0;

        for (Integer i : execution) {
            if (code.get(i).getState() == FETCH || code.get(i).getState() == DECODE) {
                maxLatency = code.get(i).getLatency();
                break;
            }
        }

        return maxLatency;
    }
}
