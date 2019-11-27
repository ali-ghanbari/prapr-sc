package org.mudebug.prapr.core.mutationtest.engine;

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

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public interface PraPRMethodMutatorFactory extends MethodMutatorFactory {
    MethodVisitor create(final MutationContext context,
                         final MethodInfo methodInfo,
                         final MethodVisitor methodVisitor,
                         final CollectedClassInfo collectedClassInfo,
                         final ClassByteArraySource cache,
                         final GlobalInfo classHierarchy);
}
