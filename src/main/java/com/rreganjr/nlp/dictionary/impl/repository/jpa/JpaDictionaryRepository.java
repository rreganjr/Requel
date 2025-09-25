/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.nlp.dictionary.impl.repository.jpa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

import org.fife.com.swabunga.spell.engine.DoubleMeta;
import org.fife.com.swabunga.spell.engine.SpellDictionary;
import org.fife.com.swabunga.spell.engine.SpellDictionaryHashMap;
import org.fife.com.swabunga.spell.engine.Transformator;
import org.fife.com.swabunga.spell.event.SpellChecker;
import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import com.rreganjr.validator.InvalidStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//import com.swabunga.spell.engine.DoubleMeta;
//import com.swabunga.spell.engine.SpellDictionary;
//import com.swabunga.spell.engine.SpellDictionaryHashMap;
//import com.swabunga.spell.engine.Transformator;
//import com.swabunga.spell.event.SpellChecker;

import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.Category;
import com.rreganjr.nlp.dictionary.Dictionary;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Lexlinkref;
import com.rreganjr.nlp.dictionary.LexlinkrefId;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.SemcorFile;
import com.rreganjr.nlp.dictionary.SemcorSentence;
import com.rreganjr.nlp.dictionary.SemcorSentenceWord;
import com.rreganjr.nlp.dictionary.Semlinkref;
import com.rreganjr.nlp.dictionary.SemlinkrefId;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.VerbNetSelectionRestrictionType;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.dictionary.impl.repository.NoSuchWordException;
import com.rreganjr.nlp.impl.DatabaseSpellDictionary;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.jpa.AbstractJpaRepository;
import com.rreganjr.repository.jpa.ExceptionMapper;
import com.rreganjr.repository.jpa.GenericPropertyValueExceptionAdapter;
import com.rreganjr.repository.jpa.InvalidStateExceptionAdapter;
import com.rreganjr.repository.jpa.OptimisticLockExceptionAdapter;
import com.rreganjr.requel.NoSuchEntityException;

/**
 * EJB3/JPA based repository
 * 
 * @author ron
 */
@Repository("dictionaryRepository")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { NoResultException.class, NoSuchEntityException.class, EntityException.class })
public class JpaDictionaryRepository extends AbstractJpaRepository implements DictionaryRepository {

	/**
	 * The name of the property in the DictionaryTool.properties file that
	 * contains a pipe "|" delimited list of paths to dictionary files relative
	 * to the classpath.<br>
	 * the simplest dictionary is "nlp/jazzy/english.0"
	 * 
	 * see PROP_ENGLISH_DICTIONARY_FILES_DEFAULT for the default paths of the
	 *      files
	 */
	public static final String PROP_ENGLISH_DICTIONARY_FILES = "EnglishDictionaryFiles";

	/**
	 * The default path to the tokenizer model file
	 */
	public static final String PROP_ENGLISH_DICTIONARY_FILES_DEFAULT = "nlp/jazzy/eng_com.dic|"
			+ "nlp/jazzy/center.dic|"
			+ "nlp/jazzy/color.dic|"
			+ "nlp/jazzy/ize.dic|"
			+ "nlp/jazzy/labeled.dic|"
			+ "nlp/jazzy/yze.dic";

	private static final Collection<SpellDictionary> staticDictionaries;
	static {
		try {
			List<SpellDictionary> dictionaries = new ArrayList<SpellDictionary>(10);
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					JpaDictionaryRepository.class.getName());

			String dictionaryFilePaths = resourceBundleHelper.getString(
					PROP_ENGLISH_DICTIONARY_FILES, PROP_ENGLISH_DICTIONARY_FILES_DEFAULT);

			if (dictionaryFilePaths.contains("|")) {
				for (String dictionaryFilePath : dictionaryFilePaths.split("\\|")) {
					if (!"".equals(dictionaryFilePath.trim())) {
						log.info("loading dictionary: " + dictionaryFilePath);
						try (InputStreamReader reader =  new InputStreamReader(JpaDictionaryRepository.class.getClassLoader().getResourceAsStream(dictionaryFilePath))) {
							// TODO: try using SpellDictionaryDisk for less memory usage than SpellDictionaryHashMap
							dictionaries.add(new SpellDictionaryHashMap(reader));
						}
					}
				}
			} else {
				try (InputStreamReader reader =  new InputStreamReader(JpaDictionaryRepository.class.getClassLoader().getResourceAsStream(dictionaryFilePaths))) {
					// TODO: try using SpellDictionaryDisk for less memory usage than SpellDictionaryHashMap
					dictionaries.add(new SpellDictionaryHashMap(reader));
				}
			}
			staticDictionaries = Collections.unmodifiableCollection(dictionaries);
		} catch (Exception e) {
			log.error("Error initializing dictionary repository", e);
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Transformator jazzyTransformator = new DoubleMeta();
	private SpellChecker spellChecker = new SpellChecker();

	/**
	 * @param exceptionMapper
	 */
	@Autowired
	public JpaDictionaryRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
		spellChecker = new SpellChecker();
		for (SpellDictionary dictionary : staticDictionaries) {
			spellChecker.addDictionary(dictionary);
		}
		try {
			// Passing null as the phonetic code file causes the dictionary to
			// use the DoubleMeta Transformator to create phentic codes for
			// spelling corrections.
			spellChecker.setUserDictionary(new DatabaseSpellDictionary(this, (File) null));
		} catch (IOException e) {
			// This will never happen because null is passed as the phonetic
			// code file.
		}

		addExceptionAdapter(PropertyValueException.class,
				new GenericPropertyValueExceptionAdapter(), Word.class, Category.class,
				Synset.class);

		addExceptionAdapter(InvalidStateException.class, new InvalidStateExceptionAdapter(),
				Word.class, Category.class, Synset.class);

		addExceptionAdapter(OptimisticLockException.class, new OptimisticLockExceptionAdapter(),
				Word.class, Category.class, Synset.class);

		addExceptionAdapter(StaleObjectStateException.class, new OptimisticLockExceptionAdapter(),
				Word.class, Category.class, Synset.class);

		addExceptionAdapter(LockAcquisitionException.class, new OptimisticLockExceptionAdapter(),
				Word.class, Category.class, Synset.class);

		addExceptionAdapter(CannotAcquireLockException.class, new OptimisticLockExceptionAdapter(),
				Word.class, Category.class, Synset.class);

        addExceptionAdapter(ObjectOptimisticLockingFailureException.class,
                new OptimisticLockExceptionAdapter(), Word.class, Category.class, Synset.class);
	}

	protected SpellChecker getSpellChecker() {
		return spellChecker;
	}

	public Boolean isKnownWord(String word) {
		if (isNumber(word)) {
			return Boolean.TRUE;
		}
		return getSpellChecker().isCorrect(word);
	}

	public List<String> findSpellingSuggestions(String word, int threshold) {
		List<String> results = new ArrayList<String>();
		boolean containsDigits = containsDigits(word);

		for (Object obj : getSpellChecker().getSuggestions(word, threshold)) {
			org.fife.com.swabunga.spell.engine.Word jazzyWord = (org.fife.com.swabunga.spell.engine.Word) obj;
			// if the word doesn't contain any digits, don't include numbers in
			// the suggestions
			if (!containsDigits && isNumber(jazzyWord.getWord())) {
				continue;
			}
			results.add(jazzyWord.getWord());
		}
		return results;
	}

	@Override
	public Set<Sense> findMoreSpecificWords(Sense sense, int maxSuggestions) {
		sense = get(sense);
		Set<Sense> suggestions = new HashSet<Sense>();
		// map of senses by the number of senses of its word
		Map<Integer, List<Sense>> senseByWordSenseCount = new HashMap<Integer, List<Sense>>();
		for (Synset hyponym : findHyponyms(sense.getSynset(), 1)) {
			// use the senses of the words with the least number of senses
			for (Sense s : hyponym.getSenses()) {
				int senseCount = s.getWord().getSenses(sense.getSynset().getPartOfSpeech()).size();
				List<Sense> senses = senseByWordSenseCount.get(senseCount);
				if (senses == null) {
					senses = new ArrayList<Sense>();
					senseByWordSenseCount.put(senseCount, senses);
				}
				senses.add(s);
			}
		}
		for (int senseCount = 1; senseByWordSenseCount.size() > 0; senseCount++) {
			if (senseByWordSenseCount.containsKey(senseCount)) {
				for (Sense s : senseByWordSenseCount.remove(senseCount)) {
					suggestions.add(s);
					if (suggestions.size() >= maxSuggestions) {
						break;
					}
				}
			}
			if (suggestions.size() >= maxSuggestions) {
				break;
			}
		}
		return suggestions;
	}

	/**
	 * Calculate the information content of a synset using the measure of Seco,
	 * Veale and Hayes, which uses the number of sub-terms (hyponyms) to
	 * determine how specific a term is. All synsets with no hyponyms are
	 * considered most specific and given a IC rank of 1.0. An artificial root
	 * node for all synsets of the same part of speech is assumed to better
	 * scale the information content of the lowest common subsumers, especially
	 * in the verb hierarchies which tend to be shallow and disjoint.
	 * 
	 * @param synset
	 * @param linkType
	 * @return
	 */
	public double infoContent(Synset synset, Linkdef linkType) {
		synset = get(synset);
		Integer conceptCount = getConceptCount(synset.getPos());
		double infoContent = (1.0d - ((Math.log(synset.getSubsumerCount(linkType) + 1)) / (Math
				.log(conceptCount))));
		if (log.isDebugEnabled()) {
			log.debug("synset: " + synset + " conceptCount: " + conceptCount + " ic: "
					+ infoContent);
		}
		return infoContent;
	}

	private boolean containsDigits(String word) {
		for (char c : word.toCharArray()) {
			if (Character.isDigit(c)) {
				return true;
			}
		}
		return false;
	}

	private boolean isNumber(String word) {
		try {
			Integer.parseInt(word);
			return true;
		} catch (NumberFormatException e) {
		}
		try {
			Double.parseDouble(word);
			return true;
		} catch (NumberFormatException e) {
		}
		return false;
	}

	public void addToDictionary(String word) {
		getSpellChecker().addToDictionary(word);
	}

	@Override
	public Dictionary getDictionary() {
		return getDictionary(null, null);
	}

	@Override
	public Dictionary getDictionary(String firstWordStartsWith, String lastWordStartsWith) {
		try {
			Dictionary dictionary = new Dictionary();
			if (((firstWordStartsWith == null) || "".equals(firstWordStartsWith.trim()))
					&& ((lastWordStartsWith == null) || "".equals(lastWordStartsWith.trim()))) {
				dictionary.setCategories(findCategories());
				dictionary.setWords(findWords());
				dictionary.setSynsets(findSynsets());
				dictionary.setSenses(findSenses());
			} else {
				Query query = getEntityManager()
						.createQuery(
								"select object(word) from Word as word where word.lemma >= :start and word.lemma < :end");
				query.setParameter("start", firstWordStartsWith);
				query.setParameter("end", lastWordStartsWith);
				for (Word word : (List<Word>) query.getResultList()) {
					dictionary.getWords().add(word);
					for (Sense sense : word.getSenses()) {
						Synset synset = sense.getSynset();
						dictionary.getSynsets().add(synset);
						dictionary.getCategories().add(synset.getCategory());
					}
				}
			}
			return dictionary;
		} catch (Exception e) {
			throw new RuntimeException("failed to get dictionary between '" + firstWordStartsWith
					+ "'" + " and '" + lastWordStartsWith + "'", e);
		}
	}

	@Override
	public Category findCategory(PartOfSpeech partOfSpeech, String name) {
		String lookupName = partOfSpeech.name().toLowerCase() + "." + name.toLowerCase();
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(category) from Category as category where category.name like :lookupName");
			query.setParameter("lookupName", lookupName);
			return (Category) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Category.class, new String[] { "partOfSpeech",
					"name" }, new Object[] { partOfSpeech, name });
		} catch (Exception e) {
			throw new RuntimeException("failed to find category for name '" + lookupName + "'", e);
		}
	}

	@Override
	public Word findWord(Long id) {
		return getEntityManager().find(Word.class, id);
	}

	/**
	 * @param word -
	 *            the base form of the word
	 * @return
	 * @throws NoSuchWordException
	 */
	public Word findWord(String word) throws NoSuchWordException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(word) from Word as word where word.lemma like :word");
			query.setParameter("word", word);
			return (Word) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchWordException.forLemma(word);
		} catch (Exception e) {
			throw new RuntimeException("failed to find word for text '" + word + "'", e);
		}
	}

	/**
	 * @param lemma -
	 *            the base form of the word
	 * @param pos -
	 *            part of speech
	 * @return
	 * @throws NoSuchWordException
	 */
	public Word findWord(String lemma, PartOfSpeech pos) throws NoSuchWordException {
		try {
			Word word = findWord(lemma);
			for (Sense sense : word.getSenses(pos)) {
				if (sense.getSynset().isPartOfSpeech(pos)) {
					return word;
				}
			}
			throw NoSuchWordException.forLemmaAndPOS(lemma, pos.name());
		} catch (NoSuchWordException e) {
			throw e;
		} catch (NoResultException e) {
			throw NoSuchWordException.forLemma(lemma);
		} catch (Exception e) {
			throw new RuntimeException("failed to find word '" + lemma + "' pos " + pos, e);
		}
	}

	/**
	 * @param lemma -
	 *            the base form of the word
	 * @return
	 * @throws NoSuchWordException
	 */
	public Word findWordExact(String lemma) throws NoSuchWordException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(word) from Word as word where word.lemma = :lemma");
			query.setParameter("lemma", lemma);
			return (Word) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchWordException.forLemma(lemma);
		} catch (Exception e) {
			throw new RuntimeException("failed to find word for text '" + lemma + "'", e);
		}
	}

	/**
	 * @param lemma -
	 *            the base form of the word
	 * @param pos -
	 *            part of speech
	 * @return
	 * @throws NoSuchWordException
	 */
	public Word findWordExact(String lemma, PartOfSpeech pos) throws NoSuchWordException {
		try {
			Word word = findWordExact(lemma);
			for (Sense sense : word.getSenses(pos)) {
				if (sense.getSynset().isPartOfSpeech(pos)) {
					return word;
				}
			}
			throw NoSuchWordException.forLemmaAndPOS(lemma, pos.name());
		} catch (NoSuchWordException e) {
			throw e;
		} catch (NoResultException e) {
			throw NoSuchWordException.forLemma(lemma);
		} catch (Exception e) {
			throw new RuntimeException("failed to find word '" + lemma + "' pos " + pos, e);
		}
	}

	@Override
	public String generatePhoneticCode(String word) {
		return jazzyTransformator.transform(word);
	}

	public List<Word> findWordsByPhoneticCode(String phoneticCode) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(word) from Word as word where word.phoneticCode = :phoneticCode");
			query.setParameter("phoneticCode", phoneticCode);
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to find words by phonetic code '" + phoneticCode
					+ "'", e);
		}
	}

	/**
	 * @return
	 * @throws RuntimeException
	 */
	public Collection<Word> findWords() throws RuntimeException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery("select object(word) from Word as word");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to find all words.", e);
		}
	}

	protected Collection<Word> getWords(int minLength, int maxLength) throws RuntimeException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(word) from Word as word "
							+ "where length(word.lemma) >= :minLength "
							+ "and length(word.lemma) <= :maxLength");
			query.setParameter("minLength", minLength);
			query.setParameter("maxLength", maxLength);
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to find words of length " + minLength + " to "
					+ maxLength, e);
		}
	}

	/**
	 * @return
	 * @throws RuntimeException
	 */
	public Collection<Category> findCategories() throws RuntimeException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(category) from Category as category");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to find all categories.", e);
		}
	}

	public Collection<Sense> findSenses() throws RuntimeException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery("select object(sense) from Sense as sense");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all senses.", e);
		}
	}

	@Override
	public Sense findSense(String lemma, PartOfSpeech partOfSpeech, int rank) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(sense) from Sense as sense "
							+ "where sense.word.lemma = :lemma and "
							+ "sense.synset.pos = :pos and sense.rank = :rank");
			query.setParameter("lemma", lemma);
			query.setParameter("pos", PartOfSpeech.toWordNetPOS(partOfSpeech));
			query.setParameter("rank", rank);
			return (Sense) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Sense.class, new String[] { "lemma",
					"partOfSpeech", "rank" }, new Object[] { lemma, partOfSpeech, rank });
		} catch (Exception e) {
			throw new RuntimeException("failed to find sense for lemma '" + lemma + "' pos '"
					+ partOfSpeech + "' and rank " + rank, e);
		}
	}

	@Override
	public Sense findSensesByWordAndSynset(Word word, Synset synset) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(sense) from Sense as sense "
							+ "where sense.word = :word and sense.synset = :synset");
			query.setParameter("word", word);
			query.setParameter("synset", synset);
			return (Sense) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Sense.class, new String[] { "word", "synset" },
					new Object[] { word, synset });
		} catch (Exception e) {
			throw new RuntimeException("failed to find sense for word '" + word + "' and synset '"
					+ synset + "'", e);
		}
	}

	@Override
	public Sense findSensesByLemmaAndSynsetId(String lemma, Long synsetId) {
		return findSensesByWordAndSynset(findWordExact(lemma), findSynset(synsetId));
	}

	@Override
	public Sense findSensesByWordnetSenseKey(String senseKey) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(sense) from Sense as sense where sense.senseKey = :senseKey");
			query.setParameter("senseKey", senseKey);
			return (Sense) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Sense.class, "senseKey", senseKey);
		} catch (Exception e) {
			throw new RuntimeException("failed to find sense for sense key '" + senseKey + "'", e);
		}
	}

	/**
	 * @return
	 * @throws RuntimeException
	 */
	public Collection<Synset> findSynsets() throws RuntimeException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(synset) from Synset as synset");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all synsets.", e);
		}
	}

	@Override
	public Lexlinkref findLexlinkref(LexlinkrefId id) {
		return findLexlinkref(findSensesByWordAndSynset(findWord(id.getWord1id()), findSynset(id
				.getSynset1id())), findSensesByWordAndSynset(findWord(id.getWord2id()),
				findSynset(id.getSynset2id())), findLinkDef(id.getLinkid()));
	}

	@Override
	public Lexlinkref findLexlinkref(Sense fromSense, Sense toSense, Linkdef linkType) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(lexlink) from Lexlinkref as lexlink "
							+ "where lexlink.fromSynset = :fromSynset "
							+ "and lexlink.fromWord = :fromWord "
							+ "and lexlink.toSynset = :toSynset " + "and lexlink.toWord = :toWord "
							+ "and lexlink.linkType = :linkType ");
			query.setParameter("fromSynset", fromSense.getSynset());
			query.setParameter("fromWord", fromSense.getWord());
			query.setParameter("toSynset", toSense.getSynset());
			query.setParameter("toWord", toSense.getWord());
			query.setParameter("linkType", linkType);
			return (Lexlinkref) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Semlinkref.class, new String[] { "fromSense",
					"toSense", "linkDef" }, new Object[] { fromSense, toSense, linkType });
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find " + linkType.getName()
					+ " lexo-semantic link from " + fromSense + " to " + toSense, e);
		}
	}

	@Override
	public Collection<Lexlinkref> findLexlinkref(Sense fromSense, Sense toSense) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(lexlink) from Lexlinkref as lexlink "
							+ "where lexlink.fromSynset = :fromSynset "
							+ "and lexlink.fromWord = :fromWord "
							+ "and lexlink.toSynset = :toSynset " + "and lexlink.toWord = :toWord "
							+ "and lexlink.linkType = :linkType ");
			query.setParameter("fromSynset", fromSense.getSynset());
			query.setParameter("fromWord", fromSense.getWord());
			query.setParameter("toSynset", toSense.getSynset());
			query.setParameter("toWord", toSense.getWord());
			return query.getResultList();
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find lexo-semantic link from " + fromSense
					+ " to " + toSense, e);
		}
	}

	public Collection<Linkdef> findLinkdefs() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(linkdef) from Linkdef as linkdef");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all semantic link definitions.", e);
		}
	}

	@Override
	public boolean buildSynsetDefinitionWords() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select count(synset) from Synset as synset where synset.wsd is not null and synset.wsd <> '' and not exists elements(synset.words)");
			long count = ((Long) query.getSingleResult()).longValue();
			// TODO: there is one sense that doesn't have a parsed definition,
			// may be a bug in the importer.
			return count > 1L;
		} catch (Exception e) {
			throw new RuntimeException(
					"failed determining if synset definition words need to be expanded.", e);
		}
	}

	@Override
	public boolean buildSynsetLinkPathsAndCounts() {
		try {
			Query query = getEntityManager().createNativeQuery(
					"select count(*) from synset_subsumer_counts");
			long count = ((BigInteger) query.getSingleResult()).longValue();
			return count == 0L;
		} catch (Exception e) {
			throw new RuntimeException(
					"failed determining if synset semantic link paths and counts need to be expanded.",
					e);
		}
	}

	public boolean buildSenseKeys() {
		try {
			Query query = getEntityManager().createNativeQuery(
					"select count(*) from sense where sense_key is null");
			long count = ((BigInteger) query.getSingleResult()).longValue();
			return count > 0L;
		} catch (Exception e) {
			throw new RuntimeException("failed determining if senses need sense key assignment.", e);
		}
	}

	@Override
	public List<Object[]> findSemanticLinks() {
		try {
			Query query = getEntityManager()
					.createNativeQuery(
							"select synset1id, synset2id, linkid, distance from semlinkref order by synset1id, linkid");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get semantic links.", e);
		}
	}

	@Override
	public Linkdef findLinkDef(Long id) {
		return getEntityManager().find(Linkdef.class, id);
	}

	@Override
	public Synset findSynset(Long id) {
		return getEntityManager().find(Synset.class, id);
	}

	@Override
	public List<Synset> findSynsetsWithColocatedDefinitionSenseAndWord(Sense sense, Word word) {
		log.debug("sense: " + sense + " word: " + word);
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createNativeQuery(
							"select sy2.* "
									+ "from synset sy1 "
									+ "left join synset_definition_word sdw1 on (sdw1.synset_id = sy1.synsetid) "
									+ "left join synset_definition_word sdw2 on (sdw2.synset_id = sy1.synsetid) "
									+ "left join synset sy2 on (sy2.synsetid = sdw2.sense_id) "
									+ "where sdw1.sense_id = :sense "
									+ "and   sdw2.word_id  = :word " +

									"union " +

									"select * from synset where synsetid = :sense ", Synset.class);
			query.setParameter("sense", sense.getId());
			query.setParameter("word", word.getId());
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<Synset>();
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find colocation in synset defintions with "
					+ sense + " and " + word, e);
		}
	}

	@Override
	public List<Synset> findSynsetsWithColocatedDefinitionWords(Word word1, Word word2) {
		log.debug("word1: " + word1 + " word2: " + word2);
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createNativeQuery(
							"select sy2.* "
									+ "from synset sy1 "
									+ "left join synset_definition_word sdw1 on (sdw1.synset_id = sy1.synsetid) "
									+ "left join synset_definition_word sdw2 on (sdw2.synset_id = sy1.synsetid) "
									+ "left join synset sy2 on (sy2.synsetid = sdw2.sense_id) "
									+ "where sdw1.word_id = :word1 "
									+ "and   sdw2.word_id  = :word2 "
									+

									" union "
									+

									"select sy2.* "
									+ "from synset sy1 "
									+ "left join synset_definition_word sdw1 on (sdw1.synset_id = sy1.synsetid) "
									+ "left join synset_definition_word sdw2 on (sdw2.synset_id = sy1.synsetid) "
									+ "left join synset sy2 on (sy2.synsetid = sdw1.sense_id) "
									+ "where sdw1.word_id = :word1 "
									+ "and   sdw2.word_id  = :word2", Synset.class);
			query.setParameter("word1", word1.getId());
			query.setParameter("word2", word2.getId());
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<Synset>();
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find colocation in synset defintions with "
					+ word1 + " and " + word2, e);
		}
	}

	@Override
	public Semlinkref findSemlinkref(SemlinkrefId id) {
		return findSemlinkref(findSynset(id.getSynset1id()), findSynset(id.getSynset2id()),
				findLinkDef(id.getLinkid()), id.getDistance());
	}

	@Override
	public Semlinkref findSemlinkref(Synset fromSynset, Synset toSynset, Linkdef linkType,
			Integer distance) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(semlink) from Semlinkref as semlink "
							+ "where semlink.fromSynset = :fromSynset "
							+ "and semlink.toSynset = :toSynset "
							+ "and semlink.linkType = :linkType "
							+ "and semlink.id.distance = :distance");
			query.setParameter("fromSynset", fromSynset);
			query.setParameter("toSynset", toSynset);
			query.setParameter("linkType", linkType);
			query.setParameter("distance", distance);
			return (Semlinkref) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Semlinkref.class, new String[] { "fromSynset",
					"toSynset", "linkDef", "distance" }, new Object[] { fromSynset, toSynset,
					linkType, distance });
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find " + linkType.getName()
					+ " semantic link from " + fromSynset + " to " + toSynset + " at distance "
					+ distance, e);
		}
	}

	private final Map<SynsetPair, Set<Synset>> lowestCommonHypernymsCache = new HashMap<SynsetPair, Set<Synset>>();

	@Override
	public Set<Synset> getLowestCommonHypernyms(Synset synset1, Synset synset2) {
		try {
			Set<Synset> lcsSet = new HashSet<Synset>();
			SynsetPair synsetPair = new SynsetPair(synset1, synset2);
			if (lowestCommonHypernymsCache.containsKey(synsetPair)) {
				for (Synset synset : lowestCommonHypernymsCache.get(synsetPair)) {
					// get a fresh copy attached to the persistence context
					lcsSet.add(get(synset));
				}
			} else {
				if (synset1.getPartOfSpeech().equals(synset2.getPartOfSpeech())) {
					Query query = getEntityManager().createNativeQuery(
							"select syn.* " + "from synset syn "
									+ "join semlinkref slr1 on (slr1.synset2id = syn.synsetid and slr1.linkid = 1) "
									+ "join semlinkref slr2 on (slr2.synset2id = syn.synsetid and slr2.linkid = 1) "
									+ "where slr1.synset1id = :synset1id "
									+ "and   slr2.synset1id = :synset2id "
									+ "and   (slr1.distance + slr2.distance) = ("
									+ "    select min(slr1b.distance + slr2b.distance) "
									+ "    from semlinkref slr1b "
									+ "         join semlinkref slr2b on (slr1b.synset2id = slr2b.synset2id) "
									+ "    where slr1b.synset1id = :synset1id "
									+ "      and slr2b.synset1id = :synset2id "
									+ "      and slr1b.linkid = 1 "
									+ "      and slr2b.linkid = 1"
									+ "  )",
							Synset.class);
					query.setParameter("synset1id", synset1.getId());
					query.setParameter("synset2id", synset2.getId());
					lcsSet.addAll(query.getResultList());
				}
				lowestCommonHypernymsCache.put(synsetPair, lcsSet);
			}
			if (log.isDebugEnabled()) {
				log.debug("synset1: " + synset1 + " synset2: " + synset2 + " -> " + lcsSet);
			}
			return Collections.unmodifiableSet(lcsSet);
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to get lowest common hypernyms for " + synset1
					+ " and " + synset2, e);
		}
	}

	private final Map<Synset, Synset> rootHypernymCache = new HashMap<Synset, Synset>();

	@Override
	public Synset getRootHypernym(Synset synset) {
		try {
			Synset rootHypernym;
			if (rootHypernymCache.containsKey(synset)) {
				rootHypernym = rootHypernymCache.get(synset);
			} else {
				Query query = getEntityManager().createNativeQuery(
						"select syn.* " + "from synset syn left join semlinkref slr1 on ( "
								+ "	slr1.synset2id = syn.synsetid) "
								+ "where slr1.synset1id = :synsetid and  slr1.linkid = 1 "
								+ "order by distance desc ", Synset.class);
				query.setParameter("synsetid", synset.getId());
				query.setMaxResults(1);
				rootHypernym = (Synset) query.getSingleResult();
				rootHypernymCache.put(synset, rootHypernym);
			}
			if (log.isDebugEnabled()) {
				log.debug("synset: " + synset + " rootHypernym: " + rootHypernym);
			}
			return rootHypernym;
		} catch (NoResultException e) {
			// return the original synset
			return synset;
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to get root hypernym for " + synset, e);
		}
	}

	private final Map<String, Integer> posConceptCountCache = new HashMap<String, Integer>();

	@Override
	public Integer getConceptCount(String pos) {
		try {
			if (!posConceptCountCache.containsKey(pos)) {
				Query query = getEntityManager().createNativeQuery(
						"select count(*) from synset where pos = :pos");
				query.setParameter("pos", pos);
				posConceptCountCache.put(pos, ((BigInteger) query.getSingleResult()).intValue());
			}
			return posConceptCountCache.get(pos);
		} catch (Exception e) {
			throw new RuntimeException("failed to get concept count for " + pos, e);
		}
	}

	private static class SynsetPair {
		private final Synset synset1;
		private final Synset synset2;

		private SynsetPair(Synset synset1, Synset synset2) {
			this.synset1 = synset1;
			this.synset2 = synset2;
		}

		public Synset getSynset1() {
			return synset1;
		}

		public Synset getSynset2() {
			return synset2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((synset1 == null) ? 0 : synset1.hashCode());
			result = prime * result + ((synset2 == null) ? 0 : synset2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SynsetPair other = (SynsetPair) obj;
			if (synset1 == null) {
				if (other.synset1 != null) {
					return false;
				}
			} else if (!synset1.equals(other.synset1)) {
				return false;
			}
			if (synset2 == null) {
				if (other.synset2 != null) {
					return false;
				}
			} else if (!synset2.equals(other.synset2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "(" + getSynset1() + ", " + getSynset2() + ")";
		}
	}

	@Override
	public Collection<SemcorSentence> findSemcorSentences() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(sentence) from SemcorSentence as sentence");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all Semcor Sentences.", e);
		}
	}

	@Override
	public Collection<SemcorSentence> findSemcorSentences(String sectionName, String fileName) {
		throw new RuntimeException("not implemented.");
	}

	@Override
	public Collection<SemcorSentence> findSemcorSentences(Sense sense) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(sentence) from SemcorSentence as sentence where :word in elements(sentence.words)");
			query.setParameter("word", sense.getWord());
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all Semcor Sentences for sense " + sense, e);
		}
	}

	@Override
	public Collection<SemcorSentence> findSemcorSentences(Word word) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(sentence) from SemcorSentence as sentence where :word in elements(sentence.words)");
			query.setParameter("word", word);
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all Semcor Sentences for word " + word, e);
		}
	}

	@Override
	public SemcorFile findSemcorFile(String corpusSectionFolder, String corpusFileName) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(file) from SemcorFile as file where file.corpusSectionFolder like :corpusSectionFolder and file.corpusFileName like :corpusFileName");
			query.setParameter("corpusSectionFolder", corpusSectionFolder);
			query.setParameter("corpusFileName", corpusFileName);
			return (SemcorFile) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(SemcorFile.class, new String[] {
					"corpusSectionFolder", "corpusFileName" }, new Object[] { corpusSectionFolder,
					corpusFileName });
		} catch (Exception e) {
			throw new RuntimeException("failed to find semcor file by section "
					+ corpusSectionFolder + " and file " + corpusFileName, e);
		}
	}

	@Override
	public Collection<SemcorFile> findSemcorFiles() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(file) from SemcorFile as file");
			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("failed to get all Semcor Sentences.", e);
		}
	}

	@Override
	public List<SemcorSentenceWord> findSemcorSentencesWithColocatedDefinitionSenseAndWord(
			Sense sense, Word word) {
		log.debug("sense: " + sense + " word: " + word);
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createNativeQuery(
					"select ssw1.* from semcor_sentence_word ssw1 "
							+ "left join semcor_sentence ss1 on ( ssw1.sentence_id = ss1.id "
							+ ") left join semcor_sentence_word ssw2 on ( "
							+ "ssw2.sentence_id = ss1.id  ) where ssw1.sense_id = :sense "
							+ "and ssw2.word_id = :word " +

							"union " +

							"select ssw2.* from semcor_sentence_word ssw1 "
							+ "left join semcor_sentence ss1 on ( ssw1.sentence_id = ss1.id "
							+ ") left join semcor_sentence_word ssw2 on ( "
							+ "ssw2.sentence_id = ss1.id ) where ssw1.sense_id = :sense "
							+ "and ssw2.word_id = :word", SemcorSentenceWord.class);
			query.setParameter("sense", sense.getId());
			query.setParameter("word", word.getId());
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<SemcorSentenceWord>();
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find colocation in semcor sentences with "
					+ sense + " and " + word, e);
		}
	}

	@Override
	public List<SemcorSentenceWord> findSemcorSentencesWithColocatedDefinitionWords(Word word1,
			Word word2) {
		log.debug("word1: " + word1 + " word2: " + word2);
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createNativeQuery(
					"select ssw1.* from semcor_sentence_word ssw1 "
							+ "left join semcor_sentence ss1 on ( ssw1.sentence_id = ss1.id "
							+ ") left join semcor_sentence_word ssw2 on ( "
							+ "ssw2.sentence_id = ss1.id  ) where ssw1.word_id = :word1 "
							+ "and ssw2.word_id = :word2 " +

							"union " +

							"select ssw2.* from semcor_sentence_word ssw1 "
							+ "left join semcor_sentence ss1 on ( ssw1.sentence_id = ss1.id "
							+ ") left join semcor_sentence_word ssw2 on ( "
							+ "ssw2.sentence_id = ss1.id ) where ssw1.word_id = :word1 "
							+ "and ssw2.word_id = :word2", SemcorSentenceWord.class);
			query.setParameter("word1", word1.getId());
			query.setParameter("word2", word2.getId());
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<SemcorSentenceWord>();
		} catch (Exception e) {
			log.debug(e, e);
			throw new RuntimeException("failed to find colocation in semcor sentences with "
					+ word1 + " and " + word2, e);
		}
	}

	@Override
	public VerbNetSelectionRestrictionType findVerbNetSelectionRestrictionType(String name) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(selResType) from VerbNetSelectionRestrictionType as selResType "
							+ " where selResType.name like :name");
			query.setParameter("name", name);
			return (VerbNetSelectionRestrictionType) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException
					.byQuery(VerbNetSelectionRestrictionType.class, "name", name);
		} catch (Exception e) {
			throw new RuntimeException(
					"failed to get VerbNetSelectionRestriction by name: " + name, e);
		}
	}

	@Override
	public boolean isHyponym(Sense hypernym, Sense sense) {
		return isHyponym(hypernym.getSynset(), sense.getSynset());
	}

	@Override
	public boolean isHyponym(Synset hypernym, Synset synset) {
		log.debug("checking if " + synset + " is a hyponym of " + hypernym);
		try {
			Query query = getEntityManager().createNativeQuery(
					"select count(*) from semlinkref slr1 left join synset hypernym on ( "
							+ "slr1.synset2id = hypernym.synsetid) "
							+ "left join synset hyponym on ( "
							+ "slr1.synset1id = hyponym.synsetid) "
							+ "where hypernym.synsetid = :hypernym "
							+ "and hyponym.synsetid = :hyponym and  slr1.linkid = 1");
			query.setParameter("hypernym", hypernym.getId());
			query.setParameter("hyponym", synset.getId());
			long count = ((BigInteger) query.getSingleResult()).longValue();
			return count > 0L;
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("failed to determine if  " + synset + " is a hyponym of  "
					+ hypernym, e);
		}
	}

	@Override
	public Collection<Synset> findHyponyms(Synset hypernym, int maxDistance) {
		log.debug("get the hyponyms of " + hypernym + " with a max distance of " + maxDistance);
		try {
			Query query = getEntityManager().createNativeQuery(
					"select sy1.* from semlinkref slr "
							+ "left join synset sy1 on (slr.synset1id = sy1.synsetid) "
							+ "left join sense sn1 on (sy1.synsetid = sn1.synsetid) "
							+ "left join word wd1 on (sn1.wordid = wd1.wordid) "
							+ "left join synset sy2 on (slr.synset2id = sy2.synsetid) "
							+ "left join sense sn2 on (sy2.synsetid = sn2.synsetid) "
							+ "left join word wd2 on (sn2.wordid = wd2.wordid) "
							+ "where slr.linkid = 1 " + "and sy2.synsetid = :hypernym "
							+ "and slr.distance <= :maxDistance", Synset.class);
			query.setParameter("hypernym", hypernym.getId());
			query.setParameter("maxDistance", maxDistance);
			return query.getResultList();
		} catch (Exception e) {
			log.error(e, e);
			throw new RuntimeException("failed to get the hyponyms of " + hypernym
					+ " with a max distance of " + maxDistance, e);
		}
	}
}
