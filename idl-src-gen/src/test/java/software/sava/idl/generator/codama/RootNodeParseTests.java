package software.sava.idl.generator.codama;

import org.junit.jupiter.api.Test;
import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class RootNodeParseTests {

  @Test
  void testParseRootNodeFromProgramMetadataJson() throws IOException {
    final var jsonPath = Path.of("src/test/resources/program_metadata.json");
    final var jsonBytes = Files.readAllBytes(jsonPath);

    final var rootNode = RootNode.parse(JsonIterator.parse(jsonBytes));

    assertNotNull(rootNode);
    assertEquals("codama", rootNode.standard());
    assertEquals("1.0.0", rootNode.version());

    final var program = rootNode.program();
    assertNotNull(program);
    assertEquals("programMetadata", program.name());
    assertEquals(PublicKey.fromBase58Encoded("ProgM6JCCvbYkfKqJYHePx4xxSUSqJp7rh8Lyv7nk7S"),
        program.publicKey()
    );
    assertEquals("0.0.0", program.version());
    assertNull(program.origin(), "origin not specified in JSON");

    final var pdas = program.pdas();
    assertNotNull(pdas);
    assertEquals(3, pdas.size());

    final var canonicalPda = pdas.getFirst();
    assertEquals("canonical", canonicalPda.name());
    assertFalse(canonicalPda.docs().isEmpty());
    assertEquals("The canonical derivation for metadata accounts managed by the program authority itself.",
        canonicalPda.docs().getFirst()
    );
    final var canonicalSeeds = canonicalPda.seeds();
    assertEquals(2, canonicalSeeds.size());

    final var canonicalFirstSeed = canonicalSeeds.getFirst();
    assertInstanceOf(VariablePdaSeedNode.class, canonicalFirstSeed);
    final var canonicalFirstVarSeed = (VariablePdaSeedNode) canonicalFirstSeed;
    assertEquals("program", canonicalFirstVarSeed.name());
    assertFalse(canonicalFirstVarSeed.docs().isEmpty());
    assertEquals("The program to which the metadata belongs.", canonicalFirstVarSeed.docs().getFirst());
    assertInstanceOf(PublicKeyTypeNode.class, canonicalFirstVarSeed.type());

    final var canonicalLastSeed = canonicalSeeds.getLast();
    assertInstanceOf(VariablePdaSeedNode.class, canonicalLastSeed);
    final var canonicalLastVarSeed = (VariablePdaSeedNode) canonicalLastSeed;
    assertEquals("seed", canonicalLastVarSeed.name());
    assertFalse(canonicalLastVarSeed.docs().isEmpty());
    assertEquals("The seed deriving the metadata account.", canonicalLastVarSeed.docs().getFirst());
    assertInstanceOf(DefinedTypeLinkNode.class, canonicalLastVarSeed.type());
    assertEquals("seed", ((DefinedTypeLinkNode) canonicalLastVarSeed.type()).name());

    final var nonCanonicalPda = pdas.get(1);
    assertEquals("nonCanonical", nonCanonicalPda.name());
    assertFalse(nonCanonicalPda.docs().isEmpty());
    assertEquals("The derivation for metadata accounts managed by third-party authorities.",
        nonCanonicalPda.docs().getFirst()
    );
    final var nonCanonicalSeeds = nonCanonicalPda.seeds();
    assertEquals(3, nonCanonicalSeeds.size());

    final var nonCanonicalFirstSeed = nonCanonicalSeeds.getFirst();
    assertInstanceOf(VariablePdaSeedNode.class, nonCanonicalFirstSeed);
    final var nonCanonicalFirstVarSeed = (VariablePdaSeedNode) nonCanonicalFirstSeed;
    assertEquals("program", nonCanonicalFirstVarSeed.name());
    assertFalse(nonCanonicalFirstVarSeed.docs().isEmpty());
    assertEquals("The program to which the metadata belongs.", nonCanonicalFirstVarSeed.docs().getFirst());
    assertInstanceOf(PublicKeyTypeNode.class, nonCanonicalFirstVarSeed.type());

    final var nonCanonicalLastSeed = nonCanonicalSeeds.getLast();
    assertInstanceOf(VariablePdaSeedNode.class, nonCanonicalLastSeed);
    final var nonCanonicalLastVarSeed = (VariablePdaSeedNode) nonCanonicalLastSeed;
    assertEquals("seed", nonCanonicalLastVarSeed.name());
    assertFalse(nonCanonicalLastVarSeed.docs().isEmpty());
    assertEquals("The seed deriving the metadata account.", nonCanonicalLastVarSeed.docs().getFirst());
    assertInstanceOf(DefinedTypeLinkNode.class, nonCanonicalLastVarSeed.type());
    assertEquals("seed", ((DefinedTypeLinkNode) nonCanonicalLastVarSeed.type()).name());

    final var metadataPda = pdas.getLast();
    assertEquals("metadata", metadataPda.name());
    assertFalse(metadataPda.docs().isEmpty());
    assertEquals("The derivation for metadata accounts, canonical or not, depending if an authority is provided.",
        metadataPda.docs().getFirst()
    );
    final var metadataSeeds = metadataPda.seeds();
    assertEquals(3, metadataSeeds.size());

    final var metadataFirstSeed = metadataSeeds.getFirst();
    assertInstanceOf(VariablePdaSeedNode.class, metadataFirstSeed);
    final var metadataFirstVarSeed = (VariablePdaSeedNode) metadataFirstSeed;
    assertEquals("program", metadataFirstVarSeed.name());
    assertFalse(metadataFirstVarSeed.docs().isEmpty());
    assertEquals("The program to which the metadata belongs.", metadataFirstVarSeed.docs().getFirst());
    assertInstanceOf(PublicKeyTypeNode.class, metadataFirstVarSeed.type());

    final var metadataLastSeed = metadataSeeds.getLast();
    assertInstanceOf(VariablePdaSeedNode.class, metadataLastSeed);
    final var metadataLastVarSeed = (VariablePdaSeedNode) metadataLastSeed;
    assertEquals("seed", metadataLastVarSeed.name());
    assertFalse(metadataLastVarSeed.docs().isEmpty());
    assertEquals("The seed deriving the metadata account.", metadataLastVarSeed.docs().getFirst());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataLastVarSeed.type());
    assertEquals("seed", ((DefinedTypeLinkNode) metadataLastVarSeed.type()).name());

    final var accounts = program.accounts();
    assertNotNull(accounts);
    assertEquals(2, accounts.size());

    final var bufferAccount = accounts.getFirst();
    assertEquals("buffer", bufferAccount.name());
    assertEquals(0, bufferAccount.size(), "variable size");
    assertNotNull(bufferAccount.data());
    assertNotNull(bufferAccount.pda());
    assertTrue(bufferAccount.discriminators().isEmpty());

    assertInstanceOf(StructTypeNode.class, bufferAccount.data());
    final var bufferStruct = (StructTypeNode) bufferAccount.data();
    final var bufferFields = bufferStruct.fields();
    assertEquals(6, bufferFields.size());

    final var bufferField0 = bufferFields.getFirst();
    assertEquals("discriminator", bufferField0.name());
    assertInstanceOf(DefinedTypeLinkNode.class, bufferField0.type());
    assertEquals("accountDiscriminator", ((DefinedTypeLinkNode) bufferField0.type()).name());
    assertNotNull(bufferField0.defaultValue());
    assertInstanceOf(ValueNode.Enum.class, bufferField0.defaultValue());
    final var bufferEnumValue = (ValueNode.Enum) bufferField0.defaultValue();
    assertEquals("buffer", bufferEnumValue.variant());
    assertEquals(ValueStrategy.omitted, bufferField0.defaultValueStrategy());

    final var bufferField1 = bufferFields.get(1);
    assertEquals("program", bufferField1.name());
    assertInstanceOf(ZeroableOptionTypeNode.class, bufferField1.type());
    final var bufferProgramType = (ZeroableOptionTypeNode) bufferField1.type();
    assertInstanceOf(PublicKeyTypeNode.class, bufferProgramType.item());
    assertNull(bufferField1.defaultValue());
    assertNull(bufferField1.defaultValueStrategy());

    final var bufferField2 = bufferFields.get(2);
    assertEquals("authority", bufferField2.name());
    assertInstanceOf(ZeroableOptionTypeNode.class, bufferField2.type());
    final var bufferAuthorityType = (ZeroableOptionTypeNode) bufferField2.type();
    assertInstanceOf(PublicKeyTypeNode.class, bufferAuthorityType.item());
    assertNull(bufferField2.defaultValue());
    assertNull(bufferField2.defaultValueStrategy());

    final var bufferField3 = bufferFields.get(3);
    assertEquals("canonical", bufferField3.name());
    assertInstanceOf(BooleanTypeNode.class, bufferField3.type());
    final var bufferCanonicalType = (BooleanTypeNode) bufferField3.type();
    assertInstanceOf(NumberTypeNode.class, bufferCanonicalType.size());
    final var bufferCanonicalSizeType = (NumberTypeNode) bufferCanonicalType.size();
    assertEquals(NumberFormat.u8, bufferCanonicalSizeType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, bufferCanonicalSizeType.endian());
    assertNull(bufferField3.defaultValue());
    assertNull(bufferField3.defaultValueStrategy());

    final var bufferField4 = bufferFields.get(4);
    assertEquals("seed", bufferField4.name());
    assertInstanceOf(PostOffsetTypeNode.class, bufferField4.type());
    final var bufferSeedType = (PostOffsetTypeNode) bufferField4.type();
    assertEquals(14, bufferSeedType.offset());
    assertEquals(PostOffsetTypeNode.Strategy.padded, bufferSeedType.strategy());
    assertInstanceOf(DefinedTypeLinkNode.class, bufferSeedType.typeNode());
    final var bufferSeedNestedType = (DefinedTypeLinkNode) bufferSeedType.typeNode();
    assertEquals("seed", bufferSeedNestedType.name());
    assertNull(bufferField4.defaultValue());
    assertNull(bufferField4.defaultValueStrategy());

    final var bufferField5 = bufferFields.getLast();
    assertEquals("data", bufferField5.name());
    assertInstanceOf(BytesTypeNode.class, bufferField5.type());
    assertNull(bufferField5.defaultValue());
    assertNull(bufferField5.defaultValueStrategy());

    final var metadataAccount = accounts.getLast();
    assertEquals("metadata", metadataAccount.name());
    assertEquals(0, metadataAccount.size(), "variable size");
    assertNotNull(metadataAccount.data());
    assertNotNull(metadataAccount.pda());
    assertTrue(metadataAccount.discriminators().isEmpty());

    assertInstanceOf(StructTypeNode.class, metadataAccount.data());
    final var metadataStruct = (StructTypeNode) metadataAccount.data();
    final var metadataFields = metadataStruct.fields();
    assertEquals(12, metadataFields.size());

    assertEquals("discriminator", metadataFields.getFirst().name());
    assertEquals("program", metadataFields.get(1).name());
    assertEquals("authority", metadataFields.get(2).name());
    assertEquals("mutable", metadataFields.get(3).name());
    assertEquals("canonical", metadataFields.get(4).name());
    assertEquals("seed", metadataFields.get(5).name());
    assertEquals("encoding", metadataFields.get(6).name());
    assertEquals("compression", metadataFields.get(7).name());
    assertEquals("format", metadataFields.get(8).name());
    assertEquals("dataSource", metadataFields.get(9).name());
    assertEquals("dataLength", metadataFields.get(10).name());
    assertEquals("data", metadataFields.getLast().name());

    final var metadataField0 = metadataFields.getFirst();
    assertEquals("discriminator", metadataField0.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField0.type());
    assertEquals("accountDiscriminator", ((DefinedTypeLinkNode) metadataField0.type()).name());
    assertNotNull(metadataField0.defaultValue());
    assertInstanceOf(ValueNode.Enum.class, metadataField0.defaultValue());
    final var metadataEnumValue = (ValueNode.Enum) metadataField0.defaultValue();
    assertEquals("metadata", metadataEnumValue.variant());
    assertEquals(ValueStrategy.omitted, metadataField0.defaultValueStrategy());

    final var metadataField1 = metadataFields.get(1);
    assertEquals("program", metadataField1.name());
    assertInstanceOf(PublicKeyTypeNode.class, metadataField1.type());
    assertNull(metadataField1.defaultValue());
    assertNull(metadataField1.defaultValueStrategy());

    final var metadataField2 = metadataFields.get(2);
    assertEquals("authority", metadataField2.name());
    assertInstanceOf(ZeroableOptionTypeNode.class, metadataField2.type());
    final var metadataAuthorityType = (ZeroableOptionTypeNode) metadataField2.type();
    assertInstanceOf(PublicKeyTypeNode.class, metadataAuthorityType.item());
    assertNull(metadataField2.defaultValue());
    assertNull(metadataField2.defaultValueStrategy());

    final var metadataField3 = metadataFields.get(3);
    assertEquals("mutable", metadataField3.name());
    assertInstanceOf(BooleanTypeNode.class, metadataField3.type());
    final var metadataMutableType = (BooleanTypeNode) metadataField3.type();
    assertInstanceOf(NumberTypeNode.class, metadataMutableType.size());
    final var metadataMutableSizeType = (NumberTypeNode) metadataMutableType.size();
    assertEquals(NumberFormat.u8, metadataMutableSizeType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, metadataMutableSizeType.endian());
    assertNull(metadataField3.defaultValue());
    assertNull(metadataField3.defaultValueStrategy());

    final var metadataField4 = metadataFields.get(4);
    assertEquals("canonical", metadataField4.name());
    assertInstanceOf(BooleanTypeNode.class, metadataField4.type());
    final var metadataCanonicalType = (BooleanTypeNode) metadataField4.type();
    assertInstanceOf(NumberTypeNode.class, metadataCanonicalType.size());
    final var metadataCanonicalSizeType = (NumberTypeNode) metadataCanonicalType.size();
    assertEquals(NumberFormat.u8, metadataCanonicalSizeType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, metadataCanonicalSizeType.endian());
    assertNull(metadataField4.defaultValue());
    assertNull(metadataField4.defaultValueStrategy());

    final var metadataField5 = metadataFields.get(5);
    assertEquals("seed", metadataField5.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField5.type());
    assertEquals("seed", ((DefinedTypeLinkNode) metadataField5.type()).name());
    assertNull(metadataField5.defaultValue());
    assertNull(metadataField5.defaultValueStrategy());

    final var metadataField6 = metadataFields.get(6);
    assertEquals("encoding", metadataField6.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField6.type());
    assertEquals("encoding", ((DefinedTypeLinkNode) metadataField6.type()).name());
    assertNull(metadataField6.defaultValue());
    assertNull(metadataField6.defaultValueStrategy());

    final var metadataField7 = metadataFields.get(7);
    assertEquals("compression", metadataField7.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField7.type());
    assertEquals("compression", ((DefinedTypeLinkNode) metadataField7.type()).name());
    assertNull(metadataField7.defaultValue());
    assertNull(metadataField7.defaultValueStrategy());

    final var metadataField8 = metadataFields.get(8);
    assertEquals("format", metadataField8.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField8.type());
    assertEquals("format", ((DefinedTypeLinkNode) metadataField8.type()).name());
    assertNull(metadataField8.defaultValue());
    assertNull(metadataField8.defaultValueStrategy());

    final var metadataField9 = metadataFields.get(9);
    assertEquals("dataSource", metadataField9.name());
    assertInstanceOf(DefinedTypeLinkNode.class, metadataField9.type());
    assertEquals("dataSource", ((DefinedTypeLinkNode) metadataField9.type()).name());
    assertNull(metadataField9.defaultValue());
    assertNull(metadataField9.defaultValueStrategy());

    final var metadataField10 = metadataFields.get(10);
    assertEquals("dataLength", metadataField10.name());
    assertInstanceOf(PostOffsetTypeNode.class, metadataField10.type());
    final var metadataDataLengthType = (PostOffsetTypeNode) metadataField10.type();
    assertEquals(5, metadataDataLengthType.offset());
    assertEquals(PostOffsetTypeNode.Strategy.padded, metadataDataLengthType.strategy());
    assertInstanceOf(NumberTypeNode.class, metadataDataLengthType.typeNode());
    final var metadataDataLengthNestedType = (NumberTypeNode) metadataDataLengthType.typeNode();
    assertEquals(NumberFormat.u32, metadataDataLengthNestedType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, metadataDataLengthNestedType.endian());
    assertNull(metadataField10.defaultValue());
    assertNull(metadataField10.defaultValueStrategy());

    final var metadataField11 = metadataFields.getLast();
    assertEquals("data", metadataField11.name());
    assertInstanceOf(BytesTypeNode.class, metadataField11.type());
    assertNull(metadataField11.defaultValue());
    assertNull(metadataField11.defaultValueStrategy());

    final var instructions = program.instructions();
    assertNotNull(instructions);
    assertFalse(instructions.isEmpty());
    assertEquals(9, instructions.size());

    final var firstInstruction = instructions.getFirst();
    assertEquals("write", firstInstruction.name());
    assertTrue(firstInstruction.docs().isEmpty());
    assertEquals(InstructionNode.AccountStrategy.programId, firstInstruction.optionalAccountStrategy());

    final var firstInstrAccounts = firstInstruction.accounts();
    assertNotNull(firstInstrAccounts);
    assertEquals(3, firstInstrAccounts.size());

    final var firstAccount = firstInstrAccounts.getFirst();
    assertEquals("buffer", firstAccount.name());
    assertEquals("The buffer to write to.", firstAccount.docs().getFirst());
    assertFalse(firstAccount.isSigner());
    assertTrue(firstAccount.isWritable());
    assertFalse(firstAccount.isOptional());
    assertNull(firstAccount.defaultValue());

    final var lastAccount = firstInstrAccounts.getLast();
    assertEquals("sourceBuffer", lastAccount.name());
    assertEquals(2, lastAccount.docs().size());
    assertEquals("Buffer to copy the data from.", lastAccount.docs().getFirst());
    assertFalse(lastAccount.isSigner());
    assertFalse(lastAccount.isWritable());
    assertTrue(lastAccount.isOptional());
    assertNull(lastAccount.defaultValue());

    final var firstInstrArguments = firstInstruction.arguments();
    assertNotNull(firstInstrArguments);
    assertEquals(3, firstInstrArguments.size());

    final var firstArgument = firstInstrArguments.getFirst();
    assertEquals("discriminator", firstArgument.name());
    assertTrue(firstArgument.docs().isEmpty());
    assertInstanceOf(NumberTypeNode.class, firstArgument.type());
    final var firstArgType = (NumberTypeNode) firstArgument.type();
    assertEquals(NumberFormat.u8, firstArgType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, firstArgType.endian());
    assertEquals(InstructionArgumentNode.Strategy.omitted, firstArgument.defaultValueStrategy());
    assertNotNull(firstArgument.defaultValue());

    final var lastArgument = firstInstrArguments.getLast();
    assertEquals("data", lastArgument.name());
    assertEquals(2, lastArgument.docs().size());
    assertEquals("The data to write at the provided offset.", lastArgument.docs().getFirst());
    assertInstanceOf(RemainderOptionTypeNode.class, lastArgument.type());
    final var lastArgType = (RemainderOptionTypeNode) lastArgument.type();
    assertInstanceOf(BytesTypeNode.class, lastArgType.item());
    assertEquals(InstructionArgumentNode.Strategy.optional, lastArgument.defaultValueStrategy());
    assertNotNull(lastArgument.defaultValue());

    final var firstInstrDiscriminators = firstInstruction.discriminators();
    assertNotNull(firstInstrDiscriminators);
    assertEquals(1, firstInstrDiscriminators.size());

    final var firstDiscriminator = firstInstrDiscriminators.getFirst();
    assertInstanceOf(FieldDiscriminatorNode.class, firstDiscriminator);
    final var fieldDiscriminator = (FieldDiscriminatorNode) firstDiscriminator;
    assertEquals("discriminator", fieldDiscriminator.name());
    assertEquals(0, fieldDiscriminator.offset());

    final var lastInstruction = instructions.getLast();
    assertEquals("extend", lastInstruction.name());
    assertTrue(lastInstruction.docs().isEmpty());
    assertEquals(InstructionNode.AccountStrategy.programId, lastInstruction.optionalAccountStrategy());

    final var lastInstrAccounts = lastInstruction.accounts();
    assertNotNull(lastInstrAccounts);
    assertEquals(4, lastInstrAccounts.size());

    final var lastInstrFirstAccount = lastInstrAccounts.getFirst();
    assertEquals("account", lastInstrFirstAccount.name());
    assertEquals("Buffer or metadata account.", lastInstrFirstAccount.docs().getFirst());
    assertFalse(lastInstrFirstAccount.isSigner());
    assertTrue(lastInstrFirstAccount.isWritable());
    assertFalse(lastInstrFirstAccount.isOptional());
    assertNull(lastInstrFirstAccount.defaultValue());

    final var lastInstrLastAccount = lastInstrAccounts.getLast();
    assertEquals("programData", lastInstrLastAccount.name());
    assertEquals("Program data account.", lastInstrLastAccount.docs().getFirst());
    assertFalse(lastInstrLastAccount.isSigner());
    assertFalse(lastInstrLastAccount.isWritable());
    assertTrue(lastInstrLastAccount.isOptional());
    assertNull(lastInstrLastAccount.defaultValue());

    final var lastInstrArguments = lastInstruction.arguments();
    assertNotNull(lastInstrArguments);
    assertEquals(2, lastInstrArguments.size());

    final var lastInstrFirstArgument = lastInstrArguments.getFirst();
    assertEquals("discriminator", lastInstrFirstArgument.name());
    assertTrue(lastInstrFirstArgument.docs().isEmpty());
    assertInstanceOf(NumberTypeNode.class, lastInstrFirstArgument.type());
    final var lastInstrFirstArgType = (NumberTypeNode) lastInstrFirstArgument.type();
    assertEquals(NumberFormat.u8, lastInstrFirstArgType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, lastInstrFirstArgType.endian());
    assertEquals(InstructionArgumentNode.Strategy.omitted, lastInstrFirstArgument.defaultValueStrategy());
    assertNotNull(lastInstrFirstArgument.defaultValue());

    final var lastInstrLastArgument = lastInstrArguments.getLast();
    assertEquals("length", lastInstrLastArgument.name());
    assertEquals("Length (in bytes) to add to the account size.", lastInstrLastArgument.docs().getFirst());
    assertInstanceOf(NumberTypeNode.class, lastInstrLastArgument.type());
    final var lastInstrLastArgType = (NumberTypeNode) lastInstrLastArgument.type();
    assertEquals(NumberFormat.u16, lastInstrLastArgType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, lastInstrLastArgType.endian());
    assertEquals(InstructionArgumentNode.Strategy.optional, lastInstrLastArgument.defaultValueStrategy());
    assertNull(lastInstrLastArgument.defaultValue());

    final var lastInstrDiscriminators = lastInstruction.discriminators();
    assertNotNull(lastInstrDiscriminators);
    assertEquals(1, lastInstrDiscriminators.size());

    final var lastInstrDiscriminator = lastInstrDiscriminators.getFirst();
    assertInstanceOf(FieldDiscriminatorNode.class, lastInstrDiscriminator);
    final var lastFieldDiscriminator = (FieldDiscriminatorNode) lastInstrDiscriminator;
    assertEquals("discriminator", lastFieldDiscriminator.name());
    assertEquals(0, lastFieldDiscriminator.offset());

    assertTrue(firstInstruction.extraArguments().isEmpty());
    assertTrue(firstInstruction.remainingAccounts().isEmpty());
    assertTrue(firstInstruction.byteDeltas().isEmpty());
    assertTrue(firstInstruction.subInstructions().isEmpty());

    assertTrue(lastInstruction.extraArguments().isEmpty());
    assertTrue(lastInstruction.remainingAccounts().isEmpty());
    assertTrue(lastInstruction.byteDeltas().isEmpty());
    assertTrue(lastInstruction.subInstructions().isEmpty());

    final var definedTypes = program.definedTypes();
    assertNotNull(definedTypes);
    assertFalse(definedTypes.isEmpty());
    assertEquals(7, definedTypes.size());

    final var firstDefinedType = definedTypes.getFirst();
    assertEquals("seed", firstDefinedType.name());
    assertTrue(firstDefinedType.docs().isEmpty());
    assertInstanceOf(FixedSizeTypeNode.class, firstDefinedType.type());
    final var seedType = (FixedSizeTypeNode) firstDefinedType.type();
    assertEquals(16, seedType.size());
    assertInstanceOf(StringTypeNode.class, seedType.typeNode());
    final var seedStringType = (StringTypeNode) seedType.typeNode();
    assertEquals(StringEncoding.utf8, seedStringType.encoding());

    final var lastDefinedType = definedTypes.getLast();
    assertEquals("externalData", lastDefinedType.name());
    assertTrue(lastDefinedType.docs().isEmpty());
    assertInstanceOf(StructTypeNode.class, lastDefinedType.type());
    final var externalDataStruct = (StructTypeNode) lastDefinedType.type();
    final var externalDataFields = externalDataStruct.fields();
    assertEquals(3, externalDataFields.size());

    final var externalDataFirstField = externalDataFields.getFirst();
    assertEquals("address", externalDataFirstField.name());
    assertTrue(externalDataFirstField.docs().isEmpty());
    assertInstanceOf(PublicKeyTypeNode.class, externalDataFirstField.type());
    assertNull(externalDataFirstField.defaultValue());
    assertNull(externalDataFirstField.defaultValueStrategy());

    final var externalDataLastField = externalDataFields.getLast();
    assertEquals("length", externalDataLastField.name());
    assertTrue(externalDataLastField.docs().isEmpty());
    assertInstanceOf(ZeroableOptionTypeNode.class, externalDataLastField.type());
    final var lengthType = (ZeroableOptionTypeNode) externalDataLastField.type();
    assertInstanceOf(NumberTypeNode.class, lengthType.item());
    final var lengthNumberType = (NumberTypeNode) lengthType.item();
    assertEquals(NumberFormat.u32, lengthNumberType.format());
    assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, lengthNumberType.endian());
    assertNull(externalDataLastField.defaultValue());
    assertNull(externalDataLastField.defaultValueStrategy());

    final var errors = program.errors();
    assertNotNull(errors);
    assertTrue(errors.isEmpty());

    final var additionalPrograms = rootNode.additionalPrograms();
    assertNotNull(additionalPrograms);
    assertTrue(additionalPrograms.isEmpty());
  }
}
