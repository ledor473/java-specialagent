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

package io.opentracing.contrib.specialagent.grpc;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.util.Arrays;

import io.opentracing.contrib.specialagent.AgentRule;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class GrpcAgentRule extends AgentRule {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final AgentBuilder builder) {
    return Arrays.asList(builder
      .type(named("io.grpc.util.MutableHandlerRegistry"))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(Registry.class).on(named("addService").and(takesArguments(1))));
        }})
      .type(hasSuperType(named("io.grpc.ServerBuilder")))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(Registry.class).on(named("addService").and(takesArguments(1))));
        }})
      .type(hasSuperType(named("io.grpc.stub.AbstractStub")))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(Stub.class).on(named("getChannel")));
        }}));
  }

  public static class Registry {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.Origin String origin, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Object service) {
      if (isEnabled(origin))
        service = GrpcAgentIntercept.addService(service);
    }
  }

  public static class Stub {
    @Advice.OnMethodExit
    public static void exit(final @Advice.Origin String origin, @Advice.Return(readOnly = false, typing = Typing.DYNAMIC) Object returned) {
      if (isEnabled(origin))
        returned = GrpcAgentIntercept.build(returned);
    }
  }
}