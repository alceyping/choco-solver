/**
 * Copyright (c) 2015, Ecole des Mines de Nantes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the <organization>.
 * 4. Neither the name of the <organization> nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chocosolver.solver.constraints.set;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.ESat;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Alexandre LEBRUN
 */
public class SumOfElementsTest {


    @Test(groups = "1s", timeOut=60000)
    public void testNominal() {
        Model model = new Model();
        SetVar setVar = model.setVar(new int[]{}, new int[]{1, 2, 3, 4, 5});
        IntVar sum = model.intVar(0, 100);
        model.post(sum(setVar, sum, true));
        checkSolutions(model, setVar, sum);
    }

    @Test(groups = "1s", timeOut=60000)
    public void testFixedSum() {
        Model model = new Model();
        SetVar setVar = model.setVar(new int[]{1, 5, 7, 8});
        IntVar sum = model.intVar(0, 100);
        model.post(sum(setVar, sum, true));
        checkSolutions(model, setVar, sum);
    }


    @Test(groups = "1s", timeOut=60000)
    public void testEmptySet() {
        Model model = new Model();
        SetVar setVar = model.setVar(new int[]{});
        IntVar sum = model.intVar(0, 100);
        model.post(sum(setVar, sum, true));

        assertEquals(model.getSolver().isSatisfied(), ESat.FALSE);
        assertFalse(model.solve());
    }


    @Test(groups = "1s", timeOut=60000)
    public void testWrongBounds() {
        Model model = new Model();
        SetVar setVar = model.setVar(new int[]{}, new int[]{1, 2, 5});
        IntVar intVar = model.intVar(9, 100);
        model.post(sum(setVar, intVar, true));

        assertEquals(model.getSolver().isSatisfied(), ESat.FALSE);
        assertFalse(model.solve());
    }

    /* *******************************************
     * Constraint
    ********************************************/

    private Constraint sum(SetVar setVar, IntVar sum, boolean notEmpty) {
        if(notEmpty) {
            return new Constraint("Sum_NE", new PropNotEmpty(setVar), new PropSumOfElements(setVar, sum, true));
        } else {
            return new Constraint("Sum_E", new PropSumOfElements(setVar, sum, false));
        }
    }

    /* *******************************************
     * Helpers
    ********************************************/

    private void checkSolutions(Model model, SetVar var, IntVar sum) {
        int nbSol = 0;
        while(model.solve()) {
            nbSol++;
            int computedSum = 0;
            for (Integer value : var.getValue()) {
                computedSum += value;
            }
            assertEquals(sum.getValue(), computedSum);
        }
        assertTrue(nbSol > 0);
    }

}