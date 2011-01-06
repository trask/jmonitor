package org.jmonitor.collector.impl.file;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.FastDateFormat;

// benchmarking different approaches to making DateFormat and DecimalFormat thread safe
public class FormatPerformanceMain {

	private static final int CONTENTION_DURATION_NANOS = 2000;
	private static final double CONTENTION_PERCENTAGE = 0.0000; // 0.01%

	private static final int GARBAGE_CREATED_PER_MILLISECOND = 1000000; // 1mb

	private static final int WARMUP_ITERATIONS = 100000;
	private static final int BENCHMARK_ITERATIONS = 100000;

	private static final Date DATE_VALUE = new Date();
	private static final Format DATE_FORMAT = DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM, DateFormat.FULL);
	private static final Format FAST_DATE_FORMAT = FastDateFormat
			.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);
	private static final Format SAFE_DATE_FORMAT = ThreadSafeFormatFactory
			.newFormatExpectingModerateToHighContention(DATE_FORMAT);

	private static final double DECIMAL_VALUE = 123.123;
	private static final Format DECIMAL_FORMAT = new DecimalFormat("#,##0.000");
	private static final Format SAFE_DECIMAL_FORMAT = ThreadSafeFormatFactory
			.newFormatExpectingModerateToHighContention(DECIMAL_FORMAT);

	public static void main(String[] args) {

		if (CONTENTION_PERCENTAGE > 0) {
			// try to simulate 1% contention without stealing cpu cycles
			Thread contentionThread = new Thread(new Runnable() {
				public void run() {
					while (true) {
						synchronized (DATE_FORMAT) {
							synchronized (DECIMAL_FORMAT) {
								// sleep for 2 microsecond (the duration of a
								// typical format operation)
								try {
									Thread.sleep(0, CONTENTION_DURATION_NANOS);
								} catch (InterruptedException e) {
									throw new IllegalStateException(e);
								}
							}
						}
						// sleep for long enough to make the percentage work out
						try {
							long nanos = (long) (CONTENTION_DURATION_NANOS / CONTENTION_PERCENTAGE)
									- CONTENTION_DURATION_NANOS;
							Thread.sleep(nanos / 1000000, (int) nanos % 1000000);
						} catch (InterruptedException e) {
							throw new IllegalStateException(e);
						}
					}
				}
			});
			// make it a daemon thread so that it doesn't prevent test from
			// terminating at end of main()
			contentionThread.setDaemon(true);
			contentionThread.start();
		}

		// try to simulate garbage collection's impact on
		// strategies that rely on weak references
		Thread garbageCreationThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					// create garbage in smallish (1kb) chunks to avoid
					// fragmentation
					for (int i = 0; i < GARBAGE_CREATED_PER_MILLISECOND / 1000; i++) {
						// not sure why, but compiler complains if you don't
						// assign new byte[1000] to something
						@SuppressWarnings("unused")
						byte[] b = new byte[1000];
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		});
		// make it a daemon thread so that it doesn't prevent test from
		// terminating at end of main()
		garbageCreationThread.setDaemon(true);
		garbageCreationThread.start();

		// benchmark DateFormat
		benchmarkOperation(new DateFormatOperationWithAlloc());
		// these are so slow (3x as slow as basic alloc above) that they are not
		// interesting so commenting out for now
		benchmarkOperation(new DateFormatOperationWithCloneNoSync());
		benchmarkOperation(new DateFormatOperationWithCloneWithSync());
		benchmarkOperation(new DateFormatOperationNoAllocNoSync());
		benchmarkOperation(new DateFormatNoAllocWithSync());
		benchmarkOperation(new FastDateFormatOperation());
		benchmarkOperation(new DateFormatOperationWithSafeFormat());

		System.out.println();

		// benchmark DecimalFormat
		benchmarkOperation(new DecimalFormatOperationWithAlloc());
		// these are so slow (3x as slow as basic alloc above) that they are not
		// interesting so commenting out for now
		// benchmarkOperation(new DecimalFormatOperationWithCloneNoSync());
		// benchmarkOperation(new DecimalFormatOperationWithCloneWithSync());
		benchmarkOperation(new DecimalFormatOperationNoAllocNoSync());
		benchmarkOperation(new DecimalFormatOperationNoAllocWithSync());
		benchmarkOperation(new DecimalFormatOperationWithSafeFormat());
	}

	private static void benchmarkOperation(final Operation operation) {

		// warm-up the hotspot compiler
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			operation.execute();
		}

		// benchmark in separate thread
		Thread thread1 = new Thread(new Runnable() {
			public void run() {
				runBenchmark(operation);
			}
		});
		thread1.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		// benchmark in second thread
		// this doesn't seem to make any difference (except make the test run
		// longer) so commenting it out for now
		// Thread thread2 = new Thread(new Runnable() {
		// public void run() {
		// runBenchmark(operation);
		// }
		// });
		// thread2.start();
		// try {
		// thread2.join();
		// } catch (InterruptedException e) {
		// throw new IllegalStateException(e);
		// }
	}

	private static void runBenchmark(final Operation operation) {

		// start fresh
		System.gc();

		long gcTimeMillis = getAccumulatedGCTimeMillis();
		long gcCount = getAccumulatedGCCount();

		// start timer
		long time = System.nanoTime();

		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			operation.execute();
		}

		// end timer
		time = System.nanoTime() - time;

		gcTimeMillis = getAccumulatedGCTimeMillis() - gcTimeMillis;
		gcCount = getAccumulatedGCCount() - gcCount;

		System.out.println(operation.getName() + ": " + BENCHMARK_ITERATIONS
				+ " operations in " + (time / 1000000.0) + " milliseconds ("
				+ gcCount + " GCs for a total of " + gcTimeMillis
				+ " milliseconds)");
	}

	private static long getAccumulatedGCTimeMillis() {
		long totalCollectionTimeMillis = 0;
		List<GarbageCollectorMXBean> mxbeans = ManagementFactory
				.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean mxbean : mxbeans) {
			totalCollectionTimeMillis += mxbean.getCollectionTime();
		}
		return totalCollectionTimeMillis;
	}

	private static long getAccumulatedGCCount() {
		long totalCollectionCount = 0;
		List<GarbageCollectorMXBean> mxbeans = ManagementFactory
				.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean mxbean : mxbeans) {
			totalCollectionCount += mxbean.getCollectionTime();
		}
		return totalCollectionCount;
	}

	private static interface Operation {

		String getName();

		void execute();
	}

	private static final class DateFormatOperationWithAlloc implements
			Operation {

		public String getName() {
			return "date format operation with alloc";
		}

		public void execute() {
			Format format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.FULL);
			format.format(DATE_VALUE);
		}
	}

	// NOT THREAD SAFE (at least not according to API docs, Format.clone()
	// should be synchronized, see DateFormatOperationWithCloneWithSync)
	private static final class DateFormatOperationWithCloneNoSync implements
			Operation {

		public String getName() {
			return "date format operation with clone no sync";
		}

		public void execute() {
			Format format = (Format) DATE_FORMAT.clone();
			format.format(DATE_VALUE);
		}
	}

	private static final class DateFormatOperationWithCloneWithSync implements
			Operation {

		public String getName() {
			return "date format operation with clone with sync";
		}

		public void execute() {
			Format format;
			synchronized (DATE_FORMAT) {
				format = (Format) DATE_FORMAT.clone();
			}
			format.format(DATE_VALUE);
		}
	}

	// NOT THREAD SAFE
	private static final class DateFormatOperationNoAllocNoSync implements
			Operation {

		public String getName() {
			return "date format operation no alloc no sync";
		}

		public void execute() {
			DATE_FORMAT.format(DATE_VALUE);
		}
	}

	private static final class DateFormatNoAllocWithSync implements Operation {

		public String getName() {
			return "date format operation no alloc with sync";
		}

		public void execute() {
			synchronized (DATE_FORMAT) {
				DATE_FORMAT.format(DATE_VALUE);
			}
		}
	}

	private static final class FastDateFormatOperation implements Operation {

		public String getName() {
			return "fast date format operation";
		}

		public void execute() {
			synchronized (FAST_DATE_FORMAT) {
				FAST_DATE_FORMAT.format(DATE_VALUE);
			}
		}
	}

	private static final class DateFormatOperationWithSafeFormat implements
			Operation {

		public String getName() {
			return "safe date format operation";
		}

		public void execute() {
			SAFE_DATE_FORMAT.format(DATE_VALUE);
		}
	}

	private static final class DecimalFormatOperationWithAlloc implements
			Operation {

		public String getName() {
			return "decimal format operation with alloc";
		}

		public void execute() {
			Format format = new DecimalFormat("#,##0.000");
			format.format(DECIMAL_VALUE);
		}
	}

	// NOT THREAD SAFE (at least not according to API docs, Format.clone()
	// should be synchronized, see DecimalFormatOperationWithCloneWithSync)
	private static final class DecimalFormatOperationWithCloneNoSync implements
			Operation {

		public String getName() {
			return "decimal format operation with clone no sync";
		}

		public void execute() {
			Format format = (Format) DECIMAL_FORMAT.clone();
			format.format(DECIMAL_VALUE);
		}
	}

	private static final class DecimalFormatOperationWithCloneWithSync
			implements Operation {

		public String getName() {
			return "decimal format operation with clone with sync";
		}

		public void execute() {
			Format format;
			synchronized (DECIMAL_FORMAT) {
				format = (Format) DECIMAL_FORMAT.clone();
			}
			format.format(DECIMAL_VALUE);
		}
	}

	// NOT THREAD SAFE
	private static final class DecimalFormatOperationNoAllocNoSync implements
			Operation {

		public String getName() {
			return "decimal format operation no alloc no sync";
		}

		public void execute() {
			DECIMAL_FORMAT.format(DECIMAL_VALUE);
		}
	}

	private static final class DecimalFormatOperationNoAllocWithSync implements
			Operation {

		public String getName() {
			return "decimal format operation no alloc with sync";
		}

		public void execute() {
			synchronized (DECIMAL_FORMAT) {
				DECIMAL_FORMAT.format(DECIMAL_VALUE);
			}
		}
	}

	private static final class DecimalFormatOperationWithSafeFormat implements
			Operation {

		public String getName() {
			return "safe decimal format operation";
		}

		public void execute() {
			SAFE_DECIMAL_FORMAT.format(DECIMAL_VALUE);
		}
	}

}
