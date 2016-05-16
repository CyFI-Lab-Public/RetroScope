/*
 * Copyright (C) 2010 The Android Open Source Project
 *
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
 */

package vogar;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The expected result of an action execution. This is typically encoded in the
 * expectations text file, which has the following format:
 * <pre>
 * test java.io.StreamTokenizer.Reset
 * result UNSUPPORTED
 * pattern .*should get token \[, but get -1.*
 *
 * # should we fix this?
 * test java.util.Arrays.CopyMethods
 * result COMPILE_FAILED
 * pattern .*cannot find symbol.*
 * </pre>
 */
public final class Expectation {

    /** The pattern to use when no expected output is specified */
    public static final Pattern MATCH_ALL_PATTERN
            = Pattern.compile(".*", Pattern.MULTILINE | Pattern.DOTALL);

    /** The expectation of a general successful run. */
    public static final Expectation SUCCESS = new Expectation(Result.SUCCESS, MATCH_ALL_PATTERN,
            Collections.<String>emptySet(), "", -1);

    /** Justification for this expectation */
    private final String description;

    /** The action's expected result, such as {@code EXEC_FAILED}. */
    private final Result result;

    /** The pattern the expected output will match. */
    private final Pattern pattern;

    /** Attributes of this test. */
    private final Set<String> tags;

    /** The tracking bug ID */
    private final long bug;

    /** True if the identified bug still active. */
    private boolean bugIsOpen = false;

    public Expectation(Result result, Pattern pattern, Set<String> tags, String description, long bug) {
        if (result == null || description == null || pattern == null) {
            throw new IllegalArgumentException(
                    "result=" + result + " description=" + description + " pattern=" + pattern);
        }

        this.description = description;
        this.result = result;
        this.pattern = pattern;
        this.tags = new LinkedHashSet<String>(tags);
        this.bug = bug;
    }

    public String getDescription() {
        return description;
    }

    public long getBug() {
        return bug;
    }

    public Result getResult() {
        return result;
    }

    public Set<String> getTags() {
        return tags;
    }

    /**
     * Set the current status of this expectation's bug. When a bug is open,
     * any result (success or failure) is permitted.
     */
    public void setBugIsOpen(boolean bugIsOpen) {
        this.bugIsOpen = bugIsOpen;
    }

    /**
     * Returns true if {@code outcome} matches this expectation.
     */
    public boolean matches(Outcome outcome) {
        return patternMatches(outcome) && (bugIsOpen || result == outcome.getResult());
    }

    private boolean patternMatches(Outcome outcome) {
        return pattern.matcher(outcome.getOutput()).matches();
    }

    @Override public String toString() {
        return "Expectation[description=" + description + " pattern=" + pattern.pattern() + "]";
    }
}
