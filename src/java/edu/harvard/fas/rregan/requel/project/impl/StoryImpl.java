package edu.harvard.fas.rregan.requel.project.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;
import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.StoryType;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * A story describes an interaction with the system as prose.
 * 
 * @author ron
 */
@Entity
@Table(name = "stories", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "story", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "story", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class StoryImpl extends AbstractTextEntity implements Story {
	static final long serialVersionUID = 0;

	private Set<StoryContainer> referers = new TreeSet<StoryContainer>(StoryContainer.COMPARATOR);
	private StoryType storyType;
	private Set<Goal> goals = new TreeSet<Goal>();
	private Set<Actor> actors = new TreeSet<Actor>();

	/**
	 * @param projectOrDomain
	 * @param name
	 * @param createdBy
	 * @param text
	 * @param storyType
	 */
	public StoryImpl(ProjectOrDomain projectOrDomain, User createdBy, String name, String text,
			StoryType storyType) {
		super(projectOrDomain, createdBy, name, text);
		// add to collection last so that sorting in the collection by entity
		// properties has access to all the properties.
		projectOrDomain.getStories().add(this);
		setStoryType(storyType);
	}

	protected StoryImpl() {
		// for hibernate
	}

	@Override
	@Column(nullable = false, unique = false)
	@NotEmpty(message = "a unique name is required.")
	@XmlElement(name = "name", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getName() {
		return super.getName();
	}

	// hack for JAXB to set the name, for some reason it won't use the inherited
	// method.
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	public String getXmlId() {
		return "STRY_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Story: " + getName();
	}

	@XmlTransient
	@ManyToAny(fetch = FetchType.LAZY, metaColumn = @Column(name = "storycontainer_type", length = 255, nullable = false))
	@AnyMetaDef(idType = "long", metaType = "string", metaValues = {
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Project", targetEntity = ProjectImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Actor", targetEntity = ActorImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Goal", targetEntity = GoalImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.UseCase", targetEntity = UseCaseImpl.class) })
	@JoinTable(name = "story_storycontainers", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = {
			@JoinColumn(name = "storycontainer_type"), @JoinColumn(name = "storycontainer_id") })
	@Sort(type = SortType.COMPARATOR, comparator = StoryContainer.StoryContainerComparator.class)
	public Set<StoryContainer> getReferers() {
		return referers;
	}

	protected void setReferers(Set<StoryContainer> referers) {
		this.referers = referers;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@XmlAttribute(name = "storyType")
	@XmlJavaTypeAdapter(StoryTypeAdapter.class)
	@Column(nullable = false)
	@NotNull(message = "a type is required.")
	public StoryType getStoryType() {
		return storyType;
	}

	@Override
	public void setStoryType(StoryType storyType) {
		this.storyType = storyType;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.GoalContainer#getGoals()
	 */
	@Override
	@XmlElementWrapper(name = "goals", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "story_goals", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = { @JoinColumn(name = "goal_id") })
	@Sort(type = SortType.NATURAL)
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	@XmlElementWrapper(name = "actors", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "actorRef", type = ActorImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = ActorImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "story_actors", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = { @JoinColumn(name = "actor_id") })
	@Sort(type = SortType.NATURAL)
	public Set<Actor> getActors() {
		return actors;
	}

	protected void setActors(Set<Actor> actors) {
		this.actors = actors;
	}

	@Override
	public int compareTo(Story o) {
		return getName().compareToIgnoreCase(o.getName());
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and to patchup
	 * existing persistent objects for the objects that are attached directly to
	 * this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final UserRepository userRepository, User defaultCreatedByUser) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					// update the references to goals
					for (Goal goal : getGoals()) {
						goal.getReferers().add(StoryImpl.this);
					}
					for (Actor actor : getActors()) {
						actor.getReferers().add(StoryImpl.this);
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException2(e);
				}
			}
		});
	}

	/**
	 * This class is used by JAXB to convert the StoryType of a Story into a
	 * string for an attribute in the xml file and the reverse when
	 * unmartialling.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class StoryTypeAdapter extends XmlAdapter<String, StoryType> {

		@Override
		public StoryType unmarshal(String typeString) throws Exception {
			return StoryType.valueOf(typeString);
		}

		@Override
		public String marshal(StoryType type) throws Exception {
			return type.toString();
		}
	}
}
