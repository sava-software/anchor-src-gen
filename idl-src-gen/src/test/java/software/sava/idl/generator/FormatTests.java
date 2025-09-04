package software.sava.idl.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class FormatTests {

  @Test
  void removeBlankLines() {
    final var str = String.format("""
        %s
        asdf      poiu
        %s
        zxcv""", "   ", "      ");
    final var expected = """
        
        asdf      poiu
        
        zxcv
        """;
    assertNotEquals(expected, str);
    assertEquals("""
        
        asdf      poiu
        
        zxcv
        """, AnchorSourceGenerator.removeBlankLines(str));
  }
}
