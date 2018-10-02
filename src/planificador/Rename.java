/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

/**
 *
 * @author OriolMiroBarcelo
 */
public class Rename {
    private static final int RENAME_REG = 16;
    private int registers;

    public Rename() {
        registers = 0;
    }

    public boolean thereAreRegisters() {
        return registers < RENAME_REG;
    }

    public void requestRegister() {
        registers++;
    }

    public void releaseRegiser() {
        if (registers > 0) registers--;
    }
}
