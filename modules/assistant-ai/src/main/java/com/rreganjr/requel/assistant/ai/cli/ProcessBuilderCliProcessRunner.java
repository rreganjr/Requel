/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.assistant.ai.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@link CliProcessRunner} over {@link ProcessBuilder}. Load-bearing details (#259):
 * <ul>
 * <li>the argv list goes to {@code ProcessBuilder} as-is — no shell, no interpolation;</li>
 * <li>the child's environment is <em>replaced</em> by the invocation's, not merged into the
 * JVM's;</li>
 * <li>stdin is written on its own thread and closed, so a large prompt cannot deadlock against a
 * child that is already writing;</li>
 * <li>stdout and stderr are each drained on their own thread up to their cap and then read and
 * discarded, so the child never blocks on a full pipe;</li>
 * <li>on timeout the process and its descendants are destroyed forcibly.</li>
 * </ul>
 * Java 17: plain daemon threads, not virtual threads.
 */
public final class ProcessBuilderCliProcessRunner implements CliProcessRunner {

	/** How long to wait for the pipes to drain once the process has ended or been killed. */
	private static final long DRAIN_WAIT_MS = 5_000;

	@Override
	public CliProcessResult run(CliInvocation invocation) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(invocation.argv());
		builder.environment().clear();
		builder.environment().putAll(invocation.environment());
		builder.directory(invocation.workingDirectory().toFile());

		long started = System.nanoTime();
		Process process = builder.start();

		CappedSink stdout = new CappedSink(process.getInputStream(), invocation.maxOutputBytes());
		CappedSink stderr = new CappedSink(process.getErrorStream(), invocation.maxErrorBytes());
		Thread outThread = daemon("requel-ai-cli-stdout", stdout);
		Thread errThread = daemon("requel-ai-cli-stderr", stderr);
		Thread inThread = daemon("requel-ai-cli-stdin",
				() -> writeAndClose(process.getOutputStream(), invocation.stdin()));
		outThread.start();
		errThread.start();
		inThread.start();

		boolean timedOut = false;
		try {
			if (!process.waitFor(invocation.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
				timedOut = true;
				kill(process);
				process.waitFor(DRAIN_WAIT_MS, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			kill(process);
			throw e;
		}
		outThread.join(DRAIN_WAIT_MS);
		errThread.join(DRAIN_WAIT_MS);
		inThread.join(DRAIN_WAIT_MS);

		Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
		int exitCode = timedOut || process.isAlive() ? -1 : process.exitValue();
		return new CliProcessResult(exitCode, stdout.bytes(), stdout.truncated(),
				new String(stderr.bytes(), StandardCharsets.UTF_8), timedOut, elapsed);
	}

	private static void kill(Process process) {
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
	}

	private static Thread daemon(String name, Runnable body) {
		Thread thread = new Thread(body, name);
		thread.setDaemon(true);
		return thread;
	}

	private static void writeAndClose(OutputStream stdin, byte[] bytes) {
		try (OutputStream out = stdin) {
			out.write(bytes);
			out.flush();
		} catch (IOException e) {
			// The child exited (or closed stdin) before reading everything: a broken pipe here is
			// reported through its exit code and output, not as a write failure.
		}
	}

	/** Reads a stream to EOF, keeping at most {@code cap} bytes. */
	private static final class CappedSink implements Runnable {
		private final InputStream in;
		private final int cap;
		private final ByteArrayOutputStream kept = new ByteArrayOutputStream();
		private volatile boolean truncated;

		CappedSink(InputStream in, int cap) {
			this.in = in;
			this.cap = cap;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[8192];
			try (InputStream stream = in) {
				int read;
				while ((read = stream.read(buffer)) != -1) {
					int room = cap - kept.size();
					if (room > 0) {
						kept.write(buffer, 0, Math.min(room, read));
					}
					if (read > room) {
						truncated = true;
					}
				}
			} catch (IOException e) {
				// Stream closed under us (process killed): keep what was read.
			}
		}

		synchronized byte[] bytes() {
			return kept.toByteArray();
		}

		boolean truncated() {
			return truncated;
		}
	}
}
