package io.github.gyai.projects.client.beta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BetaProtocolCompatibilityTest {
    private static final UUID SESSION =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUEST =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Pattern VECTOR = Pattern.compile(
            "\\\"name\\\": \\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"classification\\\": \\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"packetHex\\\": \\\"([0-9a-f]+)\\\"");

    private BetaProtocolCompatibilityTest() {
    }

    public static void main(String[] args) throws Exception {
        manifestMatchesClientConstants();
        goldenVectorsMatchClientCodec();
        sessionAndTerminalIsolationUseGoldenPayloads();
        capabilitySpecificBoundsAreEnforced();
    }

    private static void manifestMatchesClientConstants() throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of("docs/protocol/beta-protocol-v1.json"));
        String manifest = new String(bytes, StandardCharsets.UTF_8);
        assert bytes.length > 0 && bytes[bytes.length - 1] == '\n';
        assert manifest.indexOf('\r') < 0;
        assert manifest.equals(canonicalManifest());
        assert BetaProtocol.VERSION == integer(manifest, "aggregateProtocolVersion");
        assert BetaProtocol.CAPABILITIES_CHANNEL.equals(string(manifest, "capabilities"));
        assert BetaProtocol.ACKNOWLEDGEMENT_CHANNEL.equals(string(manifest, "acknowledgement"));
        assert BetaProtocol.STATE_CHANNEL.equals(string(manifest, "state"));
        assert BetaProtocol.COMMAND_CHANNEL.equals(string(manifest, "command"));
        for (BetaProtocol.Capability capability : BetaProtocol.Capability.values()) {
            assert integer(manifest, capability.id()) == 1;
        }
        assert BetaProtocol.HANDSHAKE_MAX_BYTES == integer(manifest, "handshakeBytes");
        assert BetaProtocol.PACKET_MAX_BYTES == integer(manifest, "packetBytes");
        assert BetaProtocol.STRING_MAX_BYTES == integer(manifest, "stringBytes");
        assert BetaProtocol.ID_MAX_BYTES == integer(manifest, "canonicalIdBytes");
        assert BetaProtocol.LIST_MAX_ENTRIES == integer(manifest, "listEntries");
        assert BetaProtocol.MAP_MAX_ENTRIES == integer(manifest, "mapEntries");
        assert BetaProtocol.MOB_EDITOR_LIST_PAGE_MAX_ENTRIES
                == integer(manifest, "mobEditorListPage");
    }

    private static void goldenVectorsMatchClientCodec() throws Exception {
        Map<String, Vector> vectors = loadVectors();
        assert vectors.size() == 13;
        BetaProtocol.Advertisement advertisement = BetaProtocol.decodeAdvertisement(
                packet(vectors, "capability-advertisement")).value();
        assert advertisement.sessionId().equals(SESSION);
        assert advertisement.revision() == 7;
        assert advertisement.capabilities().size() == 7;
        assert Arrays.equals(BetaProtocol.encodeAcknowledgement(
                        advertisement, advertisement.capabilities()),
                packet(vectors, "capability-acknowledgement"));

        verifyState(vectors, "hud-state", BetaProtocol.Capability.HUD,
                1, BetaDisplayDocument.Status.READY, "hud", "level", "12");
        verifyState(vectors, "party-state", BetaProtocol.Capability.PARTY,
                2, BetaDisplayDocument.Status.READY, "party", "party-id",
                "projects:test-party");
        verifyState(vectors, "element-state", BetaProtocol.Capability.ELEMENTS,
                3, BetaDisplayDocument.Status.READY, "elements", "fire-gauge", "0.5");
        verifyState(vectors, "equipment-state", BetaProtocol.Capability.EQUIPMENT,
                4, BetaDisplayDocument.Status.READY, "equipment", "tier", "T1");
        verifyState(vectors, "crafting-unavailable-balance-state",
                BetaProtocol.Capability.CRAFTING, 5, BetaDisplayDocument.Status.UNSUPPORTED,
                "UNAVAILABLE_BALANCE_DATA", "preview-status", "UNAVAILABLE_BALANCE_DATA");
        verifyState(vectors, "enhancement-unavailable-balance-state",
                BetaProtocol.Capability.ENHANCEMENT, 6,
                BetaDisplayDocument.Status.UNSUPPORTED, "UNAVAILABLE_BALANCE_DATA",
                "preview-status", "UNAVAILABLE_BALANCE_DATA");
        BetaProtocol.Envelope listEnvelope = BetaProtocol.decodeState(
                packet(vectors, "mob-editor-list-page")).value();
        BetaDisplayDocument list = BetaDisplayDocumentCodec.decode(listEnvelope.payload());
        assert listEnvelope.capability() == BetaProtocol.Capability.MOB_EDITOR_V2;
        assert list.entries().equals(List.of("projects:test-mob", "projects:test-boss"));
        BetaProtocol.Envelope conflictEnvelope = BetaProtocol.decodeState(
                packet(vectors, "mob-editor-conflict-result")).value();
        BetaDisplayDocument conflict = BetaDisplayDocumentCodec.decode(conflictEnvelope.payload());
        assert conflictEnvelope.kind() == BetaProtocol.Kind.COMMAND_RESULT;
        assert conflict.status() == BetaDisplayDocument.Status.CONFLICT;
        assert conflict.fields().get("current-revision").equals("8");

        verifyCommand(vectors, "valid-command", 8);
        verifyCommand(vectors, "stale-revision-command", 7);
        assert BetaProtocol.decodeState(packet(vectors, "malformed-trailing-byte")).status()
                == BetaProtocol.DecodeStatus.MALFORMED;

        byte[] unknown = packet(vectors, "hud-state").clone();
        int offset = indexOf(unknown, "projects:hud".getBytes(StandardCharsets.UTF_8));
        assert offset >= 0;
        System.arraycopy("projects:bad".getBytes(StandardCharsets.UTF_8), 0,
                unknown, offset, "projects:bad".length());
        assert BetaProtocol.decodeState(unknown).status()
                == BetaProtocol.DecodeStatus.UNKNOWN_CAPABILITY;
    }

    private static void verifyState(
            Map<String, Vector> vectors,
            String name,
            BetaProtocol.Capability capability,
            long revision,
            BetaDisplayDocument.Status status,
            String message,
            String field,
            String value
    ) throws IOException {
        BetaProtocol.Envelope envelope = BetaProtocol.decodeState(packet(vectors, name)).value();
        assert envelope.kind() == BetaProtocol.Kind.STATE;
        assert envelope.capability() == capability;
        assert envelope.requestOrSessionId().equals(SESSION);
        BetaDisplayDocument document = BetaDisplayDocumentCodec.decode(envelope.payload());
        assert document.revision() == revision;
        assert document.status() == status;
        assert document.message().equals(message);
        assert document.fields().get(field).equals(value);
    }

    private static void verifyCommand(
            Map<String, Vector> vectors,
            String name,
            long targetRevision
    ) {
        BetaProtocol.Command command = new BetaProtocol.Command(
                new BetaProtocol.Envelope(BetaProtocol.Kind.COMMAND,
                        BetaProtocol.Capability.MOB_EDITOR_V2, 1, SESSION,
                        "save".getBytes(StandardCharsets.UTF_8)),
                7, targetRevision, REQUEST);
        assert Arrays.equals(BetaProtocol.encodeCommand(command), packet(vectors, name));
    }

    private static void sessionAndTerminalIsolationUseGoldenPayloads() throws Exception {
        Map<String, Vector> vectors = loadVectors();
        BetaProtocol.Advertisement advertisement = BetaProtocol.decodeAdvertisement(
                packet(vectors, "capability-advertisement")).value();
        BetaClientConnectionState connection = new BetaClientConnectionState();
        connection.accept(advertisement);
        BetaProtocol.Envelope hud = BetaProtocol.decodeState(packet(vectors, "hud-state")).value();
        assert connection.receive(hud);
        BetaProtocol.Envelope delayed = new BetaProtocol.Envelope(
                hud.kind(), hud.capability(), hud.payloadVersion(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"), hud.payload());
        assert !connection.receive(delayed);

        BetaProtocol.Command pending = connection.command(
                BetaProtocol.Capability.MOB_EDITOR_V2, 8,
                "save".getBytes(StandardCharsets.UTF_8)).orElseThrow();
        BetaProtocol.Envelope goldenConflict = BetaProtocol.decodeState(
                packet(vectors, "mob-editor-conflict-result")).value();
        BetaProtocol.Envelope terminal = new BetaProtocol.Envelope(
                BetaProtocol.Kind.COMMAND_RESULT, BetaProtocol.Capability.MOB_EDITOR_V2,
                1, pending.idempotencyRequestId(), goldenConflict.payload());
        assert connection.receive(terminal);
        assert !connection.receive(terminal);
        assert connection.session().terminal(pending.idempotencyRequestId()).isPresent();
    }

    private static void capabilitySpecificBoundsAreEnforced() throws IOException {
        BetaClientConnectionState connection = new BetaClientConnectionState();
        List<BetaProtocol.Descriptor> descriptors = Arrays.stream(BetaProtocol.Capability.values())
                .map(value -> new BetaProtocol.Descriptor(value, 1)).toList();
        connection.accept(new BetaProtocol.Advertisement(SESSION, 7, descriptors));
        ArrayList<String> entries = new ArrayList<>();
        for (int index = 0; index <= BetaProtocol.MOB_EDITOR_LIST_PAGE_MAX_ENTRIES; index++) {
            entries.add("projects:mob-" + index);
        }
        byte[] oversizedPage = document(9, BetaDisplayDocument.Status.READY.ordinal(),
                "page", Map.of(), entries);
        assert !connection.receive(new BetaProtocol.Envelope(
                BetaProtocol.Kind.STATE, BetaProtocol.Capability.MOB_EDITOR_V2,
                1, SESSION, oversizedPage));
    }

    private static Map<String, Vector> loadVectors() throws IOException {
        String json = Files.readString(Path.of(
                "client-core/src/test/resources/protocol/beta-protocol-v1-vectors.json"),
                StandardCharsets.UTF_8);
        assert json.indexOf('\r') < 0 && json.endsWith("\n");
        LinkedHashMap<String, Vector> result = new LinkedHashMap<>();
        Matcher matcher = VECTOR.matcher(json);
        while (matcher.find()) {
            Vector vector = new Vector(matcher.group(2),
                    HexFormat.of().parseHex(matcher.group(3)));
            assert result.putIfAbsent(matcher.group(1), vector) == null;
        }
        return Map.copyOf(result);
    }

    private static byte[] document(
            long revision,
            int status,
            String message,
            Map<String, String> fields,
            List<String> entries
    ) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeLong(revision);
            output.writeByte(status);
            writeString(output, message);
            output.writeShort(fields.size());
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                writeString(output, entry.getKey());
                writeString(output, entry.getValue());
            }
            output.writeShort(entries.size());
            for (String entry : entries) writeString(output, entry);
        }
        return bytes.toByteArray();
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static byte[] packet(Map<String, Vector> vectors, String name) {
        return vectors.get(name).packet().clone();
    }

    private static int indexOf(byte[] source, byte[] target) {
        outer: for (int index = 0; index <= source.length - target.length; index++) {
            for (int offset = 0; offset < target.length; offset++) {
                if (source[index + offset] != target[offset]) continue outer;
            }
            return index;
        }
        return -1;
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        assert matcher.find() : key;
        return matcher.group(1);
    }

    private static int integer(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        assert matcher.find() : key;
        return Integer.parseInt(matcher.group(1));
    }

    private static String canonicalManifest() {
        return """
                {
                  "aggregateProtocolVersion": 1,
                  "channels": {
                    "capabilities": "projects:beta_caps_v1",
                    "acknowledgement": "projects:beta_ack_v1",
                    "state": "projects:beta_state_v1",
                    "command": "projects:beta_command_v1"
                  },
                  "capabilities": {
                    "projects:hud": 1,
                    "projects:party": 1,
                    "projects:elements": 1,
                    "projects:equipment": 1,
                    "projects:crafting": 1,
                    "projects:enhancement": 1,
                    "projects:mob-editor-v2": 1
                  },
                  "bounds": {
                    "handshakeBytes": 8192,
                    "packetBytes": 32768,
                    "stringBytes": 256,
                    "canonicalIdBytes": 128,
                    "listEntries": 128,
                    "mapEntries": 64,
                    "mobEditorListPage": 50
                  },
                  "opcodes": {
                    "advertisement": 1,
                    "acknowledgement": 2,
                    "state": 3,
                    "command": 4,
                    "commandResult": 5
                  }
                }
                """;
    }

    private record Vector(String classification, byte[] packet) {
        private Vector {
            packet = packet.clone();
        }

        @Override public byte[] packet() {
            return packet.clone();
        }
    }
}
