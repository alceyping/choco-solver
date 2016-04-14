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
package org.chocosolver.solver.constraints.extension.nary;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 08/06/11
 */
public class TuplesLargeTable extends LargeRelation {

    /**
     * the number of dimensions of the considered tuples
     */
    private final int n;

    /**
     * The consistency matrix
     */
    private final TIntObjectHashMap<TIntSet> tables;

    /**
     * lower bound of each variable
     */
    private final int[] lowerbounds;

    /**
     * upper bound of each variable
     */
    private final int[] upperbounds;

    private final boolean feasible;

    /**
     * in order to speed up the computation of the index of a tuple
     * in the table, blocks[i] stores the product of the size of variables j with j < i.
     */
    private long[] blocks;

    public TuplesLargeTable(Tuples tuples, IntVar[] vars) {
        n = vars.length;
        lowerbounds = new int[n];
        upperbounds = new int[n];
        feasible = tuples.isFeasible();

        blocks = new long[n];
        long totalSize = 1;
        for (int i = 0; i < n; i++) {
            blocks[i] = totalSize;
            lowerbounds[i] = vars[i].getLB();
            upperbounds[i] = vars[i].getUB();
            totalSize *= upperbounds[i] - lowerbounds[i] + 1;
            if (totalSize < 0) { // to prevent from integer overflow
                totalSize = -1;
            }
        }
        if ((totalSize / Integer.MAX_VALUE) + 1 < 0 || (totalSize / Integer.MAX_VALUE) + 1 > Integer.MAX_VALUE)
            throw new SolverException("Tuples required too much memory ...");

        tables = new TIntObjectHashMap<>();
        int nt = tuples.nbTuples();
        for (int i = 0; i < nt; i++) {
            int[] tuple = tuples.get(i);
            if (valid(tuple, vars)) {
                setTuple(tuple);
            }
        }
    }

    public boolean checkTuple(int[] tuple) {
        long address = 0;
        for (int i = (n - 1); i >= 0; i--) {
            if ((tuple[i] < lowerbounds[i]) || (tuple[i] > upperbounds[i])) {
                return false;
            }
            address += (tuple[i] - lowerbounds[i]) * blocks[i];
        }
        int a = (int) (address % Integer.MAX_VALUE);
        int t = (int) (address / Integer.MAX_VALUE);
        TIntSet ts = tables.get(t);
        return ts != null && ts.contains(a);
    }

    public boolean isConsistent(int[] tuple) {
        return checkTuple(tuple) == feasible;
    }

    private void setTuple(int[] tuple) {
        long address = 0;
        for (int i = (n - 1); i >= 0; i--) {
            address += (tuple[i] - lowerbounds[i]) * blocks[i];
        }
        int a = (int) (address % Integer.MAX_VALUE);
        int t = (int) (address / Integer.MAX_VALUE);
        TIntSet ts = tables.get(t);
        if (ts == null) {
            ts = new TIntHashSet();
            tables.put(t, ts);
        }
        ts.add(a);
    }
}
