package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GovernanceClientHttpImpl implements GovernanceClient {
  static final String PROP_BASE_URL = "governance.client.base-url";
  static final String PROP_DECIDE_PATH = "governance.client.decide-path";
  static final String PROP_MSG_FAIL_PATH = "governance.client.msg-fail-path";
  static final String PROP_CONNECT_TIMEOUT_MS = "governance.client.connect-timeout-ms";
  static final String PROP_SOCKET_TIMEOUT_MS = "governance.client.socket-timeout-ms";
  static final String PROP_CONNECTION_REQUEST_TIMEOUT_MS = "governance.client.connection-request-timeout-ms";
  static final String PROP_MAX_TOTAL = "governance.client.max-total";
  static final String PROP_MAX_PER_ROUTE = "governance.client.max-per-route";
  static final String PROP_KEEP_ALIVE_MS = "governance.client.keep-alive-ms";

  private static final ClientConfig DEFAULT_CONFIG = ClientConfig.fromSystemProperties();
  private static final GovernanceClient INSTANCE = new GovernanceClientHttpImpl(DEFAULT_CONFIG);

  private final ClientConfig config;
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;

  private GovernanceClientHttpImpl() {
    this(DEFAULT_CONFIG);
  }

  GovernanceClientHttpImpl(ClientConfig config) {
    this(config, buildHttpClient(config), new ObjectMapper());
  }

  GovernanceClientHttpImpl(ClientConfig config, CloseableHttpClient httpClient, ObjectMapper objectMapper) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public static GovernanceClient getInstance() {
    return INSTANCE;
  }

  @Deprecated
  public static GovernanceClient GetInstance() {
    return getInstance();
  }

  @Override
  public DecisionResponse decide(DecisionRequest request) {
    HttpPost httpPost = new HttpPost(config.baseUrl + config.decidePath);
    httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());

    try {
      httpPost.setEntity(new StringEntity(buildDecisionRequestJson(request), ContentType.APPLICATION_JSON));
      return executeDecision(httpPost);
    } catch (IOException ex) {
      throw new AccessControlException("governance decide call failed: " + ex.getMessage());
    }
  }

  @Override
  public void msgFail(DecisionRequest request, String rangerErrorMessage) {
    HttpPost httpPost = new HttpPost(config.baseUrl + config.msgFailPath);
    httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());

    try {
      httpPost.setEntity(new StringEntity(buildMsgFailRequestJson(request, rangerErrorMessage), ContentType.APPLICATION_JSON));
      executeNoResponseBody(httpPost, "msgFail");
    } catch (IOException ex) {
      throw new AccessControlException("governance msgFail call failed: " + ex.getMessage());
    }
  }

  private DecisionResponse executeDecision(HttpPost httpPost) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (statusCode != HttpStatus.SC_OK) {
        return new DecisionResponse(statusCode, "", null);
      }
      return parseDecisionResponse(body);
    }
  }

  private void executeNoResponseBody(HttpPost httpPost, String apiName) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
        throw new AccessControlException("governance " + apiName + " status code=" + statusCode);
      }
      if (response.getEntity() != null) {
        EntityUtils.consumeQuietly(response.getEntity());
      }
    }
  }

  private DecisionResponse parseDecisionResponse(String body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    int code = root.path("code").getIntValue();
    String traceId = textValue(root.path("traceId"));
    JsonNode dataNode = root.path("data");
    DecisionData data = null;
    if (!dataNode.isMissingNode() && !dataNode.isNull()) {
      ActionType actionType = parseActionType(textValue(dataNode.path("actionType")));
      if (actionType != null) {
        String msg = textValue(dataNode.path("msg"));
        boolean alertTriggered = dataNode.path("alertTriggered").getBooleanValue();
        String queryId = textValue(dataNode.path("queryId"));
        data = new DecisionData(actionType, msg, alertTriggered, queryId);
      }
    }
    return new DecisionResponse(code, traceId, data);
  }

  private String buildDecisionRequestJson(DecisionRequest request) throws IOException {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("jobName", request.getJobName());
    payload.put("user", request.getUser());
    payload.put("engine", request.getEngine() == null ? null : request.getEngine().name());
    payload.put("sql", request.getSql());
    payload.put("clientIp", request.getClientIp());
    payload.put("queryId", request.getQueryId());
    return objectMapper.writeValueAsString(payload);
  }

  private String buildMsgFailRequestJson(DecisionRequest request, String rangerErrorMessage) throws IOException {
    Map<String, Object> requestPayload = new LinkedHashMap<String, Object>();
    requestPayload.put("jobName", request.getJobName());
    requestPayload.put("user", request.getUser());
    requestPayload.put("engine", request.getEngine() == null ? null : request.getEngine().name());
    requestPayload.put("sql", request.getSql());
    requestPayload.put("clientIp", request.getClientIp());
    requestPayload.put("queryId", request.getQueryId());

    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("request", requestPayload);
    payload.put("rangerError", rangerErrorMessage);
    return objectMapper.writeValueAsString(payload);
  }

  private ActionType parseActionType(String actionType) {
    if (actionType == null || actionType.trim().isEmpty()) {
      return null;
    }
    try {
      return ActionType.valueOf(actionType);
    } catch (IllegalArgumentException ignore) {
      return null;
    }
  }

  private String textValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    return node.getTextValue();
  }

  private static CloseableHttpClient buildHttpClient(final ClientConfig config) {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(config.maxTotal);
    connectionManager.setDefaultMaxPerRoute(config.maxPerRoute);
    connectionManager.setValidateAfterInactivity(Math.max(1000, config.socketTimeoutMs));

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(config.connectTimeoutMs)
        .setSocketTimeout(config.socketTimeoutMs)
        .setConnectionRequestTimeout(config.connectionRequestTimeoutMs)
        .build();

    ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
      @Override
      public long getKeepAliveDuration(org.apache.http.HttpResponse response, HttpContext context) {
        HeaderElementIterator headerElementIterator = new BasicHeaderElementIterator(
            response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (headerElementIterator.hasNext()) {
          HeaderElement headerElement = headerElementIterator.nextElement();
          String parameter = headerElement.getName();
          String value = headerElement.getValue();
          if (value != null && "timeout".equalsIgnoreCase(parameter)) {
            try {
              return Math.min(TimeUnit.SECONDS.toMillis(Long.parseLong(value)), config.keepAliveMs);
            } catch (NumberFormatException ignore) {
              break;
            }
          }
        }
        return config.keepAliveMs;
      }
    };

    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .setKeepAliveStrategy(keepAliveStrategy)
        .disableAutomaticRetries()
        .evictExpiredConnections()
        .evictIdleConnections(config.keepAliveMs, TimeUnit.MILLISECONDS)
        .build();
  }

  static class ClientConfig {
    private final String baseUrl;
    private final String decidePath;
    private final String msgFailPath;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;
    private final int connectionRequestTimeoutMs;
    private final int maxTotal;
    private final int maxPerRoute;
    private final long keepAliveMs;

    ClientConfig(
        String baseUrl,
        String decidePath,
        String msgFailPath,
        int connectTimeoutMs,
        int socketTimeoutMs,
        int connectionRequestTimeoutMs,
        int maxTotal,
        int maxPerRoute,
        long keepAliveMs
    ) {
      this.baseUrl = trimTrailingSlash(baseUrl);
      this.decidePath = ensurePathPrefix(decidePath, "/api/v1/governance/decision");
      this.msgFailPath = ensurePathPrefix(msgFailPath, "/api/v1/governance/msg-fail");
      this.connectTimeoutMs = connectTimeoutMs;
      this.socketTimeoutMs = socketTimeoutMs;
      this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
      this.maxTotal = maxTotal;
      this.maxPerRoute = maxPerRoute;
      this.keepAliveMs = keepAliveMs;
    }

    static ClientConfig fromSystemProperties() {
      return new ClientConfig(
          property(PROP_BASE_URL, "http://127.0.0.1:8080"),
          property(PROP_DECIDE_PATH, "/api/v1/governance/decision"),
          property(PROP_MSG_FAIL_PATH, "/api/v1/governance/msg-fail"),
          propertyInt(PROP_CONNECT_TIMEOUT_MS, 200),
          propertyInt(PROP_SOCKET_TIMEOUT_MS, 500),
          propertyInt(PROP_CONNECTION_REQUEST_TIMEOUT_MS, 100),
          propertyInt(PROP_MAX_TOTAL, 512),
          propertyInt(PROP_MAX_PER_ROUTE, 256),
          propertyLong(PROP_KEEP_ALIVE_MS, 30_000L)
      );
    }

    private static String property(String key, String defaultValue) {
      String value = System.getProperty(key);
      return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static int propertyInt(String key, int defaultValue) {
      String value = System.getProperty(key);
      if (value == null || value.trim().isEmpty()) {
        return defaultValue;
      }
      try {
        int parsed = Integer.parseInt(value.trim());
        return parsed > 0 ? parsed : defaultValue;
      } catch (NumberFormatException ignore) {
        return defaultValue;
      }
    }

    private static long propertyLong(String key, long defaultValue) {
      String value = System.getProperty(key);
      if (value == null || value.trim().isEmpty()) {
        return defaultValue;
      }
      try {
        long parsed = Long.parseLong(value.trim());
        return parsed > 0 ? parsed : defaultValue;
      } catch (NumberFormatException ignore) {
        return defaultValue;
      }
    }

    private static String ensurePathPrefix(String value, String defaultValue) {
      String path = value == null || value.trim().isEmpty() ? defaultValue : value.trim();
      if (!path.startsWith("/")) {
        return "/" + path;
      }
      return path;
    }

    private static String trimTrailingSlash(String value) {
      if (value == null || value.trim().isEmpty()) {
        return "http://127.0.0.1:8080";
      }
      String trimmed = value.trim();
      while (trimmed.endsWith("/")) {
        trimmed = trimmed.substring(0, trimmed.length() - 1);
      }
      return trimmed;
    }
  }
}
