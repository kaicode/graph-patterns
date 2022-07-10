package io.kaicode.graphpattern.domain;

import java.util.Collection;
import java.util.List;

public class PatternSets {

	private Collection<Pattern> groupAPatterns;
	private List<Pattern> groupBPatterns;

	public PatternSets(Collection<Pattern> groupAPatterns, List<Pattern> groupBPatterns) {
		this.groupAPatterns = groupAPatterns;
		this.groupBPatterns = groupBPatterns;
	}

	public Collection<Pattern> getGroupAPatterns() {
		return groupAPatterns;
	}

	public List<Pattern> getGroupBPatterns() {
		return groupBPatterns;
	}

	public void setGroupAPatterns(Collection<Pattern> groupAPatterns) {
		this.groupAPatterns = groupAPatterns;
	}

	public void setGroupBPatterns(List<Pattern> groupBPatterns) {
		this.groupBPatterns = groupBPatterns;
	}
}
