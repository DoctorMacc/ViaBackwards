package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.ViaVersion.api.type.Type;

import java.util.function.Function;

public class SoundRewriter extends us.myles.ViaVersion.api.rewriters.SoundRewriter {

    private final Function<String, String> stringIdRewriter;

    public SoundRewriter(BackwardsProtocol protocol, IdRewriteFunction idRewriter, Function<String, String> stringIdRewriter) {
        super(protocol, idRewriter);
        this.stringIdRewriter = stringIdRewriter;
    }

    public SoundRewriter(BackwardsProtocol protocol, IdRewriteFunction idRewriter) {
        this(protocol, idRewriter, null);
    }

    public void registerNamedSound(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound identifier
                handler(wrapper -> {
                    String soundId = wrapper.get(Type.STRING, 0);
                    String mappedId = stringIdRewriter.apply(soundId);
                    if (mappedId == null) return;
                    if (!mappedId.isEmpty()) {
                        wrapper.set(Type.STRING, 0, mappedId);
                    } else {
                        wrapper.cancel();
                    }
                });
            }
        });
    }

    public void registerStopSound(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    byte flags = wrapper.passthrough(Type.BYTE);
                    if ((flags & 0x02) == 0) return; // No sound specified

                    if ((flags & 0x01) != 0) {
                        wrapper.passthrough(Type.VAR_INT); // Source
                    }

                    String soundId = wrapper.read(Type.STRING);
                    String mappedId = stringIdRewriter.apply(soundId);
                    if (mappedId == null) {
                        // No mapping found
                        wrapper.write(Type.STRING, soundId);
                        return;
                    }

                    if (!mappedId.isEmpty()) {
                        wrapper.write(Type.STRING, mappedId);
                    } else {
                        // Cancel if set to empty
                        wrapper.cancel();
                    }
                });
            }
        });
    }
}
