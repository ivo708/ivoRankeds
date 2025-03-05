package com.example;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class Comandos {
	


	static LiteralArgumentBuilder<ServerCommandSource> registerStartBattle() {
	    return CommandManager.literal("startbattle")
	            .then(CommandManager.argument("player", EntityArgumentType.player())
	                .then(CommandManager.argument("opponent", EntityArgumentType.player())
	                    .executes(context -> {
	                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
	                        ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "opponent");
	                        Ivorankeds.battleStartSingles(player1, player2, false);
	                        return 1;
	                    })
	                )
	            );
	}

	static LiteralArgumentBuilder<ServerCommandSource> registerForceStartBattle() {
	    return CommandManager.literal("forcestartbattle")
	            .then(CommandManager.argument("player", EntityArgumentType.player())
	                .then(CommandManager.argument("opponent", EntityArgumentType.player())
	                    .executes(context -> {
	                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
	                        ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "opponent");
	                        Ivorankeds.battleStartSingles(player1, player2, true);
	                        return 1;
	                    })
	                )
	            );
	}

	static LiteralArgumentBuilder<ServerCommandSource> registerRango() {
	    return CommandManager.literal("rango")
	            .executes(context -> {
	                ServerPlayerEntity jugador = context.getSource().getPlayer();
	                Rango.tellRango(jugador);
	                return 1;
	            });
	}

	static LiteralArgumentBuilder<ServerCommandSource> registerReload() {
	    return CommandManager.literal("reload")
	            .executes(context -> {
	            	Ivorankeds.loadConfig();
	                return 1;
	            });
	}

	
	static LiteralArgumentBuilder<ServerCommandSource> registerRequest() {
	    return CommandManager.literal("request")
	            .then(CommandManager.argument("player", EntityArgumentType.player())
	                    .executes(context -> {
	                        ServerPlayerEntity receiver = EntityArgumentType.getPlayer(context, "player");
	                        ServerPlayerEntity sender = context.getSource().getPlayer();
	                        Peticion peticion = new Peticion(sender, receiver);
	                        peticion.Enviar();
	                        return 1;
	                    })
	            );
	}
	
	static LiteralArgumentBuilder<ServerCommandSource> registerRequestAccept() {
	    return CommandManager.literal("accept")
	            .then(CommandManager.argument("sender", EntityArgumentType.player())
	                    .executes(context -> {
	                        ServerPlayerEntity receiver = context.getSource().getPlayer();
	                        ServerPlayerEntity sender = EntityArgumentType.getPlayer(context, "sender");
	                        Peticion peticion = PeticionManager.get(receiver.getUuid());
	                        if (peticion == null || !peticion.getSender().equals(sender)) {
	                            Ivorankeds.tellraw(receiver, receiver.getName().getString(), "No tienes ninguna petición de " + sender.getName().getString(), "red");
	                            return 0;
	                        } else {
	                            peticion.Aceptar();
	                            PeticionManager.remove(receiver.getUuid());
	                            Ivorankeds.battleStartSingles(sender, receiver, false);
	                            return 1;
	                        }
	                    })
	            );
	}
	
	
	
	
	
	
	
	
	
	
}
