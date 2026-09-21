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
 * is nothing here to resolve against the rest of the file. The phonetic code is carried for
 * completeness but is recomputed on import rather than trusted: it has to agree with the
 * transformator the running installation uses, not the one that wrote the file.
 *
 * @author ron
 */
public class DictionaryWordImportDraft {

    private final String lemma;
    private final String phoneticCode;

    private DictionaryWordImportDraft(Builder builder) {
        this.lemma = builder.lemma;
        this.phoneticCode = builder.phoneticCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLemma() {
        return lemma;
    }

    public String getPhoneticCode() {
        return phoneticCode;
    }

    public static final class Builder {
        private String lemma;
        private String phoneticCode;

        public Builder lemma(String lemma) {
            this.lemma = lemma;
            return this;
        }

        public Builder phoneticCode(String phoneticCode) {
            this.phoneticCode = phoneticCode;
            return this;
        }

        public DictionaryWordImportDraft build() {
            return new DictionaryWordImportDraft(this);
        }
    }
}
