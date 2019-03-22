package uk.ac.ox.it.calendarimporter.jobs.ical;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Limits an input stream to a number of bytes. Throws an error when an attempt is made to ready
 * beyond that limit.
 */
public class TerminatingInputStream extends FilterInputStream {

  private long left;
  private long mark = -1;

  public TerminatingInputStream(InputStream in, long limit) {
    super(in);
    checkNotNull(in);
    checkArgument(limit >= 0, "limit must be non-negative");
    left = limit;
  }

  // it's okay to mark even if mark isn't supported, as reset won't work
  @Override
  public synchronized void mark(int readLimit) {
    in.mark(readLimit);
    mark = left;
  }

  @Override
  public int read() throws IOException {
    if (left == 0) {
      throw new TerminatedIOException("Attempted to read past limit");
    }

    int result = in.read();
    if (result != -1) {
      --left;
    }
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (left == 0) {
      throw new TerminatedIOException("Attempted to read past limit");
    }

    len = (int) Math.min(len, left);
    int result = in.read(b, off, len);
    if (result != -1) {
      left -= result;
    }
    return result;
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark not supported");
    }
    if (mark == -1) {
      throw new IOException("Mark not set");
    }

    in.reset();
    left = mark;
  }

  @Override
  public long skip(long n) throws IOException {
    n = Math.min(n, left);
    long skipped = in.skip(n);
    left -= skipped;
    return skipped;
  }
}
