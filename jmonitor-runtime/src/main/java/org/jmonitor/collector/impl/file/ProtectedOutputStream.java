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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * A {@link Writer} that can be inactivated (without closing the underlying writer). This is useful
 * for defensive programming when a writer needs to be passed to a method in another class.
 * 
 * @author Trask Stalnaker
 * @see AtomicLogger.LoggerCallback#doWithLogger(java.io.PrintWriter)
 * @since 1.0
 */
public class ProtectedOutputStream extends FilterOutputStream {

    private boolean active = true;

    protected ProtectedOutputStream(OutputStream out) {
        super(out);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void write(int b) throws IOException {
        if (active) {
            super.write(b);
        } else {
            throw new IllegalStateException("not allowed to use this output stream anymore");
        }
    }

    @Override
    public void write(byte[] cbuf, int off, int len) throws IOException {
        if (active) {
            // do not call super, see note about efficiency
            // in javadoc for FilterOutputStream#write(byte[], int, int)
            out.write(cbuf, off, len);
        } else {
            throw new IllegalStateException("not allowed to use this output stream anymore");
        }
    }

    @Override
    public void flush() throws IOException {
        if (active) {
            super.flush();
        } else {
            throw new IllegalStateException("not allowed to use this output stream anymore");
        }
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("not allowed to close this output stream");
    }
}
