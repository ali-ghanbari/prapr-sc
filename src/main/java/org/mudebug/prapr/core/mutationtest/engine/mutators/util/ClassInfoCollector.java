package org.mudebug.prapr.core.mutationtest.engine.mutators.util;

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

import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.functional.Option;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.util.Log;

/**
 * Utility methods for collecting information about classes to be mutated.
 * These information include list of fields, methods, and locals within each method.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class ClassInfoCollector {
    public static CollectedClassInfo collect(final byte[] bytes) {
        final ClassReader reader = new ClassReader(bytes);
        final CollectorClassVisitor ccv = new CollectorClassVisitor();
        reader.accept(ccv, ClassReader.EXPAND_FRAMES);
        return ccv.getCollectedClassInfo();
    }

    public static CollectedClassInfo collect(final ClassByteArraySource cache,
                                             final String className) {
        if (className.startsWith("[")) { // className is an array type descriptor
            return new CollectedClassInfo();
        } else { //className is the internal name of some class
            final Option<byte[]> bytes = cache.getBytes(className);
            if (bytes.hasSome()) {
                return collect(bytes.value());
            } else {
                final String javaName = className.replace('/', '.');
                Log.getLogger().warning("OOPS! Something went wrong in reading/parsing the class " + javaName);
                return new CollectedClassInfo();
            }
        }
    }
}