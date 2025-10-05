package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.core.encoding.ByteUtil;
import software.sava.core.programs.Discriminator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.zip.InflaterInputStream;

import static software.sava.core.accounts.PublicKey.PUBLIC_KEY_LENGTH;

public record OnChainIDL(PublicKey address,
                         Discriminator discriminator,
                         PublicKey authority,
                         byte[] json) {

  public static BiFunction<PublicKey, byte[], OnChainIDL> FACTORY = (address, data) -> {
    if (data == null || data.length == 0) {
      return null;
    }
    final var discriminator = AnchorUtil.parseDiscriminator(data);
    int i = AnchorUtil.DISCRIMINATOR_LENGTH;
    final var authority = PublicKey.readPubKey(data, i);
    i += PUBLIC_KEY_LENGTH;
    final int compressedLength = ByteUtil.getInt32LE(data, i);
    i += Integer.BYTES;
    try (final var bais = new ByteArrayInputStream(data, i, compressedLength)) {
      try (final var iis = new InflaterInputStream(bais)) {
        final byte[] uncompressedData = iis.readAllBytes();
        return new OnChainIDL(address, discriminator, authority, uncompressedData);
      }
    } catch (final IOException e) {
      System.err.println(Base64.getEncoder().encodeToString(data));
      throw new UncheckedIOException(e);
    }
  };
}
