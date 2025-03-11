package software.sava.anchor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class UtilTests {

  @Test
  void testSnakeCase() {
    assertEquals("system_transfer", AnchorUtil.snakeCase("SystemTransfer"));
  }
}
