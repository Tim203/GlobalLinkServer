/*
 * Copyright (c) 2021-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */

package org.geysermc.globallinkserver.java;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;

@RequiredArgsConstructor
public class JavaServer implements org.geysermc.globallinkserver.Server {
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private final ServerStatusInfo pong = new ServerStatusInfo(
            new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
            new PlayerInfo(1, 0, new GameProfile[0]),
            Component.text("Global Link Server"),
            null);

    private Server server;

    @Override
    public boolean startServer(Config config) {
        if (server != null) {
            return false;
        }

        server = new Server(config.getBindIp(), config.getJavaPort(), MinecraftProtocol.class,
                new TcpSessionFactory());

        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, true);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY,
                (ServerInfoBuilder) session -> pong);
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY,
                (ServerLoginHandler) session -> {
                    session.send(new ServerJoinGamePacket(
                            0,
                            false,
                            GameMode.SPECTATOR,
                            GameMode.SPECTATOR,
                            1,
                            new String[]{"minecraft:the_end"},
                            TagManager.getDimensionTag(),
                            TagManager.getEndTag(),
                            "minecraft:the_end",
                            100,
                            1,
                            0,
                            false,
                            false,
                            false,
                            false
                    ));

                    // Just send the position without sending chunks
                    // This loads the player into an empty world and stops them from moving
                    session.send(new ServerPlayerPositionRotationPacket(0, 64, 0, 0, 0, 0));

                    // Manually call the connect event
                    session.callEvent(new ConnectedEvent(session));
                });
        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256); // default

        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                super.serverClosed(event);
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new PacketHandler(event.getSession(), linkManager, playerManager));
            }
        });

        server.bind();
        return true;
    }

    @Override
    public void shutdown() {
        server.close();
        server = null;
    }
}
