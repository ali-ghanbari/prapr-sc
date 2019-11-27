package org.mudebug.prapr.entry.report.compressedxml;

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

import org.apache.commons.io.output.WriterOutputStream;
import org.pitest.util.ResultOutputStrategy;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class CompressedDirectoryResultOutputStrategy implements ResultOutputStrategy {
    private final ResultOutputStrategy core;

    private CompressedDirectoryResultOutputStrategy(ResultOutputStrategy core) {
        this.core = core;
    }

    @Override
    public Writer createWriterForFile(String sourceFile) {
        final WriterOutputStream wos = new WriterOutputStream(core.createWriterForFile(sourceFile),
                StandardCharsets.UTF_8);
        final BufferedOutputStream bos = new BufferedOutputStream(wos);
        try {
            final GZIPOutputStream gzos = new GZIPOutputStream(bos);
            return new OutputStreamWriter(gzos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CompressedDirectoryResultOutputStrategy forResultOutputStrategy(final ResultOutputStrategy dros) {
        return new CompressedDirectoryResultOutputStrategy(dros);
    }
}
