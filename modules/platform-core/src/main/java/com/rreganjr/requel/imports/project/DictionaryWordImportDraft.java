/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.imports.project;

/**
 * One word from a project file's {@code <dictionary>} block (issue #313).
 * <p>
 * Deliberately thin. A project dictionary word has no external id, no creator and no annotations —
 * it is a string that is spelled correctly in one project — so unlike the other import drafts there
 * is nothing here to resolve against the rest of the file.
 * <p>
 * The file's {@code phoneticCode} attribute is deliberately <em>not</em> carried. The import
 * recomputes the code with the running installation's transformator rather than trusting the one
 * that wrote the file, so a field here would be written and never read.
 *
 * @author ron
 */
public class DictionaryWordImportDraft {

    private final String lemma;

    private DictionaryWordImportDraft(Builder builder) {
        this.lemma = builder.lemma;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLemma() {
        return lemma;
    }

    public static final class Builder {
        private String lemma;

        public Builder lemma(String lemma) {
            this.lemma = lemma;
            return this;
        }

        public DictionaryWordImportDraft build() {
            return new DictionaryWordImportDraft(this);
        }
    }
}
