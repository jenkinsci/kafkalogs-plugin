/*
 * The MIT License
 *
 * Copyright (c) 2017 OC Tanner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.kafkalogs;

import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.producer.*;

public final class KafkaConsoleLogFilter extends ConsoleLogFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(KafkaConsoleLogFilter.class.getName());

    private KafkaWrapper producer;

    public KafkaConsoleLogFilter(KafkaWrapper producer) {
        super();
        this.producer = producer;
    }

    // Runs on each log line(-ish?).
    @SuppressWarnings("rawtypes")
    @Override
    public OutputStream decorateLogger(Run build, final OutputStream logger)
            throws IOException, InterruptedException {
        if (logger == null) {
            return null;
        }
        KafkaWrapper p = this.producer;
        return new LineTransformationOutputStream() {
            @Override
            protected void eol(byte[] b, int len) throws IOException {
                p.write(new String(b, 0, len));
                logger.flush();
            }

            @Override
            public void close() throws IOException {
                logger.close();
                super.close();
            }
        };
    }
}
