/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.nlp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;

/**
 * Registers the {@link NoOpNLPProcessorFactory} as the {@code nlpProcessorFactory} bean
 * when NLP is disabled ({@code requel.nlp.enabled=false}). The real
 * {@code NLPProcessorFactoryImpl} is gated on the inverse condition
 * ({@code havingValue=true, matchIfMissing=true}), so exactly one factory is active:
 * the real one by default, the no-op when NLP is turned off.
 *
 * <p>
 * {@link ConditionalOnMissingBean} is also applied so the no-op only fills a genuine
 * gap (and supports a future "nlp-jpa absent from the classpath" scope where the real
 * impl is not present at all).
 */
@Configuration
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "false")
public class NoOpNlpConfig {

	@Bean("nlpProcessorFactory")
	@ConditionalOnMissingBean(NLPProcessorFactory.class)
	public NLPProcessorFactory noOpNlpProcessorFactory() {
		return new NoOpNLPProcessorFactory();
	}
}
