package org.mudebug.prapr.core.analysis;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.util.Log;

/**
 * Represents information that some of mutators need to conduct mutation.
 * These information include class hierarchy and the list of factory method.
 *
 * @author Ali Ghanbari  (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class GlobalInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /*super class internal name -> [subclass internal names]*/
    private final Map<String, String[]> classHierarchy;

    /*class internal name -> [(static & public) factory methods]*/
    private final Map<String, FactoryMethod[]> factoryMethods;

    private GlobalInfo() {
        this.classHierarchy = new HashMap<>();
        this.factoryMethods = new HashMap<>();
    }

    public String[] subclassesOf(final String superclass) {
        return classHierarchy.get(superclass);
    }

    public FactoryMethod[] factoryMethodsFor(final String className) {
        return factoryMethods.get(className);
    }

    private String[] addClassName(String[] names, String name) {
        String[] namesExt = new String[names.length + 1];
        System.arraycopy(names, 0, namesExt, 0, names.length);
        namesExt[names.length] = name;
        return namesExt;
    }

    private void addFactoryMethod(final String className, FactoryMethod method) {
        FactoryMethod[] methods = factoryMethods.get(className);
        if (methods == null) {
            methods = new FactoryMethod[0];
        }
        FactoryMethod[] methodsExt = new FactoryMethod[methods.length + 1];
        System.arraycopy(methods, 0, methodsExt, 0, methods.length);
        methodsExt[methods.length] = method;
        factoryMethods.put(className, methodsExt);
    }

    private void addSubclass(final String superclass, final String subclass) {
        String[] names = classHierarchy.get(superclass);
        if (names == null) {
            names = new String[0];
        }
        names = addClassName(names, subclass);
        classHierarchy.put(superclass, names);
    }

    private static void populate(final GlobalInfo ch, final InputStream is) throws Exception {
        final BasicClassVisitor classVisitor = new BasicClassVisitor();
        final ClassReader reader = new ClassReader(is);
        reader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        final String subclass = classVisitor.name;
        for (final String superclass : classVisitor.supers) {
            ch.addSubclass(superclass, subclass);
        }
        for (final FactoryMethod fm : classVisitor.factoryMethods) {
            ch.addFactoryMethod(fm.getReturnType().getInternalName(), fm);
        }
    }

    public static GlobalInfo construct(final List<File> files) {
        final GlobalInfo ch = new GlobalInfo();
        for (final File file : files) {
            try {
                if (file.getName().endsWith(".jar")) {
                    final JarFile jar = new JarFile(file);
                    for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
                        JarEntry entry = (JarEntry) enums.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            InputStream is = jar.getInputStream(entry);
                            populate(ch, is);
                            is.close();
                        }
                    }
                    jar.close();
                } else {
                    InputStream is = new FileInputStream(file);
                    populate(ch, is);
                    is.close();
                }

            } catch (Exception e) {
                Log.getLogger().info("OOPS! Something went wrong while reading the file " + file.getAbsolutePath());
                Log.getLogger().info("\t" + e.getMessage());
            }
        }
        return ch;
    }

    private static class BasicClassVisitor extends ClassVisitor {
        final List<FactoryMethod> factoryMethods;
        final List<String> supers;
        private boolean isInterface;
        String name;

        BasicClassVisitor() {
            super(Opcodes.ASM6);
            supers = new ArrayList<>();
            factoryMethods = new ArrayList<>();
            isInterface = false;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            this.name = name;
            this.supers.add(superName);
            if (interfaces != null) {
                for (final String si : interfaces) {
                    this.supers.add(si);
                }
            }
            isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methName, String desc, String signature,
                                         String[] exceptions) {
            if ((access & Opcodes.ACC_STATIC) != 0) {
                final int returnTypeSort = Type.getReturnType(desc).getSort();
                if (returnTypeSort == Type.OBJECT) {
                    factoryMethods.add(new FactoryMethod(this.name, methName, desc, isInterface));
                }
            }
            return super.visitMethod(access, methName, desc, signature, exceptions);
        }

    }
}