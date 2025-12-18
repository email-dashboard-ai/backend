package org.example.ai.util;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import org.jsoup.Jsoup;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GmailMessageTextExtractor {

  public interface AttachmentFetcher {
    byte[] fetch(String attachmentId);
  }

  public static String extractBestEffortText(
      Message message, AttachmentFetcher attachmentFetcher, int maxChars) {
    if (message == null || message.getPayload() == null) {
      return "";
    }

    MessagePart root = message.getPayload();

    Optional<String> plain = findFirstTextPart(root, attachmentFetcher, "text/plain");
    if (plain.isPresent()) {
      return truncate(normalizeWhitespace(plain.get()), maxChars);
    }

    Optional<String> html = findFirstTextPart(root, attachmentFetcher, "text/html");
    if (html.isPresent()) {
      String text = Jsoup.parse(html.get()).text();
      return truncate(normalizeWhitespace(text), maxChars);
    }

    String snippet = message.getSnippet();
    return truncate(normalizeWhitespace(snippet != null ? snippet : ""), maxChars);
  }

  private static Optional<String> findFirstTextPart(
      MessagePart root, AttachmentFetcher attachmentFetcher, String desiredMimeType) {
    Deque<MessagePart> stack = new ArrayDeque<>();
    stack.push(root);

    while (!stack.isEmpty()) {
      MessagePart part = stack.pop();
        String mimeType = part.getMimeType();
      if (mimeType != null
          && mimeType.toLowerCase(Locale.ROOT).startsWith(desiredMimeType.toLowerCase(Locale.ROOT))) {
        String content = readPartBody(part, attachmentFetcher);
        if (content != null && !content.isBlank()) {
          return Optional.of(content);
        }
      }

      List<MessagePart> children = part.getParts();
      if (children != null) {
        for (int i = children.size() - 1; i >= 0; i--) {
          stack.push(children.get(i));
        }
      }
    }

    return Optional.empty();
  }

  private static String readPartBody(MessagePart part, AttachmentFetcher attachmentFetcher) {
    MessagePartBody body = part.getBody();
    if (body == null) {
      return null;
    }

    String data = body.getData();
    if (data != null && !data.isBlank()) {
      return decodeBase64UrlToString(data);
    }

    String attachmentId = body.getAttachmentId();
    if (attachmentId != null && !attachmentId.isBlank() && attachmentFetcher != null) {
      byte[] bytes = attachmentFetcher.fetch(attachmentId);
      if (bytes != null && bytes.length > 0) {
        return new String(bytes, StandardCharsets.UTF_8);
      }
    }

    return null;
  }

  private static String decodeBase64UrlToString(String base64Url) {
    try {
      return new String(Base64.getUrlDecoder().decode(base64Url), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      // Some Gmail bodies omit padding
      String padded = padBase64(base64Url);
      return new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
    }
  }

  private static String padBase64(String s) {
    int mod = s.length() % 4;
    if (mod == 0) {
      return s;
    }
    int pad = 4 - mod;
    return s + "=".repeat(pad);
  }

  private static String normalizeWhitespace(String s) {
    if (s == null) {
      return "";
    }
    return s.replaceAll("\\s+", " ").trim();
  }

  private static String truncate(String s, int maxChars) {
    if (s == null) {
      return "";
    }
    if (maxChars <= 0 || s.length() <= maxChars) {
      return s;
    }
    return s.substring(0, maxChars);
  }
}
