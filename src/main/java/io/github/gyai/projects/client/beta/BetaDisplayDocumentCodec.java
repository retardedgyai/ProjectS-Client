package io.github.gyai.projects.client.beta;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class BetaDisplayDocumentCodec {
    private BetaDisplayDocumentCodec() {
    }

    public static BetaDisplayDocument decode(byte[] payload) throws IOException {
        if (payload == null || payload.length > BetaProtocol.PACKET_MAX_BYTES) {
            throw new IOException("Display payload is oversized");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            long revision = input.readLong();
            if (revision < 0) throw new IOException("Negative display revision");
            int statusOrdinal = input.readUnsignedByte();
            if (statusOrdinal >= BetaDisplayDocument.Status.values().length) {
                throw new IOException("Unknown display status");
            }
            String message = readString(input);
            int mapSize = input.readUnsignedShort();
            if (mapSize > 64) throw new IOException("Display map is oversized");
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            for (int index = 0; index < mapSize; index++) {
                String key = readString(input);
                if (fields.putIfAbsent(key, readString(input)) != null) {
                    throw new IOException("Duplicate display field");
                }
            }
            int listSize = input.readUnsignedShort();
            if (listSize > 128) throw new IOException("Display list is oversized");
            ArrayList<String> entries = new ArrayList<>(listSize);
            for (int index = 0; index < listSize; index++) entries.add(readString(input));
            if (input.available() != 0) throw new IOException("Trailing display bytes");
            return new BetaDisplayDocument(revision,
                    BetaDisplayDocument.Status.values()[statusOrdinal], message, fields, entries);
        } catch (EOFException exception) {
            throw new IOException("Truncated display payload", exception);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid display payload", exception);
        }
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > BetaProtocol.STRING_MAX_BYTES || length > input.available()) {
            throw new IOException("Display string is oversized");
        }
        byte[] bytes = input.readNBytes(length);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new IOException("Invalid display UTF-8", exception);
        }
    }
}
