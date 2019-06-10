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

package kafdrop.config.ini;

import org.apache.commons.lang3.*;

import java.io.*;

public class IniFileReader {
  private static final String COMMENT_CHARS = "#;";
  private static final String SEPARATOR_CHARS = "=:";
  private static final String QUOTE_CHARACTERS = "\"'";
  private static final String LINE_CONT = "\\";
  private static final String LINE_SEPARATOR = "\n";

  /**
   * Searches for a separator character directly before a quoting character.
   * If the first non-whitespace character before a quote character is a
   * separator, it is considered the "real" separator in this line - even if
   * there are other separators before.
   *
   * @param line       the line to be investigated
   * @param quoteIndex the index of the quote character
   * @return the index of the separator before the quote or &lt; 0 if there is
   * none
   */
  private static int findSeparatorBeforeQuote(String line, int quoteIndex) {
    int index = quoteIndex - 1;
    while (index >= 0 && Character.isWhitespace(line.charAt(index))) {
      index--;
    }

    if (index >= 0 && SEPARATOR_CHARS.indexOf(line.charAt(index)) < 0) {
      index = -1;
    }

    return index;
  }

  public IniFileProperties read(Reader input) throws IOException {

    final BufferedReader reader = new BufferedReader(input);

    final IniFileProperties properties = new IniFileProperties();

    String currentSection = null;

    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();

      if (!isCommentLine(line)) {
        if (isSectionLine(line)) {
          currentSection = line.substring(1, line.length() - 1);
        } else {
          String key = "";
          String value = "";
          int index = findSeparator(line);
          if (index >= 0) {
            key = line.substring(0, index);
            value = parseValue(line.substring(index + 1), reader);
          } else {
            key = line;
          }

          key = StringUtils.defaultIfEmpty(key.trim(), " ");

          properties.addSectionProperty(currentSection, key, value);
        }
      }

    }

    return properties;
  }

  /**
   * Tries to find the index of the separator character in the given string.
   * This method checks for the presence of separator characters in the given
   * string. If multiple characters are found, the first one is assumed to be
   * the correct separator. If there are quoting characters, they are taken
   * into account, too.
   *
   * @param line the line to be checked
   * @return the index of the separator character or -1 if none is found
   */
  private int findSeparator(String line) {
    int index = findSeparatorBeforeQuote(line, StringUtils.indexOfAny(line, QUOTE_CHARACTERS));
    if (index < 0) {
      index = StringUtils.indexOfAny(line, SEPARATOR_CHARS);
    }
    return index;
  }

  private String parseValue(String val, BufferedReader reader) throws IOException {
    StringBuilder propertyValue = new StringBuilder();
    boolean lineContinues;
    String value = val.trim();

    do {
      boolean quoted = value.startsWith("\"") || value.startsWith("'");
      boolean stop = false;
      boolean escape = false;

      char quote = quoted ? value.charAt(0) : 0;

      int i = quoted ? 1 : 0;

      StringBuilder result = new StringBuilder();
      char lastChar = 0;
      while (i < value.length() && !stop) {
        char c = value.charAt(i);

        if (quoted) {
          if ('\\' == c && !escape) {
            escape = true;
          } else if (!escape && quote == c) {
            stop = true;
          } else if (escape && quote == c) {
            escape = false;
            result.append(c);
          } else {
            if (escape) {
              escape = false;
              result.append('\\');
            }

            result.append(c);
          }
        } else {
          if (isCommentChar(c) && Character.isWhitespace(lastChar)) {
            stop = true;
          } else {
            result.append(c);
          }
        }

        i++;
        lastChar = c;
      }

      String v = result.toString();
      if (!quoted) {
        v = v.trim();
        lineContinues = lineContinues(v);
        if (lineContinues) {
          // remove trailing "\"
          v = v.substring(0, v.length() - 1).trim();
        }
      } else {
        lineContinues = lineContinues(value, i);
      }
      propertyValue.append(v);

      if (lineContinues) {
        propertyValue.append(LINE_SEPARATOR);
        value = reader.readLine();
      }
    } while (lineContinues && value != null);

    return propertyValue.toString();
  }

  private boolean lineContinues(String line) {
    return line.trim().endsWith(LINE_CONT);
  }

  /**
   * Tests whether the specified string contains a line continuation marker
   * after the specified position. This method parses the string to remove a
   * comment that might be present. Then it checks whether a line continuation
   * marker can be found at the end.
   *
   * @param line the line to check
   * @param pos  the start position
   * @return a flag whether this line continues
   */
  private boolean lineContinues(String line, int pos) {
    String s;

    if (pos >= line.length()) {
      s = line;
    } else {
      int end = pos;
      while (end < line.length() && !isCommentChar(line.charAt(end))) {
        end++;
      }
      s = line.substring(pos, end);
    }

    return lineContinues(s);
  }

  private boolean isCommentChar(char c) {
    return COMMENT_CHARS.indexOf(c) >= 0;
  }

  protected boolean isCommentLine(String line) {
    if (line == null) {
      return false;
    }
    // blank lines are also treated as comment lines
    return line.length() < 1 || COMMENT_CHARS.indexOf(line.charAt(0)) >= 0;
  }

  protected boolean isSectionLine(String line) {
    return line != null && line.startsWith("[") && line.endsWith("]");
  }
}
