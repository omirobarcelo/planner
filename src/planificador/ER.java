/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

import java.util.HashSet;

/**
 *
 * @author OriolMiroBarcelo
 */
public class ER {
    private static final int ER_REG = 10;
    private int registers;
    private HashSet<Integer> er;

    public ER() {
        registers = 0;
        er = new HashSet<Integer>(4*ER_REG/3);
    }

    public boolean thereAreRegisters() {
        return registers < ER_REG;
    }

    public boolean instructionInER(int instruction) {
        return er.contains(instruction);
    }

    public boolean requestRegister(int instruction) {
        er.add(instruction);
        registers++;

        return registers == ER_REG;
    }

    public void releaseRegister(int instruction) {
        er.remove(instruction);
        if (registers > 0) registers--;
    }
}