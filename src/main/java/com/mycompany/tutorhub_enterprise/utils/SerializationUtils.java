package com.mycompany.tutorhub_enterprise.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.Packet;
import java.nio.charset.StandardCharsets;

public class SerializationUtils {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Packet.class, new PacketDeserializer())
            .create();

    public static byte[] serialize(Object obj) throws Exception {
        String json = gson.toJson(obj);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    public static Object deserialize(byte[] bytes) throws Exception {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(json, Packet.class);
    }
}
