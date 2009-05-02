/*
 * $Id: SemlinkrefId.java,v 1.2 2009/01/03 10:24:32 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author ron
 */
@Embeddable
public class SemlinkrefId implements Comparable<SemlinkrefId>, Serializable {
	static final long serialVersionUID = 0;

	private Long synset1id;
	private Long synset2id;
	private Long linkid;
	private Integer distance;

	public SemlinkrefId() {
	}

	public SemlinkrefId(Long synset1id, Long synset2id, Long linkid, Integer distance) {
		this.synset1id = synset1id;
		this.synset2id = synset2id;
		this.linkid = linkid;
		this.distance = distance;
	}

	@Column(name = "synset1id", nullable = false)
	public Long getSynset1id() {
		return this.synset1id;
	}

	public void setSynset1id(Long synset1id) {
		this.synset1id = synset1id;
	}

	@Column(name = "synset2id", nullable = false)
	public Long getSynset2id() {
		return this.synset2id;
	}

	public void setSynset2id(Long synset2id) {
		this.synset2id = synset2id;
	}

	@Column(name = "linkid", nullable = false)
	public Long getLinkid() {
		return this.linkid;
	}

	public void setLinkid(Long linkid) {
		this.linkid = linkid;
	}

	@Column(name = "distance", nullable = false)
	public Integer getDistance() {
		return distance;
	}

	protected void setDistance(Integer distance) {
		this.distance = distance;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other)) {
			return true;
		}
		if ((other == null)) {
			return false;
		}
		if (!(other instanceof SemlinkrefId)) {
			return false;
		}
		SemlinkrefId castOther = (SemlinkrefId) other;

		return (this.getSynset1id() == castOther.getSynset1id())
				&& (this.getSynset2id() == castOther.getSynset2id())
				&& (this.getLinkid() == castOther.getLinkid())
				&& (this.getDistance() == castOther.getDistance());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			final int prime = 31;
			int result = 1;
			result = prime * result + getSynset1id().hashCode();
			result = prime * result + getSynset2id().hashCode();
			result = prime * result + getLinkid().hashCode();
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public int compareTo(SemlinkrefId o) {
		if (this.getSynset1id() != o.getSynset1id()) {
			return (this.getSynset1id().intValue() - o.getSynset1id().intValue());
		}
		if (this.getSynset2id() != o.getSynset2id()) {
			return (this.getSynset2id().intValue() - o.getSynset2id().intValue());
		}
		if (this.getLinkid() != o.getLinkid()) {
			return (this.getLinkid().intValue() - o.getLinkid().intValue());
		}
		if (this.getDistance() != o.getDistance()) {
			return (this.getDistance() - o.getDistance());
		}
		return 0;
	}
}
