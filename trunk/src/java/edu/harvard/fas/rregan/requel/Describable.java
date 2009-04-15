package edu.harvard.fas.rregan.requel;

import java.util.Comparator;

/**
 * @author ron
 */
public interface Describable {

	/**
	 * Get the des
	 * 
	 * @return The description of the describable object.
	 */
	public String getDescription();

	/**
	 * Compare the objects that contain goals by the description.
	 */
	public static final Comparator<Describable> COMPARATOR = new DescribableComparator();

	/**
	 * A Comparator for collections of goal containers.
	 */
	public static class DescribableComparator implements Comparator<Describable> {
		@Override
		public int compare(Describable o1, Describable o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
