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
package com.rreganjr.requel.assistant.legacynlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.TextEntity;

class LexicalGlossaryTermAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final DictionaryRepository dictionaryRepository = mock(DictionaryRepository.class);
	private final LexicalGlossaryTermAssistant assistant = new LexicalGlossaryTermAssistant(
			nlpProcessorFactory, projectRepository, dictionaryRepository);

	private final ProjectOrDomain project = mock(ProjectOrDomain.class);
	private final SortedSet<GlossaryTerm> terms = new TreeSet<>(Comparator.comparing(
			GlossaryTerm::getName, Comparator.nullsFirst(Comparator.<String>naturalOrder())));
	private final Set<Actor> actors = new HashSet<>();
	private final Set<ProjectOrDomainEntity> entities = new LinkedHashSet<>();
	@SuppressWarnings("unchecked")
	private final NLPProcessor<Collection<NLPText>> nounPhraseFinder = mock(NLPProcessor.class);

	LexicalGlossaryTermAssistantTest() {
		doReturn(terms).when(project).getGlossaryTerms();
		doReturn(actors).when(project).getActors();
		doReturn(entities).when(project).getProjectEntities();
		when(nlpProcessorFactory.getNounPhraseFinder()).thenReturn(nounPhraseFinder);
	}

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-lexical-glossary-term");
		assertThat(assistant.targetType()).isEqualTo(TextEntity.class);
	}

	@Test
	void blankTextProducesNoActions() {
		AssistantResult result = assistant.analyze(context(), textEntity(null, "", ""));
		assertThat(result.annotationActions()).isEmpty();
	}

	/**
	 * #268/#269: the issue quotes the phrase as the author wrote it ("Cloud-Watch Alarm", which the
	 * parser splits into "Cloud - Watch Alarm"), and a phrase that isn't in the text (the pre-#314
	 * fragment "ermissions matrix") raises nothing. Three other entities use the phrase, so it
	 * passes the threshold.
	 */
	@Test
	void quotesThePhraseAsWrittenAndDropsOneThatIsNotInTheText() {
		used(3, "Cloud-Watch Alarm");
		String text = "Route the Cloud-Watch Alarm and check the permissions matrix.";
		List<NLPText> written = leaves(nnp("Cloud"), punct("-"), nnp("Watch"), nnp("Alarm"));
		List<NLPText> fragment = leaves(nn("ermissions"), nn("matrix"));

		List<String> issues = issueWords(analyze(text, List.of(verb("Route")),
				phrase(written), phrase(fragment)));

		assertThat(issues).containsExactly("Cloud-Watch Alarm");
	}

	@Test
	void anExistingTermMatchesWithoutItsDeterminerAndInThePlural() {
		GlossaryTerm room = term("Room", 5L);
		GlossaryTerm testRoom = term("Test room", 6L);
		String text = "End the room, then the rooms, then a test room.";

		AssistantResult result = analyze(text, List.of(verb("End")),
				phrase(leaves(dt("the"), nn("room"))), phrase(leaves(dt("a"), nn("test"), nn("room"))));
		AssistantResult plural = analyze("Archive the rooms.", List.of(verb("Archive")),
				phrase(leaves(dt("the"), nns("rooms"))));

		assertThat(issueWords(result)).isEmpty();
		assertThat(refererTermIds(result)).containsExactlyInAnyOrder(room.getId(), testRoom.getId());
		assertThat(refererTermIds(plural)).containsExactly(room.getId());
	}

	@Test
	void anActorNameRaisesNothing() {
		actor("Zoom Host");
		assertThat(issueWords(analyze("Ask the Zoom Host.", List.of(verb("Ask")),
				phrase(leaves(dt("the"), nnp("Zoom"), nnp("Host")))))).isEmpty();
	}

	@Test
	void verbPhrasesCoordinationsPossessivesAndIndefinitesAreNotTerms() {
		used(5, "archives the stream", "stream key and RTMP endpoint", "CON-3685's manual",
				"anything else");
		assertThat(issueWords(analyze("The operator archives the stream.", List.of(dt("The")),
				phrase(leaves(nns("archives"), dt("the"), nn("stream")))))).isEmpty();
		assertThat(issueWords(analyze("Rotate the stream key and RTMP endpoint.",
				List.of(verb("Rotate")), phrase(leaves(nn("stream"), nn("key"), cc("and"),
						nnp("RTMP"), nn("endpoint")))))).isEmpty();
		assertThat(issueWords(analyze("Read CON-3685's manual.", List.of(verb("Read")),
				phrase(leaves(nnp("CON-3685"), pos("'s"), nn("manual")))))).isEmpty();
		assertThat(issueWords(analyze("Check anything else.", List.of(verb("Check")),
				phrase(leaves(nn("anything"), jj("else")))))).isEmpty();
	}

	@Test
	void aSentenceInitialThirdPersonVerbIsNotATermButBusFactorIs() {
		used(5, "alarms route", "bus factor");
		Word alarmVerb = mock(Word.class);
		when(dictionaryRepository.findWord("alarm", PartOfSpeech.VERB)).thenReturn(alarmVerb);
		NLPText alarms = nns("Alarms");
		NLPText bus = nn("Bus");

		assertThat(issueWords(analyze("Alarms route to the pager.", List.of(),
				phrase(leaves(alarms, nn("route")))))).isEmpty();
		assertThat(issueWords(analyze("Bus factor is one.", List.of(),
				phrase(leaves(bus, nn("factor")))))).containsExactly("Bus factor");
	}

	@Test
	void aSingleCommonNounIsNeverATermButAProperNounOrAcronymIs() {
		used(9, "stream", "Conduit", "IVS", "Archive");
		assertThat(issueWords(analyze("Watch the stream.", List.of(verb("Watch")),
				phrase(leaves(dt("the"), nn("stream")))))).isEmpty();
		// Sentence-initial capitals say nothing; mid-sentence ones do.
		NLPText archive = nnp("Archive");
		assertThat(issueWords(analyze("Archive the room.", List.of(), phrase(leaves(archive)))))
				.isEmpty();
		assertThat(issueWords(analyze("Deploy Conduit now.", List.of(verb("Deploy")),
				phrase(leaves(nnp("Conduit")))))).containsExactly("Conduit");
		assertThat(issueWords(analyze("IVS records it.", List.of(), phrase(leaves(nnp("IVS"))))))
				.containsExactly("IVS");
		// #268: a lowercase word the parser tagged NNP is not a proper noun.
		assertThat(issueWords(analyze("It is done entirely offline.", List.of(verb("is")),
				phrase(leaves(nnp("entirely")))))).isEmpty();
	}

	@Test
	void theLettersOfATicketKeyAreNotATerm() {
		assertThat(issueWords(analyze("See CON-3685 for details.", List.of(verb("See")),
				phrase(leaves(nnp("CON")))))).isEmpty();
	}

	@Test
	void aCommonPhraseNeedsThreeEntitiesAProperNounOne() {
		used(2, "admin console");
		String text = "Open the admin console as Chris Peterson.";
		List<NLPText> adminConsole = leaves(dt("the"), nn("admin"), nn("console"));
		List<NLPText> chris = leaves(nnp("Chris"), nnp("Peterson"));

		assertThat(issueWords(analyze(text, List.of(verb("Open")), phrase(adminConsole),
				phrase(chris)))).containsExactly("Chris Peterson");
		used(3, "admin console");
		assertThat(issueWords(analyze(text, List.of(verb("Open")), phrase(adminConsole))))
				.containsExactly("admin console");
	}

	@Test
	void theIssueIsProjectScopedAndSharedByItsLowerCasedPhrase() {
		AssistantResult result = analyze("Deploy Conduit now.", List.of(verb("Deploy")),
				phrase(leaves(nnp("Conduit"))));
		AnnotationAction issue = result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.findFirst().orElseThrow();
		assertThat(issue.metadata()).containsEntry("mustResolve", Boolean.FALSE)
				.containsEntry("scope", "PROJECT")
				.containsEntry("shareKey", "glossary-term:conduit")
				.containsEntry("findingType", "glossary-term");
	}

	@Test
	void aGlossaryTermIsNotCheckedAgainstItself() {
		GlossaryTerm target = mock(GlossaryTerm.class);
		doReturn(GlossaryTerm.class).when(target).getProjectOrDomainEntityInterface();
		when(target.getId()).thenReturn(9L);
		when(target.getName()).thenReturn("Dry-run event");
		when(target.getText()).thenReturn("A Dry-run event is a rehearsal.");
		doReturn(project).when(target).getProjectOrDomain();
		assertThat(assistant.analyze(context(), target).annotationActions()).isEmpty();
	}

	@Test
	void singularHandlesTheCommonPlurals() {
		assertThat(LexicalGlossaryTermAssistant.singular("rooms")).isEqualTo("room");
		assertThat(LexicalGlossaryTermAssistant.singular("test rooms")).isEqualTo("test room");
		assertThat(LexicalGlossaryTermAssistant.singular("entities")).isEqualTo("entity");
		assertThat(LexicalGlossaryTermAssistant.singular("access")).isEqualTo("access");
		assertThat(LexicalGlossaryTermAssistant.singular("status")).isEqualTo("status");
		assertThat(LexicalGlossaryTermAssistant.singular("ties")).isEqualTo("tie");
		assertThat(LexicalGlossaryTermAssistant.singular("bus")).isEqualTo("bus");
	}

	@Test
	void aPhraseInTheNameAndTheTextIsRaisedOnce() {
		parse("Use Conduit", List.of(verb("Use")), nnp("Conduit"));
		parse("Deploy Conduit now.", List.of(verb("Deploy")), nnp("Conduit"));
		assertThat(issueWords(assistant.analyze(context(),
				textEntity(project, "Use Conduit", "Deploy Conduit now.")))).containsExactly("Conduit");
	}

	/** A sentence-initial capital in the Name says nothing; the Text is still checked. */
	@Test
	void aPhraseDroppedInTheNameIsStillCheckedInTheText() {
		parse("Conduit", List.of(), nnp("Conduit"));
		parse("Deploy Conduit now.", List.of(verb("Deploy")), nnp("Conduit"));
		assertThat(issueWords(assistant.analyze(context(),
				textEntity(project, "Conduit", "Deploy Conduit now.")))).containsExactly("Conduit");
	}

	@Test
	void anActorNameInThePluralRaisesNothing() {
		String text = "Ask the Zoom Hosts.";
		NLPText[] candidate = { phrase(leaves(dt("the"), nnp("Zoom"), nnp("Hosts"))) };
		assertThat(issueWords(analyze(text, List.of(verb("Ask")), candidate)))
				.containsExactly("Zoom Hosts");
		actor("Zoom Host");
		assertThat(issueWords(analyze(text, List.of(verb("Ask")), candidate))).isEmpty();
	}

	/** "the" + "room" match "theroom", but "room" alone is not a whole word there. */
	@Test
	void aPhraseWhoseBodyIsNotAWholeWordRaisesNothing() {
		term("Room", 5L);
		AssistantResult result = analyze("Visit theroom now.", List.of(verb("Visit")),
				phrase(leaves(dt("the"), nn("room"))));
		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void aNounPhraseWithNoWordsRaisesNothing() {
		assertThat(analyze("Deploy it.", List.of(verb("Deploy")), phrase(leaves()))
				.annotationActions()).isEmpty();
	}

	@Test
	void everyCoordinationAndPossessiveShapeIsNotATerm() {
		used(5, "stream key or RTMP endpoint", "stream key & RTMP endpoint", "Conduit’s manual",
				"operators' manual");
		assertThat(issueWords(analyze("Rotate the stream key or RTMP endpoint.",
				List.of(verb("Rotate"), dt("the")), phrase(leaves(nn("stream"), nn("key"), nn("or"),
						nnp("RTMP"), nn("endpoint")))))).isEmpty();
		assertThat(issueWords(analyze("Rotate the stream key & RTMP endpoint.",
				List.of(verb("Rotate"), dt("the")), phrase(leaves(nn("stream"), nn("key"), cc("&"),
						nnp("RTMP"), nn("endpoint")))))).isEmpty();
		assertThat(issueWords(analyze("Read Conduit’s manual.", List.of(verb("Read")),
				phrase(leaves(nnp("Conduit"), nn("’s"), nn("manual")))))).isEmpty();
		assertThat(issueWords(analyze("Read the operators' manual.",
				List.of(verb("Read"), dt("the")),
				phrase(leaves(nns("operators"), pos("'"), nn("manual")))))).isEmpty();
	}

	/**
	 * A sentence-initial plural is read as a verb only when it has a verb sense: not "Boss"
	 * ("ss"), not "Rooms" (no sense), and not "Specs" when the lookup fails.
	 */
	@Test
	void aSentenceInitialPluralIsATermUnlessItHasAVerbSense() {
		used(3, "boss mode", "specs page", "rooms list");
		when(dictionaryRepository.findWord("spec", PartOfSpeech.VERB))
				.thenThrow(new IllegalStateException("dictionary unavailable"));
		assertThat(issueWords(analyze("Boss mode is on.", List.of(),
				phrase(leaves(nn("Boss"), nn("mode")))))).containsExactly("Boss mode");
		assertThat(issueWords(analyze("Specs page loads.", List.of(),
				phrase(leaves(nns("Specs"), nn("page")))))).containsExactly("Specs page");
		assertThat(issueWords(analyze("Rooms list loads.", List.of(),
				phrase(leaves(nns("Rooms"), nn("list")))))).containsExactly("Rooms list");
	}

	@Test
	void withoutADictionaryNoSentenceInitialPluralIsReadAsAVerb() {
		used(3, "alarms route");
		LexicalGlossaryTermAssistant noDictionary = new LexicalGlossaryTermAssistant(
				nlpProcessorFactory, projectRepository, null);
		String text = "Alarms route to the pager.";
		parse(text, List.of(), phrase(leaves(nns("Alarms"), nn("route"))));
		assertThat(issueWords(noDictionary.analyze(context(), textEntity(project, "", text))))
				.containsExactly("Alarms route");
	}

	@Test
	void eachSentenceOfAParagraphStartsAtItsFirstWord() {
		NLPText archive = nnp("Archive");
		NLPText conduit = nnp("Conduit");
		NLPText paragraph = mock(NLPText.class);
		when(paragraph.is(GrammaticalStructureLevel.PARAGRAPH)).thenReturn(true);
		List<NLPText> sentences = List.of(sentence(punct("\""), archive, nns("rooms")),
				sentence(conduit, nn("deploys")), sentence(punct("...")));
		when(paragraph.getChildren()).thenReturn(sentences);

		Set<NLPText> starts = LexicalGlossaryTermAssistant.sentenceStarts(paragraph);

		assertThat(starts).hasSize(2);
		assertThat(starts).anyMatch(s -> s == archive).anyMatch(s -> s == conduit);
	}

	@Test
	void anUnnamedTermIsSkipped() {
		term(null, 4L);
		GlossaryTerm room = term("Room", 5L);
		assertThat(refererTermIds(analyze("Archive the room.", List.of(verb("Archive")),
				phrase(leaves(dt("the"), nn("room")))))).containsExactly(room.getId());
	}

	/**
	 * Steps count toward a common phrase, with or without a name or text; report generators
	 * don't, and neither does a project with no entities or terms of its own.
	 */
	@Test
	void stepsCountTowardACommonPhraseButReportGeneratorsDoNot() {
		doReturn(null).when(project).getGlossaryTerms();
		doReturn(null).when(project).getProjectEntities();
		Step named = entity(Step.class, "Open the admin console", null);
		Step described = entity(Step.class, null, "Log in to the admin console.");
		ReportGenerator report = entity(ReportGenerator.class, "admin console", "admin console");
		String text = "Open the admin console as Chris.";
		List<NLPText> adminConsole = leaves(dt("the"), nn("admin"), nn("console"));

		doReturn(Set.of(named, described)).when(projectRepository).findStepsByProjectOrDomain(any());
		doReturn(new LinkedHashSet<>(List.of(report))).when(project).getProjectEntities();
		assertThat(issueWords(analyze(text, List.of(verb("Open")), phrase(adminConsole))))
				.isEmpty();

		doReturn(null).when(project).getProjectEntities();
		Step third = entity(Step.class, "Close the admin console", "");
		doReturn(Set.of(named, described, third)).when(projectRepository)
				.findStepsByProjectOrDomain(any());
		assertThat(issueWords(analyze(text, List.of(verb("Open")), phrase(adminConsole))))
				.containsExactly("admin console");
	}

	// ---- fixtures -------------------------------------------------------------------------------

	/**
	 * Analyze {@code text} (the entity's Text) whose parse is {@code before} followed by the
	 * candidates' leaves, and whose noun phrases are {@code candidates}. The first leaf of the
	 * whole parse is its sentence start.
	 */
	private AssistantResult analyze(String text, List<NLPText> before, NLPText... candidates) {
		parse(text, before, candidates);
		return assistant.analyze(context(), textEntity(project, "", text));
	}

	/**
	 * Parse {@code text} as {@code before} followed by the candidates' leaves, with the
	 * candidates as its noun phrases.
	 */
	private void parse(String text, List<NLPText> before, NLPText... candidates) {
		List<NLPText> all = new ArrayList<>(before);
		for (NLPText candidate : candidates) {
			all.addAll(candidate.getLeaves());
		}
		NLPText nlpText = mock(NLPText.class);
		when(nlpText.getLeaves()).thenReturn(all);
		when(nlpProcessorFactory.processText(text)).thenReturn(nlpText);
		when(nounPhraseFinder.process(nlpText)).thenReturn(Arrays.asList(candidates));
	}

	private static NLPText sentence(NLPText... leaves) {
		NLPText sentence = mock(NLPText.class);
		when(sentence.getLeaves()).thenReturn(List.of(leaves));
		return sentence;
	}

	/** {@code count} other entities of the project whose text mentions every phrase. */
	private void used(int count, String... phrases) {
		entities.clear();
		for (int i = 0; i < count; i++) {
			entities.add(textEntity(project, "entity " + i, String.join(". ", phrases) + "."));
		}
		when(projectRepository.findStepsByProjectOrDomain(any())).thenReturn(Set.of());
	}

	private static List<String> issueWords(AssistantResult result) {
		return result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.map(a -> (String) a.metadata().get("word")).toList();
	}

	private static List<Object> refererTermIds(AssistantResult result) {
		return result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.ADD_GLOSSARY_TERM_REFERER)
				.map(a -> a.metadata().get("glossaryTermId")).toList();
	}

	private GlossaryTerm term(String name, Long id) {
		GlossaryTerm term = mock(GlossaryTerm.class);
		when(term.getName()).thenReturn(name);
		when(term.getId()).thenReturn(id);
		terms.add(term);
		return term;
	}

	private void actor(String name) {
		Actor actor = mock(Actor.class);
		when(actor.getName()).thenReturn(name);
		actors.add(actor);
	}

	private static List<NLPText> leaves(NLPText... leaves) {
		return List.of(leaves);
	}

	private static NLPText phrase(List<NLPText> leaves) {
		if (leaves.size() == 1) {
			return leaves.get(0);
		}
		// Read the leaves' text before stubbing: calling a mock inside thenReturn() is an
		// unfinished stubbing.
		String text = String.join(" ", leaves.stream().map(NLPText::getText).toList());
		NLPText phrase = mock(NLPText.class);
		when(phrase.getLeaves()).thenReturn(leaves);
		when(phrase.getText()).thenReturn(text);
		return phrase;
	}

	private static NLPText nnp(String text) {
		return leaf(text, PartOfSpeech.NOUN, ParseTag.NNP);
	}

	private static NLPText nn(String text) {
		return leaf(text, PartOfSpeech.NOUN, ParseTag.NN);
	}

	private static NLPText nns(String text) {
		return leaf(text, PartOfSpeech.NOUN, ParseTag.NNS);
	}

	private static NLPText dt(String text) {
		return leaf(text, PartOfSpeech.DETERMINER, ParseTag.DT);
	}

	private static NLPText cc(String text) {
		return leaf(text, PartOfSpeech.CONJUNCTION, ParseTag.CC);
	}

	private static NLPText pos(String text) {
		return leaf(text, PartOfSpeech.UNKNOWN, ParseTag.POS);
	}

	private static NLPText jj(String text) {
		return leaf(text, PartOfSpeech.ADJECTIVE, ParseTag.JJ);
	}

	private static NLPText verb(String text) {
		return leaf(text, PartOfSpeech.VERB, ParseTag.VB);
	}

	private static NLPText punct(String text) {
		return leaf(text, PartOfSpeech.PUNCTUATION, ParseTag.PUNC_NON_TERMINATOR);
	}

	private static NLPText leaf(String text, PartOfSpeech partOfSpeech, ParseTag tag) {
		NLPText leaf = mock(NLPText.class);
		when(leaf.getText()).thenReturn(text);
		when(leaf.isLeaf()).thenReturn(true);
		when(leaf.getLeaves()).thenReturn(List.of(leaf));
		when(leaf.in(any(PartOfSpeech[].class)))
				.thenAnswer(inv -> Arrays.asList(inv.getArguments()).contains(partOfSpeech));
		when(leaf.in(any(ParseTag[].class)))
				.thenAnswer(inv -> Arrays.asList(inv.getArguments()).contains(tag));
		when(leaf.is(any(ParseTag.class))).thenAnswer(inv -> inv.getArgument(0) == tag);
		when(leaf.is(any(PartOfSpeech.class)))
				.thenAnswer(inv -> inv.getArgument(0) == partOfSpeech);
		return leaf;
	}

	private static <T extends TextEntity> T entity(Class<T> type, String name, String text) {
		T entity = mock(type);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn((long) (Math.abs((type + name + text).hashCode()) % 100000));
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		return entity;
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private static TextEntity textEntity(ProjectOrDomain projectOrDomain, String name,
			String text) {
		TextEntity entity = mock(TextEntity.class);
		Class<?> entityInterface = TextEntity.class;
		doReturn(entityInterface).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn((long) (Math.abs((name + text).hashCode()) % 100000));
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		doReturn(projectOrDomain).when(entity).getProjectOrDomain();
		return entity;
	}
}
