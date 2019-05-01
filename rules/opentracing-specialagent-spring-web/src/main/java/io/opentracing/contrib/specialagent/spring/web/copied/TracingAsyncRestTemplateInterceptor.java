package io.opentracing.contrib.specialagent.spring.web.copied;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Note: From Spring Framework 5, {@link org.springframework.web.client.AsyncRestTemplate} is deprecated.
 *
 * @author Pavol Loffay
 */
public class TracingAsyncRestTemplateInterceptor implements AsyncClientHttpRequestInterceptor {
  private static final Log log = LogFactory.getLog(TracingAsyncRestTemplateInterceptor.class);

  private Tracer tracer;
  private List<RestTemplateSpanDecorator> spanDecorators;

  public TracingAsyncRestTemplateInterceptor() {
    this(GlobalTracer.get());
  }

  public TracingAsyncRestTemplateInterceptor(Tracer tracer) {
    this.tracer = tracer;
    this.spanDecorators = Collections.<RestTemplateSpanDecorator>singletonList(new RestTemplateSpanDecorator.StandardTags());
  }

  public TracingAsyncRestTemplateInterceptor(Tracer tracer, List<RestTemplateSpanDecorator> spanDecorators) {
    this.tracer = tracer;
    this.spanDecorators = new ArrayList<>(spanDecorators);
  }

  @Override
  public ListenableFuture<ClientHttpResponse> intercept(final HttpRequest httpRequest,
      byte[] body,
      AsyncClientHttpRequestExecution execution) throws IOException {

    final Span span = tracer.buildSpan(httpRequest.getMethod().toString())
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
        .start();
    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersCarrier(httpRequest.getHeaders()));

    for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
      try {
        spanDecorator.onRequest(httpRequest, span);
      } catch (RuntimeException exDecorator) {
        log.error("Exception during decorating span", exDecorator);
      }
    }

    try (Scope scope = tracer.activateSpan(span)) {
      ListenableFuture<ClientHttpResponse> future = execution.executeAsync(httpRequest, body);
      future.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
        @Override
        public void onSuccess(ClientHttpResponse httpResponse) {
          for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
            try {
              spanDecorator.onResponse(httpRequest, httpResponse, span);
            } catch (RuntimeException exDecorator) {
              log.error("Exception during decorating span", exDecorator);
            }
          }
          span.finish();
        }

        @Override
        public void onFailure(Throwable ex) {
          for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
            try {
              spanDecorator.onError(httpRequest, ex, span);
            } catch (RuntimeException exDecorator) {
              log.error("Exception during decorating span", exDecorator);
            }
          }
          span.finish();
        }
      });
      return future;
    }
  }
}
