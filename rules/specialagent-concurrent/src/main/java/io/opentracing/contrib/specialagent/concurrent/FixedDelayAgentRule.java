/* Copyright 2018 The OpenTracing Authors
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

package io.opentracing.contrib.specialagent.concurrent;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.contrib.specialagent.AgentRule;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class FixedDelayAgentRule extends AgentRule {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final AgentBuilder builder) throws Exception {
    return Arrays.asList(builder
      .type(not(isInterface()).and(isSubTypeOf(ScheduledExecutorService.class)))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(FixedDelayAgentRule.class).on(named("scheduleWithFixedDelay").and(takesArguments(Runnable.class, long.class, long.class, TimeUnit.class))));
        }}));
  }

  @Advice.OnMethodEnter
  public static void exit(final @Advice.Origin String origin, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Runnable arg) throws Exception {
    if (!isEnabled(origin))
      return;

    if (isVerbose(FixedDelayAgentRule.class)) {
      final Span span = GlobalTracer.get().buildSpan("scheduleWithFixedDelay").withTag(Tags.COMPONENT, "java-concurrent").start();
      arg = new TracedRunnable(arg, span, true);
      span.finish();
    }
    else if (GlobalTracer.get().activeSpan() != null) {
      arg = new TracedRunnable(arg, GlobalTracer.get().activeSpan(), false);
    }
  }
}