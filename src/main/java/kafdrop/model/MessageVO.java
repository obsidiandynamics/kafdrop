/*
 * Copyright 2017 Kafdrop contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package kafdrop.model;

import java.util.*;
import java.util.stream.*;

public final class MessageVO {
  private int partition;
  private long offset;
  private String message;
  private String key;
  private Map<String, String> headers;
  private Date timestamp;

  public int getPartition() { return partition; }
  public void setPartition(int partition) { this.partition = partition; }

  public long getOffset() { return offset; }
  public void setOffset(long offset) { this.offset = offset; }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getHeadersFormatted() {
    if (headers.isEmpty()) {
      return "empty";
    } else {
      return headers.entrySet().stream()
          .map(e -> e.getKey() + ": " + e.getValue())
          .collect(Collectors.joining(", "));
    }
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
