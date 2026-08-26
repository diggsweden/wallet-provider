// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.provider.application.filter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.web.SpringBootMockServletContext;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({
    MockitoExtension.class,
    OutputCaptureExtension.class
})
@SpringBootTest(
    properties = {
        "properties.logging-filter.enabled=true",
        "properties.logging-filter.log-body=false",
        "properties.logging-filter.exclude-path.exact-match="
    })
public class LoggingFilterBodyDisabledTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LoggingFilter filter;

  @MockitoBean
  private SensitiveDataMasker sensitiveDataMasker;

  @Mock
  private MockFilterChain filterChain;

  @Test
  void bodyNotLoggedWhenLogBodyIsFalse(CapturedOutput console)
      throws IOException, ServletException {

    var httpServletRequest = MockMvcRequestBuilders
        .post("/test")
        .contentType("application/json")
        .content("{\"token\":\"sensitive-value\",\"data\":\"test\"}")
        .buildRequest(new SpringBootMockServletContext("/"));
    var httpServletResponse = new MockHttpServletResponse();

    when(sensitiveDataMasker.maskHeaders(any())).thenReturn(Map.of());

    filter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

    var consoleOut = console.getOut();
    var loggedRequest = getLoggedRequest(consoleOut);

    assertThat(loggedRequest).doesNotContainKey("body");
  }

  @Test
  void responseBodyNotLoggedWhenLogBodyIsFalse(CapturedOutput console)
      throws IOException, ServletException {

    var httpServletRequest = MockMvcRequestBuilders
        .get("/test")
        .buildRequest(new SpringBootMockServletContext("/"));
    var httpServletResponse = new MockHttpServletResponse();

    filter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

    var consoleOut = console.getOut();
    var loggedResponse = getLoggedResponse(consoleOut);

    assertThat(loggedResponse).doesNotContainKey("body");
  }

  private Map<String, Object> getLoggedRequest(String consoleOut) throws JacksonException {
    var loggedObject = getLoggedObject(consoleOut, 1);
    return (Map<String, Object>) loggedObject.get("request");
  }

  private Map<String, Object> getLoggedResponse(String consoleOut) throws JacksonException {
    var loggedObject = getLoggedObject(consoleOut, 2);
    return (Map<String, Object>) loggedObject.get("response");
  }

  private Map<String, Object> getLoggedObject(String consoleOut, int rowNumberToGet)
      throws JacksonException {

    var startPos = consoleOut.indexOf("{");
    if (startPos > -1
        && consoleOut.substring(startPos, consoleOut.length() - 1).contains("}")) {
      var jsonString = consoleOut.substring(startPos);

      java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(jsonString, "\n");
      int tokenCount = 0;
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        tokenCount++;
        int startCurlyBracketPos = token.indexOf("{");
        String subToken = token.substring(startCurlyBracketPos);

        if (tokenCount == rowNumberToGet) {
          return (Map<String, Object>) (objectMapper.readValue(subToken, Map.class));
        }
      }
    }
    return Map.of();
  }
}
