package org.mudebug.prapr.core.mutationtest.engine.config;

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

import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.ArgumentsListMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.ArgumentsListMutatorSecondPhase;
import org.mudebug.prapr.core.mutationtest.engine.mutators.CatchTypeWideningMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.DereferenceGuardMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.FactoryMethodMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.FieldAccessToMethodCallMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.FieldNameMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.FieldToLocalAccessMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.LocalNameMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.LocalToFieldAccessMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.LocalToMethodCallMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.MethodNameMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.NonVoidMethodCallGuardMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.PreconditionAdditionMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.ReturningDereferenceGuardMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.ReturningMethodCallGuardMutator;
import org.mudebug.prapr.core.mutationtest.engine.mutators.VoidMethodCallGuardMutator;
import org.pitest.functional.F;
import org.pitest.help.Help;
import org.pitest.help.PitHelpError;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class augments PIT with new APR-specific mutators
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class AugmentedMutator {
    private static final Map<String, Iterable<? extends MethodMutatorFactory>> MUTATORS;

    static {
        MUTATORS = new LinkedHashMap<>();

        /************************************************************************/
        /*******************  Expression Replacement Mutators *******************/
        /************************************************************************/

        addGroup("LOCAL_NAME_MUTATOR", LocalNameMutator.getVariants());

        final List<PraPRMethodMutatorFactory> almVariants = new LinkedList<>();

        almVariants.addAll(Arrays.asList(ArgumentsListMutator.values()));

        almVariants.addAll(Arrays.asList(ArgumentsListMutatorSecondPhase.values()));

        addGroup("ARGUMENTS_LIST_MUTATOR", almVariants);

        addGroup("FIELD_NAME_MUTATOR", FieldNameMutator.getVariants());

        final List<PraPRMethodMutatorFactory> mnVariants = new LinkedList<>();

        mnVariants.addAll(Arrays.asList(MethodNameMutator.values()));

        mnVariants.addAll(Arrays.asList(FactoryMethodMutator.values()));

        addGroup("METHOD_NAME_MUTATOR", mnVariants);

        addGroup("FIELD_ACCESS_TO_METHOD_CALL_MUTATOR", FieldAccessToMethodCallMutator.getVariants());

        addGroup("LOCAL_TO_FIELD_ACCESS_MUTATOR", LocalToFieldAccessMutator.getVariants());

        addGroup("FIELD_TO_LOCAL_ACCESS_MUTATOR", FieldToLocalAccessMutator.getVariants());

        addGroup("LOCAL_TO_METHOD_MUTATOR", LocalToMethodCallMutator.getVariants());

        addGroup("TYPE_REPLACEMENT", CatchTypeWideningMutator.getVariants());

        /************************************************************************/
        /*******************  Conditional Insertion Mutators ********************/
        /************************************************************************/

        addGroup("RET_METHOD_CALL_GUARD_MUTATOR", ReturningMethodCallGuardMutator.getVariants());

        add("VOID_METHOD_CALL_GUARD_MUTATOR", VoidMethodCallGuardMutator.VOID_METHOD_CALL_GUARD_MUTATOR);

        add("PRECONDITION_ADDITION_MUTATOR", PreconditionAdditionMutator.PRECONDITION_ADDITION_MUTATOR);

        addGroup("NON_VOID_METHOD_CALL_GUARD_MUTATOR", NonVoidMethodCallGuardMutator.getVariants());

        addGroup("RET_DEREFERENCE_GUARD_MUTATOR", ReturningDereferenceGuardMutator.getVariants());

        addGroup("DEREFERENCE_GUARD_MUTATOR", DereferenceGuardMutator.getVariants());

        /************************************************************************/
        /************************************************************************/
        /************************************************************************/

        addGroup("PIT", Mutator.all());

        addGroup("ALL", all());
    }

    private static void add(final String key, final MethodMutatorFactory value) {
        MUTATORS.put(key, Collections.singleton(value));
    }

    private static void addGroup(final String key, final Iterable<? extends MethodMutatorFactory> value) {
        MUTATORS.put(key, value);
    }

    public static Collection<? extends MethodMutatorFactory> all() {
        return fromStrings(MUTATORS.keySet());
    }

    public static Collection<MethodMutatorFactory> fromStrings(final Collection<String> names) {
        final Set<MethodMutatorFactory> unique = new TreeSet<>(compareId());
        final F<String, Iterable<? extends MethodMutatorFactory>> fromString = fromString();
        for (final String name : names) {
            for (final MethodMutatorFactory mutator : fromString.apply(name)) {
                unique.add(mutator);
            }
        }
        return unique;
    }

    private static Comparator<? super MethodMutatorFactory> compareId() {
        return new Comparator<MethodMutatorFactory>() {
            @Override
            public int compare(final MethodMutatorFactory o1, final MethodMutatorFactory o2) {
                return o1.getGloballyUniqueId().compareTo(o2.getGloballyUniqueId());
            }
        };
    }

    private static F<String, Iterable<? extends MethodMutatorFactory>> fromString() {
        return new F<String, Iterable<? extends MethodMutatorFactory>>() {
            @Override
            public Iterable<? extends MethodMutatorFactory> apply(final String a) {
                Iterable<? extends MethodMutatorFactory> i = MUTATORS.get(a);
                if (i == null) {
                    throw new PitHelpError(Help.UNKNOWN_MUTATOR, a);
                }
                return i;
            }
        };
    }
}
