package org.mudebug.prapr.entry.report.log;

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

import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.Objects;

import static org.mudebug.prapr.entry.report.Commons.sanitizeMutatorName;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class MutationResultWrapper {
    private final MutationResult mutationResult;

    private MutationResultWrapper(MutationResult mutationResult) {
        this.mutationResult = mutationResult;
    }

    public static MutationResultWrapper wrap(final MutationResult mr) {
        return new MutationResultWrapper(mr);
    }

    public MutationResult getMutationResult() {
        return mutationResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MutationResultWrapper that = (MutationResultWrapper) o;
        final DetectionStatus status = this.mutationResult.getStatus();
        final MutationDetails mutationDetails = this.mutationResult.getDetails();
        final ClassName className = mutationDetails.getClassName();
        final String mutatorId = sanitizeMutatorName(mutationDetails.getMutator());
        final int lineNumber = mutationDetails.getLineNumber();
        final String desc = mutationDetails.getDescription();
        return status == that.mutationResult.getStatus()
                && Objects.equals(className, that.mutationResult.getDetails().getClassName())
                && Objects.equals(mutatorId, sanitizeMutatorName(that.mutationResult.getDetails().getMutator()))
                && lineNumber == that.mutationResult.getDetails().getLineNumber()
                && Objects.equals(desc, that.mutationResult.getDetails().getDescription());
    }

    @Override
    public int hashCode() {
        final DetectionStatus status = this.mutationResult.getStatus();
        final MutationDetails mutationDetails = this.mutationResult.getDetails();
        final ClassName className = mutationDetails.getClassName();
        final String mutatorId = sanitizeMutatorName(mutationDetails.getMutator());
        final int lineNumber = mutationDetails.getLineNumber();
        final String desc = mutationDetails.getDescription();
        return Objects.hash(status, className, mutatorId, lineNumber, desc);
    }
}
