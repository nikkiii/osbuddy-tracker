package org.nikkii.rs07;

import org.nikkii.rs07.event.OSBuddyEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A thread which submits progress events/waits for screenshots to be valid.
 *
 * @author Nikki
 */
public class UpdateQueueWorker implements Runnable {
	/**
	 * The tracker object.
	 */
	private final JsonProgressTracker tracker;

	/**
	 * The Linked Queue for events.
	 */
	private final Queue<OSBuddyEvent> queue = new LinkedList<>();

	public UpdateQueueWorker(JsonProgressTracker tracker) {
		this.tracker = tracker;
	}

	/**
	 * Queue an event for submission.
	 *
	 * @param event The event to submit.
	 */
	public void queue(OSBuddyEvent event) {
		synchronized(queue) {
			queue.add(event);
		}
	}

	@Override
	public void run() {
		while (true) {
			synchronized(queue) {
				if (!queue.isEmpty()) {
					for (Iterator<OSBuddyEvent> it$ = queue.iterator(); it$.hasNext();) {
						OSBuddyEvent event = it$.next();

						if (!event.hasScreenshot()) {
							try {
								event.setScreenshot(event.getEntry().getScreenshot());
							} catch (Exception e) {
								// Unable to load, let it go and add again.
								System.out.println("Unable to get screenshot from " + event.getEntry().getAbsolutePath());
								e.printStackTrace();
							}
						}

						if (event.hasScreenshot()) {
							it$.remove();
							tracker.submitProgress(event);
						}
					}
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
