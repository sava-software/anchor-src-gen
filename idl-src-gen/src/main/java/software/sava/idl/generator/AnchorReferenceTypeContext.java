package software.sava.idl.generator;

public sealed interface AnchorReferenceTypeContext extends AnchorTypeContext permits
    AnchorArray, AnchorDefined, AnchorOption, AnchorPrimitive, AnchorVector {

}
