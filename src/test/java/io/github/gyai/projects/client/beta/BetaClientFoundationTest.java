package io.github.gyai.projects.client.beta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BetaClientFoundationTest {
    private BetaClientFoundationTest() {
    }

    public static void main(String[] args) throws Exception {
        constantsMatchServerContract();
        handshakeAndCommandAreDeterministic();
        malformedPacketsFailClosed();
        stateStoresAreRevisionedAndBounded();
        uiModelsExposeLoadingErrorsAndConflicts();
        lifecycleAndCompatibilityFallbacksAreSafe();
    }

    private static void constantsMatchServerContract() {
        assert BetaProtocol.VERSION == 1;
        assert BetaProtocol.CAPABILITIES_CHANNEL.equals("projects:beta_caps_v1");
        assert BetaProtocol.ACKNOWLEDGEMENT_CHANNEL.equals("projects:beta_ack_v1");
        assert BetaProtocol.STATE_CHANNEL.equals("projects:beta_state_v1");
        assert BetaProtocol.COMMAND_CHANNEL.equals("projects:beta_command_v1");
        assert Arrays.stream(BetaProtocol.Capability.values())
                .map(BetaProtocol.Capability::id).toList().equals(List.of(
                        "projects:hud", "projects:party", "projects:elements",
                        "projects:equipment", "projects:crafting", "projects:enhancement",
                        "projects:mob-editor-v2"));
        assert BetaProtocol.HANDSHAKE_MAX_BYTES == 8 * 1024;
        assert BetaProtocol.PACKET_MAX_BYTES == 32 * 1024;
        assert BetaProtocol.STRING_MAX_BYTES == 256;
        assert BetaProtocol.ID_MAX_BYTES == 128;
        assert BetaProtocol.LIST_MAX_ENTRIES == 128;
    }

    private static void handshakeAndCommandAreDeterministic() throws Exception {
        UUID session = UUID.fromString("00000000-0000-0000-0000-000000000001");
        byte[] packet = advertisement(session, 7,
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1),
                        new BetaProtocol.Descriptor(BetaProtocol.Capability.PARTY, 2)));
        BetaProtocol.DecodeResult<BetaProtocol.Advertisement> decoded =
                BetaProtocol.decodeAdvertisement(packet);
        assert decoded.successful();
        BetaClientSession client = new BetaClientSession();
        byte[] first = client.accept(decoded.value());
        byte[] second = BetaProtocol.encodeAcknowledgement(decoded.value(),
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1)));
        assert Arrays.equals(first, second);
        assert client.supports(BetaProtocol.Capability.HUD);
        assert !client.supports(BetaProtocol.Capability.PARTY);
        BetaProtocol.Command command = client.command(
                BetaProtocol.Capability.HUD, 11, new byte[] {1, 2}).orElseThrow();
        byte[] encoded = BetaProtocol.encodeCommand(command);
        assert encoded[0] == 1;
        assert encoded[1] == 4;
        assert command.playerSessionRevision() == 7;
        assert command.targetContentRevision() == 11;
        assert client.command(BetaProtocol.Capability.PARTY, 0, new byte[0]).isEmpty();
    }

    private static void malformedPacketsFailClosed() throws Exception {
        byte[] valid = advertisement(UUID.randomUUID(), 1,
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1)));
        byte[] wrongVersion = valid.clone();
        wrongVersion[0] = 2;
        assert BetaProtocol.decodeAdvertisement(wrongVersion).status()
                == BetaProtocol.DecodeStatus.UNSUPPORTED_VERSION;
        byte[] opcode = valid.clone();
        opcode[1] = 99;
        assert BetaProtocol.decodeAdvertisement(opcode).status()
                == BetaProtocol.DecodeStatus.UNKNOWN_OPCODE;
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        assert BetaProtocol.decodeAdvertisement(trailing).status()
                == BetaProtocol.DecodeStatus.MALFORMED;
        byte[] duplicate = advertisement(UUID.randomUUID(), 1,
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1),
                        new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1)));
        assert BetaProtocol.decodeAdvertisement(duplicate).status()
                == BetaProtocol.DecodeStatus.MALFORMED;
        byte[] unknown = valid.clone();
        byte[] replacement = "unknowns:hud".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(replacement, 0, unknown, 34, replacement.length);
        assert BetaProtocol.decodeAdvertisement(unknown).status()
                == BetaProtocol.DecodeStatus.UNKNOWN_CAPABILITY;
        byte[] invalidUtf8 = valid.clone();
        invalidUtf8[34] = (byte) 0xC3;
        invalidUtf8[35] = 0x28;
        assert BetaProtocol.decodeAdvertisement(invalidUtf8).status()
                == BetaProtocol.DecodeStatus.MALFORMED;
        assert BetaProtocol.decodeAdvertisement(
                new byte[BetaProtocol.HANDSHAKE_MAX_BYTES + 1]).status()
                == BetaProtocol.DecodeStatus.OVERSIZED;
        for (int size = 0; size < 256; size++) {
            byte[] fuzz = new byte[size];
            new java.util.Random(size).nextBytes(fuzz);
            assert BetaProtocol.decodeAdvertisement(fuzz) != null;
            assert BetaProtocol.decodeState(fuzz) != null;
        }
        assertThrows(() -> new BetaDisplayDocument(1, BetaDisplayDocument.Status.READY, "",
                Map.of("fire-gauge", "NaN"), List.of()));
        assertThrows(() -> new BetaDisplayDocument(1, BetaDisplayDocument.Status.READY, "",
                Map.of("cold-gauge", "Infinity"), List.of()));
    }

    private static void stateStoresAreRevisionedAndBounded() throws Exception {
        UUID sessionId = UUID.randomUUID();
        BetaProtocol.Advertisement advertisement = BetaProtocol.decodeAdvertisement(
                advertisement(sessionId, 3,
                        List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1))))
                .value();
        BetaClientSession session = new BetaClientSession();
        session.accept(advertisement);
        BetaUiStateStores stores = new BetaUiStateStores();
        BetaProtocol.Envelope newer = BetaProtocol.decodeState(statePacket(
                BetaProtocol.Capability.HUD, sessionId,
                document(2, BetaDisplayDocument.Status.READY, "ok",
                        Map.of("level", "4"), List.of()))).value();
        assert stores.receive(newer, session);
        assert stores.hud().revision() == 2;
        BetaProtocol.Envelope stale = BetaProtocol.decodeState(statePacket(
                BetaProtocol.Capability.HUD, sessionId,
                document(1, BetaDisplayDocument.Status.READY, "stale",
                        Map.of("level", "3"), List.of()))).value();
        assert !stores.receive(stale, session);
        assert stores.hud().message().equals("ok");
        byte[] duplicateMap = documentWithDuplicateMap();
        BetaProtocol.Envelope invalid = BetaProtocol.decodeState(statePacket(
                BetaProtocol.Capability.HUD, sessionId, duplicateMap)).value();
        assert !stores.receive(invalid, session);
        assert stores.hud().status() == BetaDisplayDocument.Status.ERROR;
        BetaProtocol.Envelope terminal = BetaProtocol.decodeState(statePacket(
                5, BetaProtocol.Capability.HUD, UUID.randomUUID(),
                document(3, BetaDisplayDocument.Status.TERMINAL, "done", Map.of(), List.of())))
                .value();
        assert stores.receive(terminal, session);
        assert session.terminal(terminal.requestOrSessionId()).orElseThrow().message().equals("done");
    }

    private static void uiModelsExposeLoadingErrorsAndConflicts() {
        BetaUiViewModels.Panel loading = BetaUiViewModels.hud(BetaDisplayDocument.loading());
        assert loading.status() == BetaDisplayDocument.Status.LOADING;
        BetaDisplayDocument conflict = new BetaDisplayDocument(
                5, BetaDisplayDocument.Status.CONFLICT, "revision conflict",
                Map.of("base-revision", "4"), List.of("reload required"));
        BetaUiViewModels.Panel editor = BetaUiViewModels.mobEditorV2Screen(conflict);
        assert editor.revisionConflict();
        assert editor.retryAllowed();
        BetaDisplayDocument terminal = new BetaDisplayDocument(
                6, BetaDisplayDocument.Status.RETRY_FORBIDDEN, "commit uncertain",
                Map.of(), List.of());
        assert !BetaUiViewModels.craftingScreen(terminal).retryAllowed();
        assert BetaUiViewModels.party(BetaDisplayDocument.unsupported("old server"))
                .status() == BetaDisplayDocument.Status.UNSUPPORTED;
        assert BetaUiViewModels.elementTargetOverlay(BetaDisplayDocument.loading()) != null;
        assert BetaUiViewModels.equipmentDetail(BetaDisplayDocument.loading()) != null;
        assert BetaUiViewModels.enhancementScreen(BetaDisplayDocument.loading()) != null;
    }

    private static void lifecycleAndCompatibilityFallbacksAreSafe() throws Exception {
        BetaClientSession session = new BetaClientSession();
        assert session.oldServerFallback(); // old server / new client
        session.accept(BetaProtocol.decodeAdvertisement(advertisement(
                UUID.randomUUID(), 1,
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1)))).value());
        assert !session.oldServerFallback(); // new server / new client
        session.clear(); // disconnect/reconnect cleanup
        session.clear();
        assert session.oldServerFallback();
        // Unsupported capability version is not acknowledged and existing UI remains available.
        session.accept(BetaProtocol.decodeAdvertisement(advertisement(
                UUID.randomUUID(), 2,
                List.of(new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 2)))).value());
        assert !session.supports(BetaProtocol.Capability.HUD);
    }

    private static byte[] advertisement(
            UUID session,
            long revision,
            List<BetaProtocol.Descriptor> capabilities
    ) throws Exception {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        try (DataOutputStream payload = new DataOutputStream(payloadBytes)) {
            payload.writeLong(revision);
            payload.writeShort(capabilities.size());
            for (BetaProtocol.Descriptor descriptor : capabilities) {
                writeString(payload, descriptor.capability().id());
                payload.writeInt(descriptor.payloadVersion());
            }
        }
        byte[] encodedPayload = payloadBytes.toByteArray();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(1);
            output.writeByte(1);
            writeUuid(output, session);
            output.writeInt(encodedPayload.length);
            output.write(encodedPayload);
        }
        return bytes.toByteArray();
    }

    private static byte[] statePacket(
            BetaProtocol.Capability capability,
            UUID request,
            byte[] payload
    ) throws Exception {
        return statePacket(3, capability, request, payload);
    }

    private static byte[] statePacket(
            int opcode,
            BetaProtocol.Capability capability,
            UUID request,
            byte[] payload
    ) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(1);
            output.writeByte(opcode);
            writeString(output, capability.id());
            output.writeInt(1);
            writeUuid(output, request);
            output.writeInt(payload.length);
            output.write(payload);
        }
        return bytes.toByteArray();
    }

    private static byte[] document(
            long revision,
            BetaDisplayDocument.Status status,
            String message,
            Map<String, String> fields,
            List<String> entries
    ) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeLong(revision);
            output.writeByte(status.ordinal());
            writeString(output, message);
            output.writeShort(fields.size());
            for (var entry : fields.entrySet()) {
                writeString(output, entry.getKey());
                writeString(output, entry.getValue());
            }
            output.writeShort(entries.size());
            for (String entry : entries) writeString(output, entry);
        }
        return bytes.toByteArray();
    }

    private static byte[] documentWithDuplicateMap() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeLong(3);
            output.writeByte(BetaDisplayDocument.Status.READY.ordinal());
            writeString(output, "duplicate");
            output.writeShort(2);
            writeString(output, "level");
            writeString(output, "1");
            writeString(output, "level");
            writeString(output, "2");
            output.writeShort(0);
        }
        return bytes.toByteArray();
    }

    private static void writeString(DataOutputStream output, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static void writeUuid(DataOutputStream output, UUID value) throws Exception {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    private static void assertThrows(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
