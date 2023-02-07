/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.groovycps.jenkins;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.IntStream;

public class StripJsonpReader extends FilterReader {

    private boolean startStripped;
    private boolean endFound;

    /**
     * Creates a new filtered reader.
     *
     * @param in a Reader object providing the underlying stream.
     * @throws NullPointerException if <code>in</code> is <code>null</code>
     */
    protected StripJsonpReader(Reader in) {
        super(in instanceof BufferedReader ? in : new BufferedReader(in));
    }

    @Override
    public int read() throws IOException {
        stripStart();
        if (endFound)
            return -1;

        int result = super.read();

        if ((char) result == '\n' || (char) result == '\r') {
            endFound = true;
            return -1;
        }

        return result;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        stripStart();
        if (endFound)
            return -1;

        int count = super.read(cbuf, off, len);

        if (count == -1)
            return -1;

        int index = indexOfNewLine(cbuf, off, count);

        if (index != -1) {
            endFound = true;
            return index - off;
        }
        return count;
    }

    private void stripStart() throws IOException {
        if (startStripped)
            return;
        ((BufferedReader) in).readLine();
        startStripped = true;
    }

    public static int indexOfNewLine(char[] arr, int off, int len) {
        return IntStream.range(off, off + len).filter(i -> arr[i] == '\n' || arr[i] == '\r').findFirst().orElse(-1);
    }
}
