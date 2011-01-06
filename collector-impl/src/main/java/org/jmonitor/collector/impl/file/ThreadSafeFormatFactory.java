package org.jmonitor.collector.impl.file;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

// java.text.Format subclasses are documented as not thread-safe
// this class provides a couple of different strategies for using them
// in a thread safe manner
// see FormatPerformanceMain for some related benchmarks
public class ThreadSafeFormatFactory {

	public static Format newFormatExpectingLowContention(Format format) {
		return new ThreadSafeFormatUsingSynchronized(format);
	}

	public static Format newFormatExpectingModerateToHighContention(
			Format format) {
		return new ThreadSafeFormatUsingWeakThreadLocal(format);
	}

	// fast under uncontended usage
	// minimal memory requirement
	private static class ThreadSafeFormatUsingSynchronized extends Format {

		private static final long serialVersionUID = 1L;

		private final Format format;

		private ThreadSafeFormatUsingSynchronized(Format format) {
			this.format = format;
		}

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo,
				FieldPosition pos) {

			synchronized (format) {
				return format.format(obj, toAppendTo, pos);
			}
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {

			synchronized (format) {
				return format.parseObject(source, pos);
			}
		}
	}

	// fast under both uncontended and contended usage
	// slightly higher memory requirement (depending on the number of threads)
	@SuppressWarnings("unused")
	private static class ThreadSafeFormatUsingThreadLocal extends Format {

		private static final long serialVersionUID = 1L;

		private final Format format;

		private ThreadLocal<Format> formatThreadLocal = new ThreadLocal<Format>() {
			protected Format initialValue() {
				// Format.clone() is not thread-safe so we synchronize around it
				synchronized (format) {
					return (Format) format.clone();
				}
			}
		};

		private ThreadSafeFormatUsingThreadLocal(Format format) {
			this.format = format;
		}

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo,
				FieldPosition pos) {

			return formatThreadLocal.get().format(obj, toAppendTo, pos);
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {

			return formatThreadLocal.get().parseObject(source, pos);
		}
	}

	// best of both worlds?
	// fast under both uncontended and contended usage
	// minimal memory requirement (weak reference causes memory structure to be
	// routinely cleaned up)
	// only downside is impact on garbage collection and cost of
	// re-building format after garbage collection
	//
	// by using a WeakReference for the entire ThreadLocal as opposed to inside
	// the ThreadLocal for each Format, the GC will clean up everything and
	// leave a minimal memory structure (empty WeakReference)
	private static class ThreadSafeFormatUsingWeakThreadLocal extends Format {

		private static final long serialVersionUID = 1L;

		private final Format format;

		// mutable and accessed from multiple threads so we mark it volatile
		private volatile WeakReference<ThreadLocal<Format>> weakReference = null;

		private ThreadSafeFormatUsingWeakThreadLocal(Format format) {
			this.format = format;
		}

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo,
				FieldPosition pos) {

			return getFormat().format(obj, toAppendTo, pos);
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {

			return getFormat().parseObject(source, pos);
		}

		private Format getFormat() {

			// weakReference is only null the first time
			ThreadLocal<Format> threadLocal = weakReference == null ? null
					: weakReference.get();

			if (threadLocal == null) {
				threadLocal = new ThreadLocal<Format>() {
					protected Format initialValue() {
						// Format.clone() is not thread-safe so we
						// synchronize around it
						// TODO cloning DateFormat is slower than new (see
						// FormatPerformanceMain)
//						if (format instanceof DateFormat) {
//							return DateFormat.getDateTimeInstance(
//									DateFormat.MEDIUM, DateFormat.FULL);
//						}
						synchronized (format) {
							return (Format) format.clone();
						}
					}
				};
				// no need to synchronize setting weakReference since its ok if
				// concurrent threads in this conditional clobber the value
				weakReference = new WeakReference<ThreadLocal<Format>>(
						threadLocal);
			}
			return threadLocal.get();
		}
	}
}
