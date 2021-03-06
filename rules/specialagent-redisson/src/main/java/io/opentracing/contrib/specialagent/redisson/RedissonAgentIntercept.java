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

package io.opentracing.contrib.specialagent.redisson;

import io.opentracing.contrib.redis.common.TracingConfiguration;
import org.redisson.api.RedissonClient;

import io.opentracing.contrib.redis.redisson.TracingRedissonClient;
import io.opentracing.util.GlobalTracer;

public class RedissonAgentIntercept {
  public static Object exit(final Object returned) {
    final RedissonClient redissonClient = (RedissonClient)returned;
    return new TracingRedissonClient(redissonClient, new TracingConfiguration.Builder(GlobalTracer.get()).build());
  }
}