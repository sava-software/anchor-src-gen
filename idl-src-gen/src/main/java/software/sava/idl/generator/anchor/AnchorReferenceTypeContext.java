package software.sava.idl.generator.anchor;

public sealed interface AnchorReferenceTypeContext extends AnchorTypeContext permits
    AnchorArray, AnchorDefined, AnchorOption, AnchorPrimitive, AnchorVector {

}
