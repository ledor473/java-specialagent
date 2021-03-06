/* Copyright 2019 The OpenTracing Authors
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

package io.opentracing.contrib.specialagent;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.thrift.TProcessor;
import org.junit.Test;

import io.opentracing.thrift.SpanProcessor;

public class ThriftFingerprintTest {
  private static final Logger logger = Logger.getLogger(ThriftFingerprintTest.class);

  @Test
  public void test() throws IOException {
    FingerprintBuilder.debugVisitor = false;
    Logger.setLevel(Level.FINEST);

    final URLClassLoader classLoader = new URLClassLoader(new URL[] {SpanProcessor.class.getProtectionDomain().getCodeSource().getLocation()}, new URLClassLoader(new URL[] {TProcessor.class.getProtectionDomain().getCodeSource().getLocation()}));
    final LibraryFingerprint fingerprint = new LibraryFingerprint(classLoader, logger);
    logger.info(fingerprint.toString());

    final ClassFingerprint[] classFingerprints = new FingerprintVerifier().fingerprint(classLoader);
    for (final ClassFingerprint cfp : classFingerprints)
      logger.info(cfp.toString());
  }
}