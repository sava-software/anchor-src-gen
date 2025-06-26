package software.sava.anchor;

import org.junit.jupiter.api.Test;
import software.sava.core.programs.Discriminator;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DiscriminatorTests {

  @Test
  void testHashDiscriminator() {
    final var discriminator = AnchorUtil.toDiscriminator("wsolWrap");
    assertEquals(Discriminator.toDiscriminator(26, 2, 139, 159, 239, 195, 193, 9), discriminator);
  }
}
