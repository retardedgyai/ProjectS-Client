package io.github.gyai.projects.client.beta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BetaProtocol {
    public static final int VERSION = 1;
    public static final int HANDSHAKE_MAX_BYTES = 8 * 1024;
    public static final int PACKET_MAX_BYTES = 32 * 1024;
    public static final int STRING_MAX_BYTES = 256;
    public static final int ID_MAX_BYTES = 128;
    public static final int LIST_MAX_ENTRIES = 128;
    public static final int MAP_MAX_ENTRIES = 64;
    public static final int MOB_EDITOR_LIST_PAGE_MAX_ENTRIES = 50;

    public static final String CAPABILITIES_CHANNEL = "projects:beta_caps_v1";
    public static final String ACKNOWLEDGEMENT_CHANNEL = "projects:beta_ack_v1";
    public static final String STATE_CHANNEL = "projects:beta_state_v1";
    public static final String COMMAND_CHANNEL = "projects:beta_command_v1";

    private static final int ADVERTISEMENT = 1;
    private static final int ACKNOWLEDGEMENT = 2;
    private static final int STATE = 3;
    private static final int COMMAND = 4;
    private static final int COMMAND_RESULT = 5;

    private BetaProtocol() {
    }

    public enum Capability {
        HUD("projects:hud"),
        PARTY("projects:party"),
        ELEMENTS("projects:elements"),
        EQUIPMENT("projects:equipment"),
        CRAFTING("projects:crafting"),
        ENHANCEMENT("projects:enhancement"),
        MOB_EDITOR_V2("projects:mob-editor-v2");

        private final String id;

        Capability(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Optional<Capability> fromId(String id) {
            return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
        }
    }

    public enum Kind { STATE, COMMAND, COMMAND_RESULT }

    public enum DecodeStatus {
        SUCCESS, MALFORMED, OVERSIZED, UNSUPPORTED_VERSION, UNKNOWN_CAPABILITY, UNKNOWN_OPCODE
    }

    public record Descriptor(Capability capability, int payloadVersion) {
        public Descriptor {
            if (capability == null || payloadVersion <= 0) {
                throw new IllegalArgumentException("Invalid capability descriptor");
            }
        }
    }

    public record Advertisement(UUID sessionId, long revision, List<Descriptor> capabilities) {
        public Advertisement {
            if (sessionId == null || revision < 0) throw new IllegalArgumentException("Invalid advertisement");
            capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
            if (capabilities.size() > Capability.values().length
                    || capabilities.stream().map(Descriptor::capability).distinct().count()
                    != capabilities.size()) {
                throw new IllegalArgumentException("Duplicate or excessive capabilities");
            }
        }
    }

    public record Envelope(
            Kind kind,
            Capability capability,
            int payloadVersion,
            UUID requestOrSessionId,
            byte[] payload
    ) {
        public Envelope {
            if (kind == null || capability == null || payloadVersion <= 0
                    || requestOrSessionId == null) {
                throw new IllegalArgumentException("Invalid envelope");
            }
            payload = payload == null ? new byte[0] : payload.clone();
            if (payload.length > PACKET_MAX_BYTES) throw new IllegalArgumentException("Payload is oversized");
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    public record Command(
            Envelope message,
            long playerSessionRevision,
            long targetContentRevision,
            UUID idempotencyRequestId
    ) {
        public Command {
            if (message == null || message.kind() != Kind.COMMAND
                    || playerSessionRevision < 0 || targetContentRevision < 0
                    || idempotencyRequestId == null) {
                throw new IllegalArgumentException("Invalid command");
            }
        }
    }

    public record DecodeResult<T>(DecodeStatus status, T value, String detail) {
        public DecodeResult {
            if (status == null) throw new IllegalArgumentException("Status is required");
            detail = detail == null ? "" : detail;
            if (detail.length() > 256) detail = detail.substring(0, 256);
        }

        public boolean successful() {
            return status == DecodeStatus.SUCCESS && value != null;
        }
    }

    public static DecodeResult<Advertisement> decodeAdvertisement(byte[] packet) {
        if (packet == null) return failure(DecodeStatus.MALFORMED, "Missing packet");
        if (packet.length > HANDSHAKE_MAX_BYTES) return failure(DecodeStatus.OVERSIZED, "Oversized packet");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet))) {
            int version = input.readUnsignedByte();
            if (version != VERSION) return failure(DecodeStatus.UNSUPPORTED_VERSION, "Unsupported version");
            if (input.readUnsignedByte() != ADVERTISEMENT) {
                return failure(DecodeStatus.UNKNOWN_OPCODE, "Unknown opcode");
            }
            UUID session = readUuid(input);
            int payloadLength = input.readInt();
            if (payloadLength < 0) return failure(DecodeStatus.MALFORMED, "Negative payload length");
            if (payloadLength > HANDSHAKE_MAX_BYTES || payloadLength > input.available()) {
                return failure(DecodeStatus.OVERSIZED, "Oversized payload");
            }
            byte[] payloadBytes = input.readNBytes(payloadLength);
            if (input.available() != 0) return failure(DecodeStatus.MALFORMED, "Trailing bytes");
            DataInputStream payload = new DataInputStream(new ByteArrayInputStream(payloadBytes));
            long revision = payload.readLong();
            if (revision < 0) return failure(DecodeStatus.MALFORMED, "Negative revision");
            int count = payload.readUnsignedShort();
            if (count > Capability.values().length || count > LIST_MAX_ENTRIES) {
                return failure(DecodeStatus.OVERSIZED, "Too many capabilities");
            }
            ArrayList<Descriptor> capabilities = new ArrayList<>(count);
            HashSet<Capability> ids = new HashSet<>();
            for (int index = 0; index < count; index++) {
                Capability capability = Capability.fromId(readString(payload, ID_MAX_BYTES)).orElse(null);
                if (capability == null) return failure(DecodeStatus.UNKNOWN_CAPABILITY, "Unknown capability");
                if (!ids.add(capability)) return failure(DecodeStatus.MALFORMED, "Duplicate capability");
                int payloadVersion = payload.readInt();
                if (payloadVersion <= 0) return failure(DecodeStatus.MALFORMED, "Invalid payload version");
                capabilities.add(new Descriptor(capability, payloadVersion));
            }
            if (payload.available() != 0) return failure(DecodeStatus.MALFORMED, "Trailing payload bytes");
            return success(new Advertisement(session, revision, capabilities));
        } catch (EOFException exception) {
            return failure(DecodeStatus.MALFORMED, "Truncated packet");
        } catch (IOException | IllegalArgumentException exception) {
            return failure(DecodeStatus.MALFORMED, exception.getMessage());
        }
    }

    public static byte[] encodeAcknowledgement(Advertisement advertisement, List<Descriptor> supported) {
        List<Descriptor> exact = List.copyOf(supported == null ? List.of() : supported);
        if (!advertisement.capabilities().containsAll(exact)) {
            throw new IllegalArgumentException("Cannot acknowledge unadvertised capability");
        }
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            try (DataOutputStream payload = new DataOutputStream(payloadBytes)) {
                payload.writeLong(advertisement.revision());
                payload.writeShort(exact.size());
                for (Descriptor descriptor : exact) {
                    writeString(payload, descriptor.capability().id(), ID_MAX_BYTES);
                    payload.writeInt(descriptor.payloadVersion());
                }
            }
            byte[] encodedPayload = payloadBytes.toByteArray();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeByte(VERSION);
                output.writeByte(ACKNOWLEDGEMENT);
                writeUuid(output, advertisement.sessionId());
                output.writeInt(encodedPayload.length);
                output.write(encodedPayload);
            }
            return bounded(bytes.toByteArray(), HANDSHAKE_MAX_BYTES);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot encode acknowledgement", exception);
        }
    }

    public static DecodeResult<Envelope> decodeState(byte[] packet) {
        if (packet == null) return failure(DecodeStatus.MALFORMED, "Missing packet");
        if (packet.length > PACKET_MAX_BYTES) return failure(DecodeStatus.OVERSIZED, "Oversized packet");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet))) {
            int version = input.readUnsignedByte();
            if (version != VERSION) return failure(DecodeStatus.UNSUPPORTED_VERSION, "Unsupported version");
            int opcode = input.readUnsignedByte();
            Kind kind = opcode == STATE ? Kind.STATE : opcode == COMMAND_RESULT ? Kind.COMMAND_RESULT : null;
            if (kind == null) return failure(DecodeStatus.UNKNOWN_OPCODE, "Unknown opcode");
            Capability capability = Capability.fromId(readString(input, ID_MAX_BYTES)).orElse(null);
            if (capability == null) return failure(DecodeStatus.UNKNOWN_CAPABILITY, "Unknown capability");
            int payloadVersion = input.readInt();
            if (payloadVersion <= 0) return failure(DecodeStatus.MALFORMED, "Invalid payload version");
            UUID request = readUuid(input);
            int length = input.readInt();
            if (length < 0) return failure(DecodeStatus.MALFORMED, "Negative payload length");
            if (length > PACKET_MAX_BYTES || length > input.available()) {
                return failure(DecodeStatus.OVERSIZED, "Oversized payload");
            }
            byte[] payload = input.readNBytes(length);
            if (input.available() != 0) return failure(DecodeStatus.MALFORMED, "Trailing bytes");
            return success(new Envelope(kind, capability, payloadVersion, request, payload));
        } catch (EOFException exception) {
            return failure(DecodeStatus.MALFORMED, "Truncated packet");
        } catch (IOException | IllegalArgumentException exception) {
            return failure(DecodeStatus.MALFORMED, exception.getMessage());
        }
    }

    public static byte[] encodeCommand(Command command) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeByte(VERSION);
                output.writeByte(COMMAND);
                writeString(output, command.message().capability().id(), ID_MAX_BYTES);
                output.writeInt(command.message().payloadVersion());
                writeUuid(output, command.message().requestOrSessionId());
                output.writeLong(command.playerSessionRevision());
                output.writeLong(command.targetContentRevision());
                writeUuid(output, command.idempotencyRequestId());
                byte[] payload = command.message().payload();
                output.writeInt(payload.length);
                output.write(payload);
            }
            return bounded(bytes.toByteArray(), PACKET_MAX_BYTES);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot encode command", exception);
        }
    }

    private static String readString(DataInputStream input, int maximumBytes) throws IOException {
        int length = input.readUnsignedShort();
        if (length > maximumBytes || length > input.available()) throw new IOException("Oversized string");
        byte[] bytes = input.readNBytes(length);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new IOException("Invalid UTF-8", exception);
        }
    }

    private static void writeString(DataOutputStream output, String value, int maximumBytes)
            throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes) throw new IOException("Oversized string");
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static void writeUuid(DataOutputStream output, UUID value) throws IOException {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }

    private static byte[] bounded(byte[] bytes, int maximum) {
        if (bytes.length > maximum) throw new IllegalArgumentException("Packet is oversized");
        return bytes;
    }

    private static <T> DecodeResult<T> success(T value) {
        return new DecodeResult<>(DecodeStatus.SUCCESS, value, "");
    }

    private static <T> DecodeResult<T> failure(DecodeStatus status, String detail) {
        return new DecodeResult<>(status, null, detail);
    }
}
