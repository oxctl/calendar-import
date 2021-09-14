package uk.ac.ox.it.calendarimporter.jobs.csv;

/**
 * Exception to indicate there was a problem with a row.
 */
public class RowException extends RuntimeException {

    private final long row;

    public RowException(long row, String message) {
        super(message);
        this.row = row;
    }

    public long getRow() {
        return row;
    }
}
