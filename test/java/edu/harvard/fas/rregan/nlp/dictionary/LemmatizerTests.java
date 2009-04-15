package edu.harvard.fas.rregan.nlp.dictionary;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.impl.NLPTextImpl;

/**
 * Test the Lemmatizer
 * 
 * @author ron
 */
public class LemmatizerTests extends AbstractIntegrationTestCase {

	public void testLemmatize() {
		NLPProcessor<NLPText> lemmatizer = getNlpProcessorFactory().getLemmatizer();
		// standard suffix replacing tests
		assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.ADJECTIVE)).getLemma());
		assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.NOUN)).getLemma());
		assertEquals("run", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("ringing", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("rings", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("being", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("rang", PartOfSpeech.VERB)).getLemma());

		// special cases
		assertEquals("run", lemmatizer.process(new NLPTextImpl("ran", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("been", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("am", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("is", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("are", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("was", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("were", PartOfSpeech.VERB)).getLemma());
	}

}
