package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleTypes;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import kotlin.Unit;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Ivorankeds implements ModInitializer {
	public static final String MOD_ID = "ivorankeds";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static List<PokemonBattle> battleList=new ArrayList();
    public static final long MATCH_CD= 24 * 60 * 60 * 1000;

	@Override
	public void onInitialize() {
		commandRegistration();
		CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGH, new BattleVictoryEventHandler());

	    
	}
	public void battleStartSingles(ServerPlayerEntity player1,ServerPlayerEntity player2,boolean isForced) {
		if(!isValid(player1) || !isValid(player2)) {
    		String mensaje="LOS EQUIPOS NO SON VÁLIDOS, BATALLA CANCELADA.";
            tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
            tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
            return;
		}
		
		if(!isForced) {
			if(cdCheck(player1,player2)) {
				return;
			}
		}
		Cobblemon.INSTANCE.getBattleRegistry().startBattle(BattleFormat.Companion.getGEN_9_DOUBLES(),
				 new BattleSide(createBattleActor(player1)),
				 new BattleSide(createBattleActor(player2)),
				false
		).ifSuccessful(pokemonBattle -> {
			battleList.add(pokemonBattle);
            UUID battleId = pokemonBattle.getBattleId();
            LOGGER.info("BATALLA INICIADA CON UUID:"+battleId.toString());
            
            List<BattleRegistry> data = loadBattleRegistryList();
            data.add(new BattleRegistry(player1.getUuid().toString(),
						            		player2.getUuid().toString(),
						            		player1.getName().getLiteralString(),
						            		player2.getName().getLiteralString(),
						            		pokemonBattle.getBattleId().toString(),
						            		"NADIE",
						            		System.currentTimeMillis())
            			);
            saveBattleRegistryList(data);
            return Unit.INSTANCE;
        });
		
	}
	/*public void battleStartDoubles(ServerPlayerEntity player1,ServerPlayerEntity player2,ServerPlayerEntity player3,ServerPlayerEntity player4) {		
		Cobblemon.INSTANCE.getBattleRegistry().startBattle(BattleFormat.Companion.getGEN_9_MULTI(),
				 new BattleSide(createBattleActor(player1),createBattleActor(player2)),
				 new BattleSide(createBattleActor(player3),createBattleActor(player4)),
				false
		).ifSuccessful(pokemonBattle -> {
			battleList.add(pokemonBattle);
            UUID battleId = pokemonBattle.getBattleId();
            LOGGER.info("BATALLA INICIADA CON UUID:"+battleId.toString());
            PlayerBattleActor actor = (PlayerBattleActor) pokemonBattle.getActor(player1);
            SoundEvent battleTheme = CobblemonSounds.PVN_BATTLE;
            actor.setBattleTheme(battleTheme);
            return Unit.INSTANCE;
        });
	}*/
	
    public boolean isValid(ServerPlayerEntity player) {
    	PartyStore party=Cobblemon.INSTANCE.getStorage().getParty(player);
    	List<BattlePokemon> team=party.toBattleTeam(false, false, party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid());
    	if(team.size()<2) {
    		String mensaje="DEBES TENER AL MENOS DOS POKÉMON EN EL EQUIPO";
            tellraw(player, player.getName().getLiteralString(), mensaje, "red");
            return false;
    	}
    	return true;
    }
	
    public BattleActor createBattleActor(ServerPlayerEntity player) {
    	PartyStore party=Cobblemon.INSTANCE.getStorage().getParty(player);
    	List<BattlePokemon> team=party.toBattleTeam(false, false, party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid());
    	if(team.size()>4) {
    		team=team.subList(0, 4);
    	}
        return new PlayerBattleActor(
                player.getUuid(),
                team
        );
    }
    
    public void commandRegistration() {
    	CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
    	    dispatcher.register(
    	        CommandManager.literal("ivoRankeds")
    	            // Subcomando "startbattle"
    	            .then(CommandManager.literal("startbattle")
    	                    .then(CommandManager.argument("player", EntityArgumentType.player())
    	                        .then(CommandManager.argument("opponent", EntityArgumentType.player())
    	                            .executes(context -> {
    	                                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
    	                                ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "opponent");
    	                                
    	                                // Llama a tu método para iniciar la batalla en singles
    	                                battleStartSingles(player1, player2,false);
    	                                
    	                                return 1;
    	                            })
    	                        )
    	                    )
    	            )
    	            .then(CommandManager.literal("forcestartbattle")
    	                    .then(CommandManager.argument("player", EntityArgumentType.player())
    	                        .then(CommandManager.argument("opponent", EntityArgumentType.player())
    	                            .executes(context -> {
    	                                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
    	                                ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "opponent");
    	                                
    	                                // Llama a tu método para iniciar la batalla en singles
    	                                battleStartSingles(player1, player2,true);
    	                                
    	                                return 1;
    	                            })
    	                        )
    	                    )
    	            )
    	            .then(CommandManager.literal("rango")
    	                    .executes(context -> {
    	                        ServerPlayerEntity jugador = context.getSource().getPlayer();    	                        
    	                        Rango.tellRango(jugador);    	                        
    	                        return 1;
    	                    })
    	            )
    	    );
    	});
    }
    public static void borrarBatalla(PokemonBattle batalla) {
        for(int i=0;i<battleList.size();i++) {
        	if(battleList.get(i).getBattleId()==batalla.getBattleId()) {
        		battleList.remove(i);
        	}
        }
    }
    
    public static BattleRegistry getLastestMatch(ServerPlayerEntity player1,ServerPlayerEntity player2) {
    	 String uuidMenor;
         String uuidMayor;
         if (player1.getUuid().toString().compareTo(player2.getUuid().toString()) <= 0) {
             uuidMenor = player1.getUuid().toString();
             uuidMayor = player2.getUuid().toString();
         } else {
             uuidMenor = player2.getUuid().toString();
             uuidMayor = player1.getUuid().toString();
         }
             List<BattleRegistry> data = loadBattleRegistryList();
             if (data == null) {
                 return null;
             }
             for (int i = data.size() - 1; i >= 0; i--) {
                 BattleRegistry batalla = data.get(i);
                 if (batalla.uuidP1.equals(uuidMenor) && batalla.uuidP2.equals(uuidMayor)) {
                	  return batalla;
 	            }
             }
         return null;
    }

    public boolean cdCheck(ServerPlayerEntity player1,ServerPlayerEntity player2) {
    	BattleRegistry batalla=getLastestMatch(player1,player2);
    	if(batalla==null) {
    		return false;
    	}
    	long tiempoActual = System.currentTimeMillis();
            if ((tiempoActual - batalla.matchEpoch) < MATCH_CD) {
                long timeRemainingMillis = MATCH_CD - (tiempoActual - batalla.matchEpoch);
                // Convertir a horas, minutos y segundos
                long hours = timeRemainingMillis / (1000 * 60 * 60);
                long minutes = (timeRemainingMillis / (1000 * 60)) % 60;
                long seconds = (timeRemainingMillis / 1000) % 60;
                
                // Formatear el mensaje con el tiempo restante
                String timeRemaining = hours + "h " + minutes + "m " + seconds + "s";
                String mensaje = "No puedes luchar con este jugador hasta dentro de: " + timeRemaining;
                
                // Enviar el tellraw al jugador (por ejemplo, al mismo jugador que ejecuta el comando)
                tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
                tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
                return true;
            } else {
                return false;
            }
        }
    
    public static void saveBattleRegistryList(List<BattleRegistry> matchHistory) {
        Path directory = Paths.get("config/ivorankeds/history");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.err.println("Error al crear el directorio: " + e.getMessage());
                return;
            }
        }
        Path filePath = directory.resolve("matchHistory.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(matchHistory);

        try {
            Files.write(filePath, json.getBytes());
            System.out.println("Lista guardada correctamente en: " + filePath.toString());
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo: " + e.getMessage());
        }
    }
    
    public static List<BattleRegistry> loadBattleRegistryList() {
        Path directory = Paths.get("config/ivorankeds/history");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.err.println("Error al crear el directorio: " + e.getMessage());
                return new ArrayList<>();
            }
        }
        Path filePath = directory.resolve("matchHistory.json");
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                Files.write(filePath, "[]".getBytes(StandardCharsets.UTF_8));
                System.out.println("Archivo matchHistory.json creado con contenido inicial.");
            } catch (IOException e) {
                System.err.println("Error al crear el archivo: " + e.getMessage());
                return new ArrayList<>();
            }
            return new ArrayList<>();
        }

        try {
            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<BattleRegistry>>() {}.getType();
            List<BattleRegistry> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void tellraw(ServerPlayerEntity jugador,String target,String mensaje, String color) {
    	String cmd = "tellraw "+target+" {\"text\":\"\\n"+mensaje+"\\n\",\"bold\":true,\"italic\":true,\"color\":\""+color+"\"}";
    	jugador.getServer().getCommandManager().executeWithPrefix(jugador.getServer().getCommandSource(),cmd);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}