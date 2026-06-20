package dev.local.ai.logging;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

//gererated by cursor to review
final class LoggingStreams {

    private LoggingStreams() {
    }

    static PrintStream asPrintStream(Consumer<String> lineSink) {
        return new PrintStream(new LineBufferingOutputStream(lineSink), true, StandardCharsets.UTF_8);
    }

    private static final class LineBufferingOutputStream extends OutputStream {

        private static final int INITIAL_BUFFER = 256;

        private final Consumer<String> lineSink;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(INITIAL_BUFFER);

        LineBufferingOutputStream(Consumer<String> lineSink) {
            this.lineSink = lineSink;
        }

        @Override
        public synchronized void write(int b) {
            if (b == '\n') {
                flushLine();
            } else {
                buffer.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] data, int offset, int length) {
            int lineStart = offset;
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                if (data[i] == '\n') {
                    buffer.write(data, lineStart, i - lineStart);
                    flushLine();
                    lineStart = i + 1;
                }
            }
            if (lineStart < end) {
                buffer.write(data, lineStart, end - lineStart);
            }
        }

        @Override
        public synchronized void flush() {
            flushLine();
        }

        private void flushLine() {
            int size = buffer.size();
            if (size == 0) {
                return;
            }
            byte[] bytes = buffer.toByteArray();
            buffer.reset();
            int strippedLength = (bytes[size - 1] == '\r') ? size - 1 : size;
            if (strippedLength == 0) {
                return;
            }
            lineSink.accept(new String(bytes, 0, strippedLength, StandardCharsets.UTF_8));
        }
    }
}
