/*

 * Copyright (C) 2020-2022 Huawei Technologies Co., Ltd. All rights reserved.

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

package com.huaweicloud.common.adapters.web;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.huaweicloud.common.access.AccessLogLogger;
import com.huaweicloud.common.configration.dynamic.ContextProperties;
import com.huaweicloud.common.context.InvocationContext;
import com.huaweicloud.common.context.InvocationContextHolder;

public class AccessLogClientHttpRequestInterceptor implements
    ClientHttpRequestInterceptor, Ordered {
  private final ContextProperties contextProperties;

  private final AccessLogLogger accessLogLogger;

  public AccessLogClientHttpRequestInterceptor(ContextProperties contextProperties,
      AccessLogLogger accessLogLogger) {
    this.contextProperties = contextProperties;
    this.accessLogLogger = accessLogLogger;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
      throws IOException {
    if (!contextProperties.isEnableTraceInfo()) {
      return execution.execute(request, body);
    }

    InvocationContext context = InvocationContextHolder.getOrCreateInvocationContext();
    String url = request.getURI().getPath();
    String target = request.getURI().getHost() + ":" + request.getURI().getPort();
    accessLogLogger.log(context, "RestTemplate start request", url,
        null, target, 0, 0);

    long begin = System.currentTimeMillis();
    try {
      ClientHttpResponse response = execution.execute(request, body);
      accessLogLogger.log(context, "RestTemplate finish request", url,
          null, target, response.getRawStatusCode(),
          System.currentTimeMillis() - begin);
      return response;
    } catch (Throwable error) {
      accessLogLogger.log(context, "RestTemplate finish request(" + error.getClass().getName() + ")", url,
          null, target, -1,
          System.currentTimeMillis() - begin);
      throw error;
    }
  }

  @Override
  public int getOrder() {
    // after RestTemplateAddServiceNameContext
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }
}
