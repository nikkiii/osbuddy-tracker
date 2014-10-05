package org.nikkii.rs07.gallery;

import java.util.Comparator;

/**
 * @author Nikki
 */
public class GalleryEntrySorter implements Comparator<GalleryEntry> {
	@Override
	public int compare(GalleryEntry o1, GalleryEntry o2) {
		if (o1.getTime() < o2.getTime()) {
			return -1;
		} else if (o1.getTime() > o2.getTime()) {
			return 1;
		}
		return 0;
	}
}
