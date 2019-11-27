package org.mudebug.prapr.core;

/*
 * #%L
 * prapr-plugin
 * %%
 * Copyright (C) 2018 - 2019 University of Texas at Dallas
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum SuspStrategyImpl implements SuspStrategy {
    OCHIAI {
        @Override
        public double computeSusp(int ef, int ep, int nf, int np) {
            final double denom = Math.sqrt((ef + ep) * (ef + nf));
            return denom > 0.D ? ef / denom : 0.D;
        }
    },
    TARANTULA {
        @Override
        public double computeSusp(int ef, int ep, int nf, int np) {
            final double denom1 = (ef + nf) == 0 ? 0.D : ef / ((double) (ef + nf));
            final double denom2 = (ep + np) == 0 ? 0.D : ep / ((double) (ep + np));
            final double denom = denom1 + denom2;
            return denom > 0.D ? denom1 / denom : 0.D;
        }
    }
}
