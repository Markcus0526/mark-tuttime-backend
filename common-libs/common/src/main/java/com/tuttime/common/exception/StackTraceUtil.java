package com.tuttime.common.exception;

/**
 * Utility class for generating shortened stack trace summaries for logging.
 *
 * <p>This helps reduce log verbosity by limiting stack traces to a few lines
 * instead of printing the entire stack trace.
 */
public class StackTraceUtil {

    /**
     * Gets a shortened stack trace summary for logging.
     *
     * @param ex       The exception
     * @param maxLines Maximum number of stack trace lines to include
     * @return Stack trace summary string (first N lines), or empty string if ex is null
     */
    public static String getStackTraceSummary(Throwable ex, int maxLines) {
        if (ex == null) {
            return "";
        }

        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        int linesToInclude = Math.min(maxLines, stackTrace.length);

        for (int i = 0; i < linesToInclude; i++) {
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(stackTrace[i].toString());
        }

        if (stackTrace.length > maxLines) {
            summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
        }

        return summary.toString();
    }
}

