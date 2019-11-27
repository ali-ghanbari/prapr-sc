package org.mudebug.prapr.core.commons;

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
 * A set of utility methods for using ANSI codes for colorful/stylized writing
 * on the screen.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class TextStyleUtil {
    private static final String BOLD_FACE = "\033[1m";

    private static final String UNDERLINED = "\033[4m";

    private static final String NORMAL = "\033[0m";

    public static String bold(final String text) {
        return BOLD_FACE + text + NORMAL;
    }

    public static String underlined(final String text) {
        return UNDERLINED + text + NORMAL;
    }
}
