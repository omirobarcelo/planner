/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

import java.util.ArrayList;

/**
 *
 * @author OriolMiroBarcelo
 */
public class Instruction {
    private String opcode;
    private String opd;
    private String ops1;
    private String ops2;
    private Main.UF_Types type;
    private int state;
    private int latency;
    // Of hoy many instructions is dependent this instruction
    private int dependentOf;
    // List of the instructions that depends of this one
    private ArrayList<Integer> dependencies;

    public Instruction(String opcode, String opd, String ops1, String ops2) {
        this.opcode = opcode;
        this.opd = opd;
        this.ops1 = ops1;
        this.ops2 = ops2;
        this.state = Main.READY;
        this.latency = 0;
        this.dependentOf = 0;
        this.dependencies = new ArrayList<Integer>();
    }

    @Override
    public String toString() {
        String s = "";

        if (opd == null) {
            s = opcode;
        } else if (ops2 == null) {
            s = opcode + " " + opd + "," + ops1;
        } else {
            s = opcode + " " + opd + "," + ops1 + "," + ops2;
        }

        return s;
    }

    public String toCompleteString() {
        String s = "";

        s = this.toString();
        s += "\t" + type + " l: " + latency + " dOf: " + dependentOf + " dep: ";
        for (Integer i : dependencies) {
            s += i + " ";
        }

        return s;
    }

    public String getOpcode() {
        return opcode;
    }

    public String getOpd() {
        return opd;
    }

    public String getOps1() {
        return ops1;
    }

    public String getOps2() {
        return ops2;
    }

    public void setType(Main.UF_Types type) {
        this.type = type;
    }

    public Main.UF_Types getType() {
        return type;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

    public int getLatency() {
        return latency;
    }

    public void increaseDependent() {
        this.dependentOf++;
    }

    public void decreaseDependent() {
        if (this.dependentOf > 0) this.dependentOf--;
    }

    public int getDependentOf() {
        return dependentOf;
    }

    public void setDependency(int dependency) {
        this.dependencies.add(dependency);
    }

    public ArrayList<Integer> getDependencies() {
        return dependencies;
    }
}
