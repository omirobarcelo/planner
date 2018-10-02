/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

/**
 *
 * @author OriolMiroBarcelo
 */
public class UF {
    private int LATENCY;
    // Identifier of the instruction assigned to this UF
    private int instruction;
    private boolean occupied;
    private int slotCycle;

    public UF(int latency) {
        this.LATENCY = latency;
        this.instruction = 0;
        this.occupied = false;
        this.slotCycle = latency;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void requestUF(int inst) {
        instruction = inst;
        slotCycle = LATENCY;
        occupied = true;
    }

    public void releaseUF() {
        instruction = 0;
        occupied = false;
    }

    // Returns true if instruction ended execution
    public boolean completeCycle() {
        if (slotCycle > 0) slotCycle--;
        return slotCycle == 0;
    }

    public int getInstruction() {
        return instruction;
    }
}
