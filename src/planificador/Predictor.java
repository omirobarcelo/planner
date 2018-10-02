/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package planificador;

/**
 *
 * @author OriolMiroBarcelo
 */
public class Predictor {
    public static final boolean TAKEN = true;
    private static final int MAX_VALUE = 3;
    private static final int MIN_VALUE = 0;
    private int predictor;

    public Predictor(int initialValue) {
        if (initialValue <= MAX_VALUE && initialValue >= MIN_VALUE)
            predictor = initialValue;
        else predictor = 0;
    }

    public void setPrediction(boolean t) {
        if (t == TAKEN) {
            if (predictor < MAX_VALUE) predictor++;
        } else {
            if (predictor > MIN_VALUE) predictor--;
        }
    }

    public boolean isTaken() {
        return (predictor > MAX_VALUE/2);
    }
}
