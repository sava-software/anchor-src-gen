module software.sava.anchor_src_gen {
  requires java.net.http;

  requires transitive systems.comodal.json_iterator;

  requires transitive software.sava.core;
  requires transitive software.sava.rpc;

  exports software.sava.anchor;
}
