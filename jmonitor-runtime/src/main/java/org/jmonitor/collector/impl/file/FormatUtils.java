/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.collector.impl.file;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for formatting numbers, dates and text.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// DateFormat and NumberFormat are not thread safe so we have to synchronize access
// this still seems better than creating new instances of DateFormat all the time
// (semi-related see http://forums.sun.com/thread.jspa?threadID=5331680)
public final class FormatUtils {

    private static final double NANOSECONDS_PER_MILLISECOND = 1000000.0;
    private static final double NANOSECONDS_PER_SECOND = 1000000000.0;
    private static final int MILLISECONDS_PER_SECOND = 1000;

    // since timing data is collected in nanoseconds, we can display more
    // precision just by changing these formatter patterns
    private static final NumberFormat SECONDS_FORMAT = new DecimalFormat("#,##0.000");
    private static final NumberFormat MILLISECONDS_FORMAT = new DecimalFormat("#,##0.0");

    // There's not really a correct locale-independenty way to format a date including milliseconds
    // (e.g. DateFormat.getDateTimeInstance(..))
    // so this class has one implementation (which integrates the milliseconds into the format) for
    // Locale.US
    // and another implementation for other locales which just adds the milliseconds at the end
    // of the locale-independent format.
    private static final DateFormat DATE_FORMAT;
    private static final boolean MILLISECONDS_INTEGRATED_INTO_DATE_FORMAT;
    private static final NumberFormat NON_INTEGRATED_MILLISECONDS_FORMAT =
            new DecimalFormat("#.000");

    static {
        if (Locale.getDefault().equals(Locale.US)) {
            DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy hh:mm:ss.SSS a z", Locale.US);
            MILLISECONDS_INTEGRATED_INTO_DATE_FORMAT = true;
        } else {
            DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);
            MILLISECONDS_INTEGRATED_INTO_DATE_FORMAT = false;
        }
    }

    // utility class
    private FormatUtils() {}

    public static String formatNanosecondsAsSeconds(long nanoseconds) {
        synchronized (SECONDS_FORMAT) {
            return SECONDS_FORMAT.format(nanoseconds / NANOSECONDS_PER_SECOND);
        }
    }

    public static String formatNanosecondsAsMilliseconds(long nanoseconds) {
        synchronized (MILLISECONDS_FORMAT) {
            return MILLISECONDS_FORMAT.format(nanoseconds / NANOSECONDS_PER_MILLISECOND);
        }
    }

    public static String formatWithMilliseconds(Date date) {
        if (MILLISECONDS_INTEGRATED_INTO_DATE_FORMAT) {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.format(date);
            }
        } else {
            StringBuffer text = new StringBuffer();
            synchronized (DATE_FORMAT) {
                text.append(DATE_FORMAT.format(date));
            }
            text.append(" (");
            text.append(NON_INTEGRATED_MILLISECONDS_FORMAT.format(
                    (date.getTime() % MILLISECONDS_PER_SECOND) / (double) MILLISECONDS_PER_SECOND));
            text.append(')');
            return text.toString();
        }
    }

    public static String formatPercentage(double percentage) {
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        return format.format(percentage);
    }

    public static void printLeftPaddedTable(PrintWriter out, String[][] rows) {

        if (rows.length == 0) {
            return;
        }

        int[] widths = new int[rows[0].length];

        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = row[i].length();
                }
            }
        }

        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    out.print("  ");
                }
                FormatUtils.printPadding(out, widths[i] - StringUtils.length(row[i]));
                out.print(StringUtils.defaultString(row[i]));
            }
            out.println();
        }
    }

    public static void printPadding(PrintWriter out, int numSpaces) {
        for (int i = 0; i < numSpaces; i++) {
            out.print(' ');
        }
    }

    public static void printHeader(PrintWriter out, String leftText, String... rightText) {

        out.println(OperationPrinter.HEADING2);
        if (rightText.length > 0) {
            for (int i = 0; i < rightText.length - 1; i++) {
                printPadding(out, OperationPrinter.HEADING2.length() - rightText[i].length());
                out.println(rightText[i]);
            }
            String lastRightText = rightText[rightText.length - 1];
            out.print(leftText);
            printPadding(out, OperationPrinter.HEADING2.length() - leftText.length()
                    - lastRightText.length());
            out.println(lastRightText);
        } else {
            out.println(leftText);
        }
        out.println(OperationPrinter.HEADING2);
        out.println();
    }
}
