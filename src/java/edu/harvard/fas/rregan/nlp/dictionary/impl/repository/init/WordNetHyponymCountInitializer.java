/*
 * $Id: WordNetHyponymCountInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.command.BatchCommand;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.dictionary.Dictionary;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Linkdef;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetCommand;

/**
 * Count all the hyponyms that are subsumed by a synset.
 * 
 * @author ron
 */
@Component("wordNetHyponymCountInitializer")
@Scope("prototype")
public class WordNetHyponymCountInitializer extends AbstractSystemInitializer {

	private final DictionaryRepository dictionaryRepository;
	private final CommandHandler commandHandler;
	private final DictionaryCommandFactory dictionaryCommandFactory;
	private final Map<Long, SynsetNode> semanticGraph = new HashMap<Long, SynsetNode>();
	private final Map<Long, Linkdef> semanticLinkTypes = new HashMap<Long, Linkdef>();

	/**
	 * @param dictionaryRepository
	 * @param commandHandler
	 * @param dictionaryCommandFactory
	 */
	@Autowired
	public WordNetHyponymCountInitializer(DictionaryRepository dictionaryRepository,
			CommandHandler commandHandler, DictionaryCommandFactory dictionaryCommandFactory) {
		super(20);
		this.dictionaryRepository = dictionaryRepository;
		this.commandHandler = commandHandler;
		this.dictionaryCommandFactory = dictionaryCommandFactory;
	}

	@Override
	public void initialize() {
		try {
			// TODO: this disables the initalizer and shouldn't be hard coded
			if (true) {
				return;
			}
			if (dictionaryRepository.buildSynsetLinkPathsAndCounts()) {
				// load the link types in a map for quick references
				for (Linkdef linkType : dictionaryRepository.findLinkdefs()) {
					semanticLinkTypes.put(linkType.getId(), linkType);
				}

				// getting the dictionary avoids the overhead of wrapping all
				// the synsets in proxy on return from the repository.
				Dictionary dictionary = dictionaryRepository.getDictionary();
				Collection<Synset> synsets = dictionary.getSynsets();
				for (Object[] semanticLink : dictionaryRepository.findSemanticLinks()) {
					// log.info(hypernymLink);
					Long fromSynset = (Long) semanticLink[0];
					Long toSynset = (Long) semanticLink[1];
					Long linkType = (Long) semanticLink[2];
					Integer distance = (Integer) semanticLink[3];

					SynsetNode fromSynsetNode = semanticGraph.get(fromSynset);
					if (fromSynsetNode == null) {
						fromSynsetNode = new SynsetNode(fromSynset);
						semanticGraph.put(fromSynset, fromSynsetNode);
					}
					Set<SemlinkPath> semlinkPaths = fromSynsetNode.semanticPaths.get(linkType);
					if (semlinkPaths == null) {
						semlinkPaths = new HashSet<SemlinkPath>();
						fromSynsetNode.semanticPaths.put(linkType, semlinkPaths);
					}
					semlinkPaths.add(new SemlinkPath(linkType, toSynset, distance));
					// build a node for the target too, otherwise root nodes
					// in the graph won't have a SynsetNode
					SynsetNode targetNode = semanticGraph.get(toSynset);
					if (targetNode == null) {
						semanticGraph.put(toSynset, new SynsetNode(toSynset));
					}

				}

				int count = 0;
				for (Long synset : semanticGraph.keySet()) {
					count++;
					if (count % 1000 == 0) {
						log.info("walking " + count);
					}
					walkSynsetRelations(synset);
				}

				count = 0;
				BatchCommand batchCommand = dictionaryCommandFactory.newBatchCommand();
				for (Synset synset : synsets) {
					count++;
					if (count % 100 == 0) {
						commandHandler.execute(batchCommand);
						batchCommand = dictionaryCommandFactory.newBatchCommand();
						log.info("generating updates " + count);
					}
					SynsetNode node = semanticGraph.get(synset.getId().intValue());
					EditSynsetCommand command = dictionaryCommandFactory.newEditSynsetCommand();
					batchCommand.addCommand(command);
					command.setSynset(synset);
					if (node != null) {
						Map<Long, Integer> subsumerCounts = new HashMap<Long, Integer>();
						for (Long linkType : node.subsumerCounts.keySet()) {
							subsumerCounts.put(linkType, node.subsumerCounts.get(linkType).count);
						}
						command.setSubsumerCounts(subsumerCounts);

						if (node.semanticPaths != null) {
							for (Long linkType : node.semanticPaths.keySet()) {
								for (SemlinkPath spd : node.semanticPaths.get(linkType)) {
									EditSemlinkRefCommand editSemlinkRefCommand = dictionaryCommandFactory
											.newEditSemlinkRefCommand();
									editSemlinkRefCommand.setFromSynset(node.synsetid);
									editSemlinkRefCommand.setToSynset(spd.synsetid);
									editSemlinkRefCommand.setLinkDef(spd.linkdefid);
									editSemlinkRefCommand.setDistance(spd.distance);
									batchCommand.addCommand(editSemlinkRefCommand);
								}
							}
						}

					} else {
						command.setSubsumerCounts(null);
					}
				}
			}
		} catch (Exception e) {
			log.error("failed to initialize wordnet hyponym counts: " + e, e);
		} catch (Error e) {
			log.error("failed to initialize wordnet hyponym counts: " + e, e);
		}
	}

	/**
	 * Walk all the hypernym paths from the startSynset to the root of the tree
	 * that contains it. Increment all the hyponym counts of the ancestors of
	 * the start synset and add the ancestor plus distance to the set of
	 * ancestor paths. Make sure the start synset is only counted once for each
	 * ancestor even though the ancestor may be contained in multiple paths.
	 * 
	 * @param startSynset
	 */
	private void walkSynsetRelations(Long startSynset) {
		SynsetNode startSynsetNode = semanticGraph.get(startSynset);
		for (Long linkType : startSynsetNode.semanticPaths.keySet()) {
			// only expand the paths of link types that are transative
			if (semanticLinkTypes.get(linkType).getRecurses() && (linkType != 2)) {
				walkSynsetRelations(startSynset, linkType);
			}
		}
	}

	private void walkSynsetRelations(Long startSynset, Long linkType) {
		int tick = 0;
		SynsetNode startSynsetNode = semanticGraph.get(startSynset);
		Set<SemlinkPath> semanticPaths = startSynsetNode.semanticPaths.get(linkType);
		Set<SemlinkPath> synsetsVisited = new HashSet<SemlinkPath>();
		Deque<SemlinkPath> pathsToVisit = new LinkedList<SemlinkPath>();
		// add a path to itself, i.e. a synset subsumes itself, and it is
		// the LCS between itself and all its subsumies
		pathsToVisit.push(new SemlinkPath(linkType, startSynset, 0));
		while (!pathsToVisit.isEmpty()) {
			SemlinkPath currentPath = pathsToVisit.pop();
			if (!synsetsVisited.contains(currentPath)) {
				tick++;
				synsetsVisited.add(currentPath);
				semanticPaths.add(currentPath);
				SynsetNode synsetNode = semanticGraph.get(currentPath.synsetid);
				// paths of the current node being visited
				Set<SemlinkPath> paths = synsetNode.semanticPaths.get(linkType);
				if (paths != null) {
					for (SemlinkPath path : paths) {
						// only look at the immediate links of the current node.
						if (path.distance == 1) {
							Long linkTarget = path.synsetid;
							SynsetNode linkTargetNode = semanticGraph.get(linkTarget);
							Counter subsumerCounter = linkTargetNode.subsumerCounts.get(linkType);
							if (subsumerCounter == null) {
								subsumerCounter = new Counter();
								linkTargetNode.subsumerCounts.put(linkType, subsumerCounter);
							}
							subsumerCounter.count++;
							pathsToVisit.push(new SemlinkPath(linkType, linkTarget,
									currentPath.distance + 1));
						}
					}
				}
			}
		}
	}

	public static class SynsetNode {
		public Long synsetid;
		public final Map<Long, Set<SemlinkPath>> semanticPaths = new HashMap<Long, Set<SemlinkPath>>();
		public final Map<Long, Counter> subsumerCounts = new HashMap<Long, Counter>();

		public SynsetNode(Long synsetid) {
			this.synsetid = synsetid;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof SynsetNode) {
				return (((SynsetNode) o).synsetid == this.synsetid);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return synsetid.hashCode();
		}

		@Override
		public String toString() {
			return "SynsetNode[" + synsetid + "]";
		}
	}

	public static class SemlinkPath {
		public Long linkdefid;
		public Long synsetid;
		public Integer distance;

		public SemlinkPath(Long linkdefid, Long synsetid, Integer distance) {
			this.linkdefid = linkdefid;
			this.synsetid = synsetid;
			this.distance = distance;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof SemlinkPath) {
				SemlinkPath other = (SemlinkPath) o;
				if (!linkdefid.equals(other.linkdefid)) {
					return false;
				}
				return (synsetid.equals(other.synsetid));
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((synsetid == null) ? 0 : synsetid.hashCode());
			result = prime * result + ((linkdefid == null) ? 0 : linkdefid.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "SemlinkPath[" + synsetid + ", " + linkdefid + ", " + distance + "]";
		}
	}

	/**
	 * holds a count that can be incremented.
	 * 
	 * @author ron
	 */
	public static class Counter {
		public int count = 0;
	}
}
