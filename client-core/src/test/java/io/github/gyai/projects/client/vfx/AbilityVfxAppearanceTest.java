package io.github.gyai.projects.client.vfx;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Appearance regression vectors stay Minecraft-free; direct spawning is isolated at the client boundary. */
public final class AbilityVfxAppearanceTest {
    public static void main(String[] args) throws Exception {
        String catalog=Files.readString(Path.of("client-core/src/test/resources/protocol/ability-vfx-appearance-v01-catalog.txt"));
        assert sha(catalog.getBytes(StandardCharsets.UTF_8)).equals("E8F6A9083CC1D146070B9A74CE0A91545473647A575F257F46C3DA89F3642297");
        assert SupportedAppearanceCatalog.particleIds().equals(Set.of("minecraft:ash","minecraft:cloud","minecraft:crit","minecraft:enchanted_hit","minecraft:end_rod","minecraft:firework","minecraft:flame","minecraft:soul","minecraft:soul_fire_flame"));
        assert !SupportedAppearanceCatalog.isParticle("minecraft:dust");
        try { AbilityVfx.Appearance.particle("minecraft:dust"); throw new AssertionError("dust accepted"); } catch (IllegalArgumentException expected) { }
        byte[] v1=hex(Files.readString(Path.of("client-core/src/test/resources/protocol/ability-vfx-v1-golden.hex")).trim());
        assert AbilityVfx.decode(v1).cue().primitives().getFirst().appearance().equals(AbilityVfx.Appearance.DEBUG_QUAD);
        String text=Files.readString(Path.of("client-core/src/test/resources/protocol/ability-vfx-v2-particle-golden.hex"));
        assert sha(text.getBytes(StandardCharsets.UTF_8)).equals("767E2A7E26960548CB123070B26E3DEBD766595CAB92EC3F1BEAA1A4598CCABB");
        byte[] particle=hex(text.trim()); assert sha(particle).equals("CA56D6D65FBD469E48178B072FF93267EC597CC6272A9C153353C6F795E4E306");
        AbilityVfx.Decoded decoded=AbilityVfx.decode(particle);assert decoded.valid()&&decoded.cue().primitives().getFirst().appearance().equals(AbilityVfx.Appearance.particle("minecraft:flame"));
        int header=primitiveHeader(particle);int length=((particle[header+2]&255)<<8)|(particle[header+3]&255);byte[] mixed=Arrays.copyOf(particle,particle.length+129);mixed[header-1]=2;int at=particle.length;mixed[at]=(byte)AbilityVfx.Type.CIRCLE.ordinal();mixed[at+1]=1;mixed[at+2]=0;mixed[at+3]=125;System.arraycopy(particle,header+4,mixed,at+4,125);decoded=AbilityVfx.decode(mixed);assert length==143&&decoded.valid()&&decoded.cue().primitives().size()==2&&decoded.cue().primitives().get(1).appearance().equals(AbilityVfx.Appearance.DEBUG_QUAD);
        byte[] unknown=particle.clone();replace(unknown,"minecraft:flame","minecraft:xxxxx");decoded=AbilityVfx.decode(unknown);assert decoded.valid()&&decoded.cue().primitives().isEmpty();
        byte[] invalidRadius=unknown.clone();java.nio.ByteBuffer.wrap(invalidRadius).order(java.nio.ByteOrder.BIG_ENDIAN).putDouble(header+4+66,0);assert !AbilityVfx.decode(invalidRadius).valid();byte[] invalidSlot=unknown.clone();java.nio.ByteBuffer.wrap(invalidSlot).order(java.nio.ByteOrder.BIG_ENDIAN).putDouble(header+4+74,1);assert !AbilityVfx.decode(invalidSlot).valid();byte[] trailing=Arrays.copyOf(unknown,unknown.length+1);trailing[header+2]=0;trailing[header+3]=(byte)144;trailing[trailing.length-1]=7;assert !AbilityVfx.decode(trailing).valid();
        AbilityVfxStore store=new AbilityVfxStore();AbilityVfx.Cue noOp=decoded.cue();for(long sequence=1;sequence<=80;sequence++)assert store.receive(new AbilityVfx.Decoded(true,copy(noOp,sequence,UUID.randomUUID(),noOp.castId(),noOp.hook(),List.of())),noOp.worldId().toString(),noOp.dimension(),0);assert store.size()==0;
        AbilityVfx.Cue supported=copy(AbilityVfx.decode(particle).cue(),81,UUID.randomUUID(),noOp.castId(),AbilityVfx.Hook.TELEGRAPH,AbilityVfx.decode(particle).cue().primitives());assert store.receive(new AbilityVfx.Decoded(true,supported),supported.worldId().toString(),supported.dimension(),0)&&store.size()==1;assert !store.receive(new AbilityVfx.Decoded(true,supported),supported.worldId().toString(),supported.dimension(),0);
        AbilityVfx.Cue cancel=copy(noOp,82,UUID.randomUUID(),supported.castId(),AbilityVfx.Hook.CANCEL,List.of());assert store.receive(new AbilityVfx.Decoded(true,cancel),cancel.worldId().toString(),cancel.dimension(),0)&&store.size()==0;assert !store.receive(new AbilityVfx.Decoded(true,cancel),cancel.worldId().toString(),cancel.dimension(),0);
        byte[] malformed=particle.clone();malformed[malformed.length-18]=0;assert !AbilityVfx.decode(malformed).valid();
        assert AbilityVfxParticlePolicy.density(256,AbilityVfx.Quality.HIGH,0)==64&&AbilityVfxParticlePolicy.density(64,AbilityVfx.Quality.LOW,2)==6;
        assert AbilityVfxParticlePolicy.allow(64,240,500)==12&&AbilityVfxParticlePolicy.allow(64,0,512)==0;
        AbilityVfxParticleTickGate gate=new AbilityVfxParticleTickGate();assert gate.first(4)&&!gate.first(4)&&gate.first(5);gate.reset();assert gate.first(4);
        AbilityVfx.Primitive dense=new AbilityVfx.Primitive(AbilityVfx.Type.CIRCLE,0,20,new AbilityVfx.Color(255,255,255,255),.1,256,1,new AbilityVfx.Vec(0,0,0),0,0,3,0,0,0,0,0,0,0,List.of(),AbilityVfx.Appearance.particle("minecraft:flame"));
        var cue=new AbilityVfx.Cue(UUID.randomUUID(),1,UUID.randomUUID(),UUID.randomUUID(),"projects:vfx/particle",AbilityVfx.Hook.TELEGRAPH,0,0,UUID.randomUUID(),"minecraft:overworld",new AbilityVfx.Frame(new AbilityVfx.Vec(0,0,0),new AbilityVfx.Vec(0,0,1),new AbilityVfx.Vec(0,1,0)),0,0,20,Collections.nCopies(16,dense));
        var remote=AbilityVfxParticlePlanner.plan(cue,p->true,p->1,AbilityVfx.Quality.HIGH,0,0);var local=AbilityVfxParticlePlanner.plan(cue,p->true,p->1,AbilityVfx.Quality.HIGH,0,0);assert remote.spawns().equals(local.spawns())&&remote.spawns().size()==256&&remote.cueUsed()==256;
        assert AbilityVfxParticlePlanner.plan(cue,p->true,p->1,AbilityVfx.Quality.HIGH,0,300).spawns().size()==212;
        AbilityVfxParticlePlanner.Spawn first=new AbilityVfxParticlePlanner.Spawn(AbilityVfx.Appearance.particle("minecraft:flame"),new AbilityVfx.Vec(1,0,0)),second=new AbilityVfxParticlePlanner.Spawn(AbilityVfx.Appearance.particle("minecraft:soul"),new AbilityVfx.Vec(2,0,0)),localSpawn=new AbilityVfxParticlePlanner.Spawn(AbilityVfx.Appearance.particle("minecraft:ash"),new AbilityVfx.Vec(3,0,0));List<String> delivered=new ArrayList<>();int reserved=AbilityVfxParticleDispatchRunner.run(List.of(used->{throw new IllegalStateException("broken cue");},used->new AbilityVfxParticlePlanner.Plan(List.of(first,second),2),used->new AbilityVfxParticlePlanner.Plan(List.of(localSpawn),1)),spawn->{if(spawn==first)throw new IllegalStateException("broken spawn");delivered.add(spawn.appearance().id());},0);assert reserved==3&&delivered.equals(List.of("minecraft:soul","minecraft:ash"));int capped=AbilityVfxParticleDispatchRunner.run(List.of(used->new AbilityVfxParticlePlanner.Plan(Collections.nCopies(400,first),400),used->new AbilityVfxParticlePlanner.Plan(Collections.nCopies(400,second),400)),spawn->{},0);assert capped==512;
    }
    private static void replace(byte[] raw,String from,String to){byte[] a=from.getBytes(StandardCharsets.UTF_8),b=to.getBytes(StandardCharsets.UTF_8);assert a.length==b.length;outer:for(int i=0;i<=raw.length-a.length;i++){for(int j=0;j<a.length;j++)if(raw[i+j]!=a[j])continue outer;System.arraycopy(b,0,raw,i,b.length);return;}throw new AssertionError("text missing");}
    private static int primitiveHeader(byte[] raw){int at=1+16+8+16+16;int visual=((raw[at]&255)<<8)|(raw[at+1]&255);at+=2+visual+1+4+4+16;int dimension=((raw[at]&255)<<8)|(raw[at+1]&255);return at+2+dimension+9*8+8+8+4+1;}
    private static AbilityVfx.Cue copy(AbilityVfx.Cue cue,long sequence,UUID id,UUID cast,AbilityVfx.Hook hook,List<AbilityVfx.Primitive> primitives){return new AbilityVfx.Cue(cue.session(),sequence,id,cast,cue.visualId(),hook,cue.actionIndex(),cue.emissionIndex(),cue.worldId(),cue.dimension(),cue.frame(),cue.serverTick(),cue.startTick(),cue.duration(),primitives);}
    private static byte[] hex(String value){byte[] out=new byte[value.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return out;}
    private static String sha(byte[] bytes)throws Exception{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).toUpperCase(Locale.ROOT);}
}
