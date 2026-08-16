package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Appearance envelope over the canonical v1 editor body.  Shape/scalar parsing remains
 * owned by {@link SkillVfxEditorProtocol}; this class only validates and patches tables.
 */
public final class SkillVfxEditorProtocolV2 {
    public static final int VERSION=2, MAX_PACKET=SkillVfxEditorProtocol.MAX_PACKET, MAX_STRING=SkillVfxEditorProtocol.MAX_STRING;
    private static final int MAX_PRIMITIVE_TABLE=SkillVfxEditorProtocol.MAX_HOOKS*SkillVfxEditorProtocol.MAX_EMISSIONS*SkillVfxEditorProtocol.MAX_PRIMITIVES;
    private SkillVfxEditorProtocolV2() { }

    public static byte[] encodeRequest(SkillVfxEditorProtocol.Request request) {
        if(request==null) throw bad("request");
        if(request.visualBytes()!=null)throw bad("appearance source required");
        return encodeRequest(request,null);
    }
    /** The authored visual supplies the v2 table; v1 bytes still supply canonical shape/scalar fields. */
    public static byte[] encodeRequest(SkillVfxEditorProtocol.Request request,SkillVfxModel.Visual visual) {
        if(request==null||(request.visualBytes()==null)!=(visual==null))throw bad("request visual");
        if(visual!=null&&!primitiveIds(SkillVfxEditorProtocol.decodeVisual(request.visualBytes())).equals(primitiveIds(visual)))throw bad("request primitive ids");
        return frame(SkillVfxEditorProtocol.encodeRequest(request),visual==null?List.of():List.of(visual));
    }
    public static SkillVfxEditorProtocol.Request decodeRequest(byte[] bytes) {
        Frame frame=unframe(bytes); SkillVfxEditorProtocol.Request request=SkillVfxEditorProtocol.decodeRequest(frame.body());
        if(request.visualBytes()==null) { if(!frame.tables().isEmpty()) throw bad("request tables"); return request; }
        SkillVfxModel.Visual patched=decodeRequestVisual(bytes);
        return new SkillVfxEditorProtocol.Request(request.operation(),request.correlation(),request.session(),request.abilityId(),request.revision(),request.baseFingerprint(),request.effectiveFingerprint(),SkillVfxEditorProtocol.encodeVisual(patched));
    }
    /** Appearance-aware request decode mirrors the server v2 contract for executable client tests. */
    public static SkillVfxModel.Visual decodeRequestVisual(byte[] bytes) {
        Frame frame=unframe(bytes);SkillVfxEditorProtocol.Request request=SkillVfxEditorProtocol.decodeRequest(frame.body());if(request.visualBytes()==null||frame.tables().size()!=1)throw bad("request tables");return patch(SkillVfxEditorProtocol.decodeVisual(request.visualBytes()),frame.tables().getFirst());
    }
    public static byte[] encodeState(SkillVfxEditorProtocol.State state) {
        if(state==null||state.canonical()==null) throw bad("state");
        List<SkillVfxModel.Visual> visuals=state.snapshot()==null?List.of():List.of(state.snapshot().base(),state.snapshot().effective());
        return frame(SkillVfxEditorProtocol.encodeState(state),visuals);
    }
    public static SkillVfxEditorProtocol.State decodeState(byte[] bytes) {
        Frame frame=unframe(bytes); SkillVfxEditorProtocol.State state=SkillVfxEditorProtocol.decodeState(frame.body());
        if(state.snapshot()==null) { if(!frame.tables().isEmpty()) throw bad("state tables"); return state; }
        if(frame.tables().size()!=2) throw bad("state tables");
        SkillVfxEditorProtocol.Snapshot old=state.snapshot();
        SkillVfxEditorProtocol.Snapshot snapshot=new SkillVfxEditorProtocol.Snapshot(old.abilityId(),old.displayName(),old.gameplay(),old.visualId(),patch(old.base(),frame.tables().get(0)),patch(old.effective(),frame.tables().get(1)),old.revision(),old.baseFingerprint(),old.effectiveFingerprint(),old.sessionOverride());
        return new SkillVfxEditorProtocol.State(state.status(),state.correlation(),state.session(),state.catalog(),snapshot,state.previewAllowed(),state.message(),frame.body());
    }
    public static byte[] encodeVisual(SkillVfxModel.Visual visual) { return frame(SkillVfxEditorProtocol.encodeVisual(visual),List.of(visual)); }
    public static SkillVfxModel.Visual decodeVisual(byte[] bytes) { Frame frame=unframe(bytes); if(frame.tables().size()!=1) throw bad("visual tables"); return patch(SkillVfxEditorProtocol.decodeVisual(frame.body()),frame.tables().getFirst()); }

    private record Frame(byte[] body,List<Map<String,SkillVfxModel.Appearance>> tables) { }
    private static byte[] frame(byte[] body,List<SkillVfxModel.Visual> visuals) {
        if(body==null||body.length>65535||visuals.size()>2) throw bad("body");
        return out(output -> { output.writeByte(VERSION);output.writeShort(body.length);output.write(body);output.writeByte(visuals.size());for(SkillVfxModel.Visual visual:visuals)table(output,visual); });
    }
    private static Frame unframe(byte[] bytes) {
        if(bytes==null||bytes.length>MAX_PACKET) throw bad("packet");
        try(DataInputStream input=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(input.readUnsignedByte()!=VERSION) throw new IOException("version"); int length=input.readUnsignedShort(); byte[] body=input.readNBytes(length); if(body.length!=length) throw new EOFException();
            int count=input.readUnsignedByte(); if(count>2) throw new IOException("tables"); List<Map<String,SkillVfxModel.Appearance>> tables=new ArrayList<>(count);for(int index=0;index<count;index++)tables.add(readTable(input));
            if(input.available()!=0) throw new IOException("trailing"); return new Frame(body,List.copyOf(tables));
        } catch(IOException|RuntimeException exception) { throw bad("malformed",exception); }
    }
    private static void table(DataOutputStream output,SkillVfxModel.Visual visual)throws IOException {
        List<SkillVfxModel.Primitive> primitives=primitives(visual); if(primitives.size()>MAX_PRIMITIVE_TABLE) throw new IOException("table"); output.writeShort(primitives.size());
        for(SkillVfxModel.Primitive primitive:primitives){str(output,primitive.id());output.writeByte(primitive.appearance().kind().ordinal());str(output,primitive.appearance().id());}
    }
    private static Map<String,SkillVfxModel.Appearance> readTable(DataInputStream input)throws IOException {
        int count=input.readUnsignedShort();if(count>MAX_PRIMITIVE_TABLE)throw new IOException("table");Map<String,SkillVfxModel.Appearance> result=new LinkedHashMap<>();
        for(int index=0;index<count;index++){String primitiveId=str(input);AbilityVfx.AppearanceKind kind=enumValue(AbilityVfx.AppearanceKind.values(),input.readUnsignedByte());SkillVfxModel.Appearance appearance=new SkillVfxModel.Appearance(kind,str(input));if(result.put(primitiveId,appearance)!=null)throw new IOException("duplicate primitive");}
        return Map.copyOf(result);
    }
    private static SkillVfxModel.Visual patch(SkillVfxModel.Visual visual,Map<String,SkillVfxModel.Appearance> table) {
        List<SkillVfxModel.Primitive> all=primitives(visual);Set<String> ids=new HashSet<>();for(SkillVfxModel.Primitive primitive:all)ids.add(primitive.id());if(table.size()!=all.size()||!table.keySet().equals(ids))throw bad("primitive ids");
        List<SkillVfxModel.HookBinding> hooks=new ArrayList<>();for(SkillVfxModel.HookBinding hook:visual.hooks()){List<SkillVfxModel.Emission> emissions=new ArrayList<>();for(SkillVfxModel.Emission emission:hook.emissions()){List<SkillVfxModel.Primitive> primitives=new ArrayList<>();for(SkillVfxModel.Primitive primitive:emission.primitives())primitives.add(primitive.withAppearance(table.get(primitive.id())));emissions.add(new SkillVfxModel.Emission(emission.id(),emission.actionIndex(),primitives));}hooks.add(new SkillVfxModel.HookBinding(hook.hook(),emissions));}return new SkillVfxModel.Visual(visual.id(),hooks);
    }
    private static List<SkillVfxModel.Primitive> primitives(SkillVfxModel.Visual visual) { if(visual==null)throw bad("visual");List<SkillVfxModel.Primitive> result=new ArrayList<>();for(SkillVfxModel.HookBinding hook:visual.hooks())for(SkillVfxModel.Emission emission:hook.emissions())result.addAll(emission.primitives());return result; }
    private static Set<String> primitiveIds(SkillVfxModel.Visual visual){Set<String> result=new HashSet<>();for(SkillVfxModel.Primitive primitive:primitives(visual))result.add(primitive.id());return result;}
    private static void str(DataOutputStream output,String value)throws IOException { if(value==null)throw new IOException("string");byte[] raw=value.getBytes(StandardCharsets.UTF_8);if(raw.length>MAX_STRING)throw new IOException("string");output.writeShort(raw.length);output.write(raw); }
    private static String str(DataInputStream input)throws IOException { int length=input.readUnsignedShort();if(length>MAX_STRING)throw new IOException("string");byte[] raw=input.readNBytes(length);if(raw.length!=length)throw new EOFException();try{String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();if(!Arrays.equals(raw,value.getBytes(StandardCharsets.UTF_8)))throw new IOException("utf8");return value;}catch(CharacterCodingException exception){throw new IOException("utf8",exception);} }
    private static <T>T enumValue(T[] values,int ordinal)throws IOException {if(ordinal<0||ordinal>=values.length)throw new IOException("enum");return values[ordinal];}
    private interface Writer { void write(DataOutputStream output)throws IOException; }
    private static byte[] out(Writer writer) {try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream output=new DataOutputStream(bytes)){writer.write(output);}if(bytes.size()>MAX_PACKET)throw bad("packet");return bytes.toByteArray();}catch(IOException exception){throw bad("write",exception);}}
    private static IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }
    private static IllegalArgumentException bad(String message,Exception cause) { return new IllegalArgumentException(message,cause); }
}
