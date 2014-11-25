package org.nikkii.rs07;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * @author Nikki
 */
public class ScreenshotWatcher implements Runnable {
	private final ProgressTracker tracker;
	private final File directory;

	private final List<File> createdFiles = new ArrayList<>();

	public ScreenshotWatcher(ProgressTracker tracker, File directory) {
		this.tracker = tracker;
		this.directory = directory;
	}

	@Override
	public void run() {
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();

			Path path = directory.toPath();

			path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);

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

					if (kind == ENTRY_CREATE) {
						createdFiles.add(created);
					} else if (kind == ENTRY_MODIFY) {
						// Only track files modified after creation (usually a write to file with the image data)
						if (createdFiles.contains(created)) {
							tracker.screenshotCreated(directory, created);
							createdFiles.remove(created);
						}
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
