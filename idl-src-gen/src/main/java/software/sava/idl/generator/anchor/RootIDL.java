package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;

import java.util.List;

public abstract class RootIDL implements IDL {

  protected final IDLType idlType;
  protected final PublicKey address;
  protected final String version;
  protected final String name;
  protected final String origin;
  protected final List<String> docs;
  protected final byte[] json;

  protected RootIDL(final IDLType idlType,
                    final PublicKey address,
                    final String version,
                    final String name,
                    final String origin,
                    final List<String> docs,
                    final byte[] json) {
    this.idlType = idlType;
    this.address = address;
    this.version = version;
    this.name = name;
    this.origin = origin;
    this.docs = docs;
    this.json = json;
  }

  @Override
  public final IDLType type() {
    return idlType;
  }

  @Override
  public final PublicKey address() {
    return address;
  }

  @Override
  public final String version() {
    return version;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final String origin() {
    return origin;
  }

  @Override
  public final List<String> docs() {
    return docs;
  }

  @Override
  public final byte[] json() {
    return json;
  }
}
