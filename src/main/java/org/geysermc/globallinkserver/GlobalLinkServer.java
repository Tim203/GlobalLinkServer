/*
 * Copyright (c) 2021-2024 GeyserMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.geysermc.globallinkserver.bedrock.BedrockServer;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.config.ConfigReader;

public class GlobalLinkServer {
    private static final Path collectedSkinsPath = Paths.get("./collected_skins.json");
    private static JsonArray collectedSkins = new JsonArray();
    public static final Logger LOGGER = Logger.getGlobal();

    public static void main(String... args) throws InterruptedException, IOException {
        // Make logging more simple, adopted from https://stackoverflow.com/a/5937929
        System.setProperty(
                "java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %5$s%6$s%n");
        Config config = ConfigReader.readConfig();

        if (Files.exists(collectedSkinsPath)) {
            String s = new String(Files.readAllBytes(collectedSkinsPath), StandardCharsets.UTF_8);
            collectedSkins = new Gson().fromJson(s, JsonArray.class);
        }

        new BedrockServer(null, null).startServer(config);

        LOGGER.info("Started Global Linking Server");

        // we have to keep the program alive
        Thread.sleep(Long.MAX_VALUE);
    }

    public static void addCollectedSkin(String xuid, String username, long timestamp, String data) {
        JsonObject object = new JsonObject();
        object.addProperty("xuid", xuid);
        object.addProperty("username", username);
        object.addProperty("timestamp", timestamp);
        object.addProperty("data", data);
        collectedSkins.add(object);

        try {
            Files.write(collectedSkinsPath, new Gson().toJson(collectedSkins).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to write to file!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
