package com.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.minecraft.server.network.ServerPlayerEntity;

public class Peticion {
    private final ServerPlayerEntity sender;
    private final ServerPlayerEntity receiver;
    private boolean activa;
    private ScheduledFuture<?> expirationTask;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public Peticion(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.activa = true;
    }

    public ServerPlayerEntity getSender() {
        return sender;
    }

    public boolean isActiva() {
		return activa;
	}

	public void setActiva(boolean activa) {
		this.activa = activa;
	}

	public ServerPlayerEntity getReceiver() {
        return receiver;
    }

	public void Enviar() {
	    if (PeticionManager.existePeticionActiva(sender, receiver)) {
	        Ivorankeds.tellraw(sender, sender.getName().getString(), 
	            "Ya tienes una petición activa hacia " + receiver.getName().getString(), "red");
	        return;
	    }

	    Ivorankeds.tellraw(sender, sender.getName().getString(), 
	        "Has enviado una petición a " + receiver.getName().getString(), "green");
	    Ivorankeds.tellraw(receiver, receiver.getName().getString(), 
	        sender.getName().getString() + " te ha enviado una petición. Escribe /ranked accept " 
	        + sender.getName().getString() + " para aceptarla.", "yellow");

	    PeticionManager.add(this);

	    expirationTask = scheduler.schedule(() -> {
	        if (activa) {
	            activa = false;
	            
	            sender.getServer().execute(() -> {
	                Ivorankeds.tellraw(sender, sender.getName().getString(), 
	                    "Tu petición a " + receiver.getName().getString() + " ha expirado.", "red");
	                Ivorankeds.tellraw(receiver, receiver.getName().getString(), 
	                    "La petición de " + sender.getName().getString() + " ha expirado.", "red");
	                PeticionManager.remove(receiver.getUuid());
	            });
	        }
	    }, 30, TimeUnit.SECONDS);
	}


    public void Aceptar() {
        if (activa) {
            activa = false;
            if (expirationTask != null && !expirationTask.isDone()) {
                expirationTask.cancel(false);
            }
            Ivorankeds.tellraw(sender, sender.getName().getString(), receiver.getName().getString() + " ha aceptado tu petición.", "blue");
            Ivorankeds.tellraw(receiver, receiver.getName().getString(), "Has aceptado la petición de " + sender.getName().getString(), "blue");
        } else {
            Ivorankeds.tellraw(receiver, receiver.getName().getString(), "La petición ya ha expirado o fue aceptada previamente.", "red");
        }
    }
}

