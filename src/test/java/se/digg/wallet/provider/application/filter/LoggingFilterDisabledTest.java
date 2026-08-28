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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({
    MockitoExtension.class,
    OutputCaptureExtension.class
})
@SpringBootTest(
    properties = {
        "properties.logging-filter.enabled=false"
    })
public class LoggingFilterDisabledTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LoggingFilter filter;

  @Mock
  private SensitiveDataMasker sensitiveDataMasker;

  private final MockFilterChain filterChain = new MockFilterChain();

  @Test
  void shouldNotLogWhenDisabled(CapturedOutput console) throws IOException, ServletException {

    var httpServletRequest = MockMvcRequestBuilders
        .get("/test")
        .buildRequest(new SpringBootMockServletContext("/"));
    var httpServletResponse = new MockHttpServletResponse();

    filter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

    var consoleOut = console.getOut();

    assertThat(getLoggedObject(consoleOut)).isEmpty();
  }

  private Map getLoggedObject(String consoleOut) throws JacksonException {

    var startPos = consoleOut.indexOf("{");
    if (startPos > -1) {
      // Find the matching closing brace for the first JSON object
      int braceCount = 0;
      boolean inString = false;
      boolean escapeNext = false;
      int endPos = -1;

      for (int i = startPos; i < consoleOut.length(); i++) {
        char c = consoleOut.charAt(i);

        if (escapeNext) {
          escapeNext = false;
          continue;
        }

        if (c == '\\') {
          escapeNext = true;
          continue;
        }

        if (c == '"' && !escapeNext) {
          inString = !inString;
          continue;
        }

        if (!inString) {
          if (c == '{') {
            braceCount++;
          } else if (c == '}') {
            braceCount--;
            if (braceCount == 0) {
              endPos = i + 1;
              break;
            }
          }
        }
      }

      if (endPos > startPos) {
        var jsonString = consoleOut.substring(startPos, endPos);
        return objectMapper.readValue(jsonString, Map.class);
      }
    }
    return Map.of();
  }
}
