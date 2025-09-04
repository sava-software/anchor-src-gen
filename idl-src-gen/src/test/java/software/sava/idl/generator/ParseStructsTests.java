package software.sava.idl.generator;

import org.junit.jupiter.api.Test;
import systems.comodal.jsoniter.JsonIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ParseStructsTests {

  @Test
  void testPodBool() {
    var json = """
        {
          "name": "PodBool",
          "docs": [
            "Represents a bool stored as a byte"
          ],
          "repr": {
            "kind": "transparent"
          },
          "type": {
            "kind": "struct",
            "fields": [
              "u8"
            ]
          }
        }""";

    var ji = JsonIterator.parse(json);
    final var namedTypeParser = new AnchorNamedTypeParser(IDLType.ANCHOR, true);
    ji.testObject(namedTypeParser);
    final var namedType = namedTypeParser.create();
    assertEquals("PodBool", namedType.name());
    final var type = namedType.type();
    if (type instanceof AnchorStruct(final var fields)) {
      assertEquals(1, fields.size());
      final var field = fields.getFirst();
      assertEquals("_u8", field.name());
      if (field.type() instanceof AnchorPrimitive(final var anchorType)) {
        assertEquals(AnchorType.u8, anchorType);
      }
    }
  }
}
