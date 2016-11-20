package com.fd.proxydetector.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleHttpDecoder {
 
    public static final byte SPLITOR_CR = '\r';
    public static final byte SPLITOR_LF = '\n';
    public static final Pattern RESPONSE_LINE_PATTERN = 
            Pattern.compile("HTTP/1\\.\\d\\s+(\\d+)\\s+.*");
    
    private static void parseResponseLine(HttpResponse response, byte[] buffer,
            int start, int end) {
        String responseLine = new String(buffer, start, end);
        Matcher matcher = RESPONSE_LINE_PATTERN.matcher(responseLine);
        if (matcher.find()) {
            try {
                response.status = Integer.parseInt(matcher.group(1));
            } catch (Exception e) {}
        }
    }
    
    private static void parseBody(HttpResponse response, byte[] buffer, int start) {
        if (start >= buffer.length) {
            return;
        }
        response.body = new byte[buffer.length - start];
        System.arraycopy(buffer, start, response.body, 0, response.body.length);
    }
    
    public static HttpResponse decode(byte[] content) {
        HttpResponse response = new HttpResponse();
        int lineStart = 0;
        int lineEnd = lineStart;
        int lineCount = 0;
        for (int i = 0; i < content.length; i++) {
            if (content[i] == SPLITOR_CR && content[i + 1] == SPLITOR_LF) {
                lineEnd = i - 1;
                if (lineCount > 0 && lineStart == i) {
                    parseBody(response, content, i + 2);
                    break;
                }
                if (lineCount == 0) {
                    parseResponseLine(response, content, lineStart, lineEnd);
                    if (response.status == 0) {
                        break;
                    }
                }
                lineCount++;
                lineStart = i + 2;
            }
        }
        return response;
    }
}
