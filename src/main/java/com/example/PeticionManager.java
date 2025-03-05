package com.example;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.network.ServerPlayerEntity;

public class PeticionManager {
    private static final ConcurrentHashMap<UUID, Peticion> activeRequests = new ConcurrentHashMap<>();

    public static void add(Peticion peticion) {
        activeRequests.put(peticion.getReceiver().getUuid(), peticion);
    }

    public static Peticion get(UUID receiverId) {
        return activeRequests.get(receiverId);
    }

    public static void remove(UUID receiverId) {
        activeRequests.remove(receiverId);
    }
    
    public static boolean existePeticionActiva(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        for (Peticion peticion : activeRequests.values()) {
            if (peticion.getSender().equals(sender) &&
                peticion.getReceiver().equals(receiver) &&
                peticion.isActiva()) {
                return true;
            }
        }
        return false;
    }
}
