package com.rreganjr.nlp.dictionary;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.impl.NLPTextImpl;
import org.junit.Assert;

/**
 * Test the Lemmatizer
 * 
 * @author ron
 */
public class LemmatizerTests extends AbstractIntegrationTestCase {

	public void testLemmatize() {
		NLPProcessor<NLPText> lemmatizer = getNlpProcessorFactory().getLemmatizer();
		// standard suffix replacing tests
		Assert.assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.ADJECTIVE)).getLemma());
		Assert.assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.NOUN)).getLemma());
		Assert.assertEquals("run", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("ring", lemmatizer.process(new NLPTextImpl("ringing", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("ring", lemmatizer.process(new NLPTextImpl("rings", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("being", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("ring", lemmatizer.process(new NLPTextImpl("rang", PartOfSpeech.VERB)).getLemma());

		// special cases
		Assert.assertEquals("run", lemmatizer.process(new NLPTextImpl("ran", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("been", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("am", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("is", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("are", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("was", PartOfSpeech.VERB)).getLemma());
		Assert.assertEquals("be", lemmatizer.process(new NLPTextImpl("were", PartOfSpeech.VERB)).getLemma());
	}

}
