package com.ojcoleman.europa.core;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.configurable.Parameter;
import com.thoughtworks.xstream.XStream;

/**
 * Utility class for performing parallel iteration over generic Collections or anything that implements the Iterable
 * interface. Code adapted from http://stackoverflow.com/questions/4010185/parallel-for-for-java#4010275
 */
public class Parallel extends ComponentBase {
	private final static Logger logger = LoggerFactory.getLogger(Evolver.class);

	@Parameter(description = "The number of threads to use for parallel operations. If set to 0 or not set then the number of CPU cores is used.", minimumValue = "0", defaultValue = "0")
	protected int threadCount;

	// Mark as transient so XStream does not store it. See readResolve().
	protected transient ExecutorService executor;
	
	
	public Parallel(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		if (threadCount <= 0) {
			threadCount = Runtime.getRuntime().availableProcessors();
		}
		
		executor = Executors.newFixedThreadPool(threadCount, new DaemonThreadFactory(Parallel.class.getName()));
		
		this.getParentComponent(Run.class).addEventListener(new Observer() {
			@Override
			public void eventOccurred(Observable observed, Object event, Object state) {
				if (event == Run.Event.SnapshotBegin) {
					if (executor instanceof ThreadPoolExecutor && ((ThreadPoolExecutor) executor).getActiveCount() > 0) {
						logger.warn("Snapshotting while tasks are still executing. Resuming from this snapshot may produce strange results.");
					}
				}
			}
		});
	}
	
	private Object readResolve() {
		// Create new thread pool upon resuming from a save file.
		executor = Executors.newFixedThreadPool(threadCount, new DaemonThreadFactory(Parallel.class.getName()));
		return this;
	}
	
	
	/**
	 * Perform the given {@link Parallel.Operation} on the given elements. Returns when all elements have been
	 * processed.
	 * 
	 * @param elements The Collection of elements to apply the operation to.
	 * @param operation The operation to apply to each element.
	 */
	public <T> void foreach(final Collection<T> elements, final Operation<T> operation) {
		submitAndWait(elements, operation, elements.size());
	}

	/**
	 * Perform the given {@link Parallel.Operation} on the given elements. Returns when all elements have been
	 * processed.
	 * 
	 * @param elements An Iterator over elements to apply the operation to.
	 * @param operation The operation to apply to each element.
	 */
	public <T> void foreach(final Iterable<T> elements, final Operation<T> operation) {
		submitAndWait(elements, operation, 8);
	}

	private <T> void submitAndWait(final Iterable<T> elements, final Operation<T> operation, int size) {
		if (executor.isShutdown()) {
			throw new IllegalStateException("Executor service for Parallel has been shutdown, cannot submit new tasks.");
		}
		
		try {
			List<Future<Void>> futures = executor.invokeAll(createCallables(elements, operation, size));
			assert (futures.size() == size) : futures.size() + " != " + size;

			// Wait for all elements to be processed.
			for (Future<?> f : futures) {
				f.get();
			}
		} catch (Exception e) {
			throw new RuntimeException("Error executing parallel operation.", e);
		}
	}

	private <T> Collection<Callable<Void>> createCallables(final Iterable<T> elements, final Operation<T> operation, int size) {
		List<Callable<Void>> callables = new ArrayList<Callable<Void>>(size);
		for (final T elem : elements) {
			callables.add(new Callable<Void>() {
				@Override
				public Void call() {
					operation.perform(elem);
					return null;
				}
			});
		}

		return callables;
	}

	/**
	 * An operation to be performed on a single element. The perform method will be invoked for each element in the
	 * given collection, with the element passed as the parameter.
	 */
	public static interface Operation<T> {
		public void perform(T parameter);
	}

	/**
	 * ThreadFactory to create daemon threads. Uses the factory given by {@link Executors#defaultThreadFactory()} to
	 * create the threads, then makes them daemons.
	 */
	public static class DaemonThreadFactory implements ThreadFactory {
		final String name;
		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger(1);

		public DaemonThreadFactory() {
			this("DaemonThreadFactory");
		}

		public DaemonThreadFactory(String name) {
			this(name, new ThreadGroup(name));
		}

		public DaemonThreadFactory(String name, ThreadGroup group) {
			this.name = name;
			this.group = group;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, name + "-" + threadNumber.getAndIncrement());
			t.setDaemon(true);
			return t;
		}
	}

	/**
	 * Shutdown all threads.
	 */
	public void stop() {
		executor.shutdown();
	}
}
