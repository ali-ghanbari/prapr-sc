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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

/**
 * A set of utility methods useful for mutating JVM byetcode.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class Commons {
    private Commons() {
        
    }
    
    public static MethodVisitor dummyMethodVisitor(final MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM6, mv) {
            /* do nothing */
        };
    }
    
    public static LocalVarInfo pickLocalVariable(final List<LocalVarInfo> visibleLocals,
            final String desc,
            final int skip,
            final int index) {
        int count = skip;
        for (final LocalVarInfo lvi : visibleLocals) {
            if (lvi.typeDescriptor.equals(desc)) {
                if (index == count) {
                    return lvi;
                }
                count++;
            }
        }
        return null;
    }
    
    public static FieldInfo pickField(final Map<String, List<FieldInfo>> fieldsInfo,
            final String desc,
            final int skip,
            final int index,
            final boolean accessedInStaticMeth) {
        final List<FieldInfo> fil = fieldsInfo.get(desc);
        int count = skip;
        if (fil != null) {
            if (accessedInStaticMeth) {
                for (final FieldInfo fi : fil) {
                    if (fi.isStatic) {
                        if (count == index) {
                            return fi;
                        }
                        count++;
                    }
                }
            } else {
                for (final FieldInfo fi : fil) {
                    if (count == index) {
                        return fi;
                    }
                    count++;
                }
            }
        }
        return null; 
    }
    
    public static int[] createTempLocals(final LocalVariablesSorter lvs, final Type... types) {
        final int[] tempLocals = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            tempLocals[i] = lvs.newLocal(types[i]);
        }
        return tempLocals;
    }
    
    public static void storeValues(final MethodVisitor mv, final Type[] args, final int[] tempLocals) {
        for (int i = tempLocals.length - 1; i >= 0; i--) {
            final int tempLocal = tempLocals[i];
            switch (args[i].getSort()) {
            case Type.OBJECT:
            case Type.METHOD:
            case Type.ARRAY:
                mv.visitVarInsn(Opcodes.ASTORE, tempLocal);
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FSTORE, tempLocal);
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LSTORE, tempLocal);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DSTORE, tempLocal);
                break;
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitVarInsn(Opcodes.ISTORE, tempLocal);
                break;
            default:
                throw new RuntimeException();
            }
        }
    }
    
    public static void restoreValues(final MethodVisitor mv, final int[] tempLocals, final Type[] args) {
        for (int i = 0; i < args.length; i++) {
            final int tempLocal = tempLocals[i];
            switch (args[i].getSort()) {
            case Type.OBJECT:
            case Type.METHOD:
            case Type.ARRAY:
                mv.visitVarInsn(Opcodes.ALOAD, tempLocal);
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FLOAD, tempLocal);
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LLOAD, tempLocal);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DLOAD, tempLocal);
                break;
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitVarInsn(Opcodes.ILOAD, tempLocal);
                break;
            default:
                throw new RuntimeException();
            }
        }
    }
    
    public static void injectFieldValue(final MethodVisitor mv,
                                        final int baseIndex,
                                        final FieldInfo fi,
                                        final Type expectedType) {
        final String desc = expectedType.getDescriptor();
        final String ownerInternalName = fi.owningClassName.asInternalName();
        final String name = fi.name;
        if (fi.isStatic) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, ownerInternalName, name, desc);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, baseIndex);
            mv.visitFieldInsn(Opcodes.GETFIELD, ownerInternalName, name, desc);
        }
    }
    
    public static void injectLocalValue(final MethodVisitor mv,
                                        final int local,
                                        final Type expectedType) {
        switch (expectedType.getSort()) {
        case Type.OBJECT:
        case Type.METHOD:
        case Type.ARRAY:
            mv.visitVarInsn(Opcodes.ALOAD, local);
            break;
        case Type.FLOAT:
            mv.visitVarInsn(Opcodes.FLOAD, local);
            break;
        case Type.LONG:
            mv.visitVarInsn(Opcodes.LLOAD, local);
            break;
        case Type.DOUBLE:
            mv.visitVarInsn(Opcodes.DLOAD, local);
            break;
        case Type.BYTE:
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.SHORT:
        case Type.INT:
            mv.visitVarInsn(Opcodes.ILOAD, local);
            break;
        default:
            throw new RuntimeException();
        }
    }
    
    public static void injectDefaultValue(final MethodVisitor mv,
                                          final Type expectedType) {
        switch (expectedType.getSort()) {
        case Type.OBJECT:
        case Type.METHOD:
        case Type.ARRAY:
            mv.visitInsn(Opcodes.ACONST_NULL);
            break;
        case Type.FLOAT:
            mv.visitInsn(Opcodes.FCONST_0);
            break;
        case Type.LONG:
            mv.visitInsn(Opcodes.LCONST_0);
            break;
        case Type.DOUBLE:
            mv.visitInsn(Opcodes.DCONST_0);
            break;
        case Type.BYTE:
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.SHORT:
        case Type.INT:
            mv.visitInsn(Opcodes.ICONST_0);
            break;
        default:
            throw new RuntimeException();
        }
    }
    
    public static void injectReturnStmt(final MethodVisitor mv,
            final Type returnType,
            final LocalVarInfo lvi,
            final FieldInfo fi) {
        final int mutatedMethodReturnSort = returnType.getSort();
        if (mutatedMethodReturnSort == Type.VOID) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            if (lvi == null && fi == null) {
                injectDefaultValue(mv, returnType);
            } else if (fi == null) {
                injectLocalValue(mv, lvi.index, returnType);
            } else {
                injectFieldValue(mv, 0, fi, returnType);
            }
            switch (mutatedMethodReturnSort) {
            case Type.OBJECT:
            case Type.METHOD:
            case Type.ARRAY:
                mv.visitInsn(Opcodes.ARETURN);
                break;
            case Type.FLOAT:
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case Type.DOUBLE:
                mv.visitInsn(Opcodes.DRETURN);
                break;
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(Opcodes.IRETURN);
                break;
            default:
                throw new RuntimeException();
            }
        }
    }
    
    public static String defValString(final Type type) {
        switch (type.getSort()) {
        case Type.OBJECT:
        case Type.METHOD:
        case Type.ARRAY: 
            return "null";
        case Type.FLOAT:
            return "0.F";
        case Type.LONG:
            return "0L";
        case Type.DOUBLE:
            return "0.D";
        case Type.BYTE:
            return "0";
        case Type.BOOLEAN:
            return "false";
        case Type.CHAR:
            return "'\\0'";
        case Type.SHORT:
            return "0";
        case Type.INT:
            return "0";
        default:
            throw new RuntimeException();
        }
    }
    
    public static boolean isVirtualCall(int opcode) {
        switch (opcode) {
        case Opcodes.INVOKEINTERFACE:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKEVIRTUAL:
            return true;
        }
        return false;
    }

    public static boolean isStaticCall(int opcode) {
        return opcode == Opcodes.INVOKESTATIC;
    }

    private static class SimpleClassVisitor extends ClassVisitor {
        private String superName;

        public SimpleClassVisitor() {
            super(Opcodes.ASM6);
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces) {
            this.superName = superName;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        public String getSuperName() {
            return superName;
        }
    }

    public static String getSupertype(final ClassByteArraySource cache,
                                      final String typeInternalName) {
        final byte[] bytes = cache.getBytes(typeInternalName).value();
        final SimpleClassVisitor cv = new SimpleClassVisitor();
        final ClassReader cr = new ClassReader(bytes);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cv.getSuperName();
    }

    /**
     *
     * @param methodInfo
     * @return
     * @since 2.0.3
     */
    public static ClassName getOwningClassName(final MethodInfo methodInfo) {
        final String methodName = methodInfo.getDescription();
        final int indexOfColon = methodName.indexOf(':');
        return ClassName.fromString(methodName.substring(0, indexOfColon));
    }

    /**
     *
     * @param methodInfo
     * @return
     * @since 2.0.3
     */
    public static int getMethodAccess(final MethodInfo methodInfo) {
        try {
            final Field accessField = MethodInfo.class.getDeclaredField("access");
            accessField.setAccessible(true);
            return accessField.getInt(methodInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }
}