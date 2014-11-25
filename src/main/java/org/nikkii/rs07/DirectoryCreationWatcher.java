package org.nikkii.rs07;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * @author Nikki
 */
public class DirectoryCreationWatcher implements Runnable {
	private final ProgressTracker tracker;
	private final File directory;

	public DirectoryCreationWatcher(ProgressTracker tracker, File directory) {
		this.tracker = tracker;
		this.directory = directory;
	}

	@Override
	public void run() {
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();

			Path path = directory.toPath();

			path.register(watcher, ENTRY_CREATE);

			for (; ; ) {
				// wait for key to be signaled
				WatchKey key;

				try {
					key = watcher.take();
				} catch (InterruptedException e) {
					break;
				}

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();

					if (kind == OVERFLOW) {
						continue;
					}

					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path filename = ev.context();

					File created = new File(directory, filename.toString());

					if (created.isDirectory()) {
						tracker.trackScreenshots(created);
					}
				}

				boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
