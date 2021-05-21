package labkit_cluster.command;

import java.util.AbstractList;

/**
 * List that contains a range of integers.
 */
public class IntRange extends AbstractList<Integer> {

	private final int start;

	private final int size;

	public IntRange(int start, int stopExclusive) {
		this.start = start;
		this.size = Math.max(0, stopExclusive - start);
	}

	@Override
	public Integer get(int i) {
		return start + i;
	}

	@Override
	public int size() {
		return size;
	}
}
