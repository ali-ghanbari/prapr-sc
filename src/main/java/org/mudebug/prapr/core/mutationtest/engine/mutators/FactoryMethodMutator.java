package org.mudebug.prapr.core.mutationtest.engine.mutators;

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
import org.mudebug.prapr.core.commons.ImmutablePair;
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.PraPRMethodInfo;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum FactoryMethodMutator implements PraPRMethodMutatorFactory {
    FACTORY_METHOD_MUTATOR_0(Preference.PREF_0), // 1st subclass 1st ctor
    FACTORY_METHOD_MUTATOR_1(Preference.PREF_0), // 1st subclass 2nd ctor
    FACTORY_METHOD_MUTATOR_2(Preference.PREF_0), // ...
    FACTORY_METHOD_MUTATOR_3(Preference.PREF_0), // 1st subclass nth ctor
    FACTORY_METHOD_MUTATOR_4(Preference.PREF_1), // 2nd subclass 1st ctor
    FACTORY_METHOD_MUTATOR_5(Preference.PREF_1), // 2nd subclass 2nd ctor
    FACTORY_METHOD_MUTATOR_6(Preference.PREF_1), // ...
    FACTORY_METHOD_MUTATOR_7(Preference.PREF_1); // 2nd subclass nth ctor

    private enum Preference {
        PREF_0,
        PREF_1
    }

    private final Preference preference;

    FactoryMethodMutator(Preference preference) {
        this.preference = preference;
    }

    @Override
    public MethodVisitor create(MutationContext context,
                                MethodInfo methodInfo,
                                MethodVisitor methodVisitor,
                                CollectedClassInfo collectedClassInfo,
                                ClassByteArraySource cache,
                                GlobalInfo classHierarchy) {
        if (methodInfo.isConstructor()) {
            return new MethodVisitor(Opcodes.ASM6, methodVisitor) {
                /*do nothing*/
            };
        }
        final FactoryMethodMutatorMethodVisitor fmmmv = new FactoryMethodMutatorMethodVisitor(context,
                methodVisitor, methodInfo, collectedClassInfo, cache, this, classHierarchy);
        final int methodAccess = Commons.getMethodAccess(methodInfo);
        fmmmv.lvs = new LocalVariablesSorter(methodAccess, methodInfo.getMethodDescriptor(), fmmmv);
        return fmmmv.lvs;
    }

    @Override
    public MethodVisitor create(MutationContext mutationContext, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGloballyUniqueId() {
        return String.format("MethodNameMutator_%d", this.ordinal());
    }

    @Override
    public String getName() {
        return this.name();
    }

    public static Iterable<PraPRMethodMutatorFactory> getVariants() {
        return Arrays.<PraPRMethodMutatorFactory> asList(values());
    }

    protected int getOrdinal() {
        for (final FactoryMethodMutator fmm : values()) {
            if (fmm.preference == this.preference) {
                final int base = fmm.ordinal();
                return ordinal() - base;
            }
        }
        return 0; // non-reachable
    }

    protected int getPreferenceOrdinal() {
        return this.preference.ordinal();
    }
}

class FactoryMethodMutatorMethodVisitor extends MethodVisitor {
    private final Map<String, List<PraPRMethodInfo>> mutatedClassMethodsInfo;

    private final FactoryMethodMutator variant;

    private final ClassName mutatedClassName;

    private final ClassByteArraySource cba;

    private final MutationContext context;

    protected LocalVariablesSorter lvs;

    private final GlobalInfo ch;

    FactoryMethodMutatorMethodVisitor(MutationContext context,
                                      MethodVisitor methodVisitor,
                                      MethodInfo methodInfo,
                                      CollectedClassInfo cci,
                                      ClassByteArraySource cache,
                                      FactoryMethodMutator variant,
                                      GlobalInfo classHierarchy) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.mutatedClassMethodsInfo = cci.methodsInfo;
        this.cba = cache;
        this.variant = variant;
        this.mutatedClassName = Commons.getOwningClassName(methodInfo);
        this.ch = classHierarchy;
    }

    private boolean isNonArrayObjectType(final Type type) {
        return type.getSort() == Type.OBJECT;
    }

    private ClassName pickClass(final String superName) {
        final int o = this.variant.getOrdinal() - 1;
        if (o < 0) {
            return ClassName.fromString(superName);
        } else {
            final String[] subClasses = ch.subclassesOf(superName);
            if (subClasses != null) {
                if (o < subClasses.length) {
                    return ClassName.fromString(subClasses[o]);
                }
            }
        }
        return null;
    }

    private ImmutablePair<String, PraPRMethodInfo> pickConstructor(final boolean isPublic,
                                                                   final Map<String, List<PraPRMethodInfo>> methInfo) {
        int count = 0;
        for (final Map.Entry<String, List<PraPRMethodInfo>> ent : methInfo.entrySet()) {
            final String desc = ent.getKey();
            if (Type.getReturnType(desc).getSort() == Type.VOID) { // constructors return void
                for (final PraPRMethodInfo mi : ent.getValue()) {
                    if ((!isPublic || mi.isPublic) && mi.name.equals("<init>")) {
                        if (count == variant.getPreferenceOrdinal()) {
                            return new ImmutablePair<>(desc, mi);
                        }
                        count++;
                    }
                }
            }
        }
        return null;
    }

    private boolean isClassObject(final Type type) {
        if (type.getSort() == Type.OBJECT) {
            return type.getInternalName().equals("java/lang/Object");
        }
        return false;
    }

    private int firstMatch(final Type[] asis, final boolean[] used, final Type cat) {
        for (int i = 0; i < asis.length; i++) {
            if ((asis[i].equals(cat) || isClassObject(asis[i])) && !used[i]) {
                return i;
            }
        }
        return -1;
    }

    private void prepareStack(final Type[] asis, final int[] tempLocals, final Type[] tobe) {
        final boolean[] used = new boolean[asis.length];
        Arrays.fill(used, false);
        for (final Type cat : tobe) {
            final int argIndex = firstMatch(asis, used, cat);
            final int localIndex;
            final Type localType;
            if (argIndex >= 0) {
                used[argIndex] = true;
                localIndex = tempLocals[argIndex];
                localType = asis[argIndex];
            } else { // fall back to using a default value
                localIndex = -1;
                localType = null;
            }
            if (localIndex < 0) {
                Commons.injectDefaultValue(this.mv, cat);
            } else {
                if (isClassObject(localType) && cat.getSort() != Type.OBJECT) {
                    Commons.injectDefaultValue(this.mv, cat);
                } else {
                    Commons.injectLocalValue(this.mv, localIndex, cat);
                }
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        final Type returnType = Type.getReturnType(desc);
        if (isNonArrayObjectType(returnType)) {
            final ClassName returnTypeClassName = pickClass(returnType.getInternalName());
            if (returnTypeClassName != null) {
                final Map<String, List<PraPRMethodInfo>> methInfo;
                final boolean isPublic;
                if (mutatedClassName.equals(returnTypeClassName)) {
                    methInfo = mutatedClassMethodsInfo;
                    isPublic = false;
                } else {
                    final CollectedClassInfo cci = ClassInfoCollector.collect(cba, returnTypeClassName.asInternalName());
                    methInfo = cci.methodsInfo;
                    isPublic = true;
                }
                final ImmutablePair<String, PraPRMethodInfo> ctor = pickConstructor(isPublic, methInfo);
                if (ctor != null) {
                    final String msg = String.format("the call to factory method %s.%s%s is "
                                    + "replaced by an instantiation of type %s using %s",
                            owner.replace('/', '.'),
                            name,
                            desc,
                            returnTypeClassName.asJavaName(),
                            ctor.getFirst());
                    final MutationIdentifier newId = this.context.registerMutation(this.variant, msg);
                    if (this.context.shouldMutate(newId)) {
                        final Type[] asis = Type.getArgumentTypes(desc);
                        final int[] tempLocals = Commons.createTempLocals(lvs, asis);
                        Commons.storeValues(this.mv, asis, tempLocals);
                        if (!Commons.isStaticCall(opcode)) {
                            super.visitInsn(Opcodes.POP); //get rid of any receiver object
                        }
                        final String toBeInstantiatedInternalName = ctor.getSecond().owningClassName.asInternalName();
                        super.visitTypeInsn(Opcodes.NEW, toBeInstantiatedInternalName);
                        super.visitInsn(Opcodes.DUP);
                        final Type[] tobe = Type.getArgumentTypes(ctor.getFirst());
                        prepareStack(asis, tempLocals, tobe);
                        super.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                toBeInstantiatedInternalName,
                                "<init>", ctor.getFirst(), false);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

}