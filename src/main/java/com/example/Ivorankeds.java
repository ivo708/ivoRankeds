package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRules;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleType;
import com.cobblemon.mod.common.battles.BattleTypes;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

import kotlin.Unit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Ivorankeds implements ModInitializer {
	public static final String MOD_ID = "ivorankeds";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<PokemonBattle> battleList=new ArrayList();
    //public static final long MATCH_CD= 6 * 60 * 60 * 1000;
    public static JsonObject config;
    public static MinecraftServer server;

	@Override
	public void onInitialize() {
		commandRegistration();
		loadConfig();
		CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGH, new BattleVictoryEventHandler());
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);

	    
	}
	public static void battleStartSingles(ServerPlayerEntity player1, ServerPlayerEntity player2, boolean isForced) {
		if(!isInBattlefield(player1,player2)) {
	        String mensaje = "LOS DOS JUGADORES DEBEN ESTAR EN UNA PISTA.";
	        tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
	        tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
	        return;
		}
	    if (!isValid(player1) || !isValid(player2)) {
	        String mensaje = "LOS EQUIPOS NO SON VÁLIDOS, BATALLA CANCELADA.";
	        tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
	        tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
	        return;
	    }
	    if(Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player1)!=null || Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player1)!=null) {
	        String mensaje = "YA HAY UNA BATALLA ACTIVA CON UNO DE LOS PARTICIPANTES.";
	        tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
	        tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
	        return;
	    }
	    
	    
	    if (!isForced) {
	        if (cdCheck(player1, player2)) {
	            return;
	        }
	    }
	    
	    final double pos1X = player1.getX();
	    final double pos1Y = player1.getY();
	    final double pos1Z = player1.getZ();
	    final float yaw1 = player1.getYaw();
	    final float pitch1 = player1.getPitch();
	    
	    final double pos2X = player2.getX();
	    final double pos2Y = player2.getY();
	    final double pos2Z = player2.getZ();
	    final float yaw2 = player2.getYaw();
	    final float pitch2 = player2.getPitch();
	    
	    TeamDisplay.openMenu(player1,player2);
	    TeamDisplay.openMenu(player2,player1);
	    
	    scheduler.schedule(() -> {
		    if(Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player1)!=null || Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player1)!=null) {
		        String mensaje = "YA HAY UNA BATALLA ACTIVA CON UNO DE LOS PARTICIPANTES.";
		        tellraw(player1, player1.getName().getLiteralString(), mensaje, "red");
		        tellraw(player2, player2.getName().getLiteralString(), mensaje, "red");
		        return;
		    }
		    BattleFormat formato= BattleFormat.Companion.getGEN_9_DOUBLES();
		    formato=formato.copy(formato.component1(), formato.component2(), formato.component3(), 9, 50);
	        player1.teleport((ServerWorld) player1.getWorld(), pos1X, pos1Y, pos1Z, yaw1, pitch1);
	        player2.teleport((ServerWorld) player2.getWorld(), pos2X, pos2Y, pos2Z, yaw2, pitch2);
	        UUID starting1=Cobblemon.INSTANCE.getStorage().getParty(player1).toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid();
	        UUID starting2=Cobblemon.INSTANCE.getStorage().getParty(player2).toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid();
	        BattleBuilder.INSTANCE.pvp1v1(
	        		player1,
	        		player2,
	        		starting1,
	        		starting2,	        		
	        		formato,
	        		false,
	        		true	        		
	        		)
	        .ifSuccessful(pokemonBattle -> {
			    LOGGER.info("SUCCESSFULL");
	            battleList.add(pokemonBattle);
	            UUID battleId = pokemonBattle.getBattleId();
	            LOGGER.info("BATALLA INICIADA CON UUID:" + battleId.toString());

	            List<BattleRegistry> data = loadBattleRegistryList();
	            boolean order = player1.getUuid().toString().compareTo(player2.getUuid().toString()) <= 0;
	            data.add(new BattleRegistry(
	                order ? player1.getUuid().toString() : player2.getUuid().toString(),
	                order ? player2.getUuid().toString() : player1.getUuid().toString(),
	                order ? player1.getName().getString() : player2.getName().getString(),
	                order ? player2.getName().getString() : player1.getName().getString(),
	                pokemonBattle.getBattleId().toString(),
	                "NADIE",
	                System.currentTimeMillis()
	            ));
	            saveBattleRegistryList(data);
	            return Unit.INSTANCE;
	        }).ifErrored(pokemonBattle -> {
	        		LOGGER.info("Error");
		            return Unit.INSTANCE;
	        });
	    }, 30, TimeUnit.SECONDS);
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
	
    public static boolean isValid(ServerPlayerEntity player) {
    	PartyStore party=Cobblemon.INSTANCE.getStorage().getParty(player);
    	List<BattlePokemon> team=party.toBattleTeam(true, false, party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid());
    	if(team.size()<2) {
    		String mensaje="DEBES TENER AL MENOS DOS POKÉMON EN EL EQUIPO";
            tellraw(player, player.getName().getLiteralString(), mensaje, "red");
            return false;
    	}
    	if(team.size()>4) {
    		String mensaje="DEBES TENER 4 POKÉMON COMO MÁXIMO EN EL EQUIPO";
            tellraw(player, player.getName().getLiteralString(), mensaje, "red");
            return false;
    	}
    	return true;
    }
    
    public static boolean isInBattlefield(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        try {
            Path CONFIG_FILE_PATH = Paths.get("config", "ivorankeds", "main", "config.json");
            String jsonContent = new String(Files.readAllBytes(CONFIG_FILE_PATH), StandardCharsets.UTF_8);
            JsonObject config = new Gson().fromJson(jsonContent, JsonObject.class);

            JsonArray pistas = config.getAsJsonArray("pistas");
            for (JsonElement pistaElem : pistas) {
                JsonArray pista = pistaElem.getAsJsonArray();
                if (pista.size() < 2) {
                    continue;
                }
                JsonArray posA = pista.get(0).getAsJsonArray();
                JsonArray posB = pista.get(1).getAsJsonArray();

                double xA = posA.get(0).getAsDouble();
                double zA = posA.get(1).getAsDouble();
                double xB = posB.get(0).getAsDouble();
                double zB = posB.get(1).getAsDouble();

                if ((isAtPosition(player1, xA, zA) && isAtPosition(player2, xB, zB)) ||
                    (isAtPosition(player1, xB, zB) && isAtPosition(player2, xA, zA))) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    private static boolean isAtPosition(ServerPlayerEntity player, double targetX, double targetZ) {
        double tolerance = 1.75;
        return Math.abs(player.getX() - targetX) <= tolerance &&
               Math.abs(player.getZ() - targetZ) <= tolerance;
    }
	
    public static BattleActor createBattleActor(ServerPlayerEntity player) {
    	PartyStore party=Cobblemon.INSTANCE.getStorage().getParty(player);
    	List<BattlePokemon> team=party.toBattleTeam(true, false, party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid());
    	if(team.size()>4) {
    		team=team.subList(0, 4);
    	}
    	for(int i=0;i<team.size();i++) {
    		if(team.get(i).getEffectedPokemon().getLevel()>50) {
            	team.get(i).getEffectedPokemon().setLevel(50);
    		}
    		team.get(i).getEffectedPokemon().setOriginalTrainer(player.getUuid());
    	}
        return new PlayerBattleActor(
                player.getUuid(),
                team
        );
    }
    
    public void commandRegistration() {
    	CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
    	    dispatcher.register(
    	        CommandManager.literal("ranked")
    	            .then(Comandos.registerStartBattle())
    	            .then(Comandos.registerForceStartBattle())
    	            .then(Comandos.registerRango())
    	            .then(Comandos.registerReload())
    	            .then(Comandos.registerRequest())
    	            .then(Comandos.registerRequestAccept())
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
             	LOGGER.info("EL REGISTRO DE BATALLAS ES NULL");
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

    public static boolean cdCheck(ServerPlayerEntity player1,ServerPlayerEntity player2) {
    	BattleRegistry batalla=getLastestMatch(player1,player2);
    	if(batalla==null) {
    		return false;
    	}
    	long tiempoActual = System.currentTimeMillis();
            if ((tiempoActual - batalla.matchEpoch) < config.get("matchCD").getAsInt()) {
                long timeRemainingMillis = config.get("matchCD").getAsInt() - (tiempoActual - batalla.matchEpoch);
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
    
    public static long timeSinceLast(ServerPlayerEntity player1, ServerPlayerEntity player2) {
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
        	LOGGER.info("EL REGISTRO DE BATALLAS ES NULL");
        	return config.get("matchCD").getAsInt() * config.get("matchCDfullTimes").getAsInt();
        }
        
        BattleRegistry lastBattle = null;
        BattleRegistry previousBattle = null;
        for (int i = data.size() - 1; i >= 0; i--) {
            BattleRegistry battle = data.get(i);
            if (battle.uuidP1.equals(uuidMenor) && battle.uuidP2.equals(uuidMayor)) {
                if (lastBattle == null) {
                    // La primera coincidencia es la última batalla
                    lastBattle = battle;
                } else {
                    // La siguiente coincidencia es la batalla anterior
                    previousBattle = battle;
                    break;
                }
            }
        }
        if (lastBattle == null || previousBattle == null) {
            return config.get("matchCD").getAsInt() * config.get("matchCDfullTimes").getAsInt();
        }
        return lastBattle.matchEpoch - previousBattle.matchEpoch;
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
    
    public static void tellraw(ServerPlayerEntity jugador,String target,String mensaje, String color) {
    	String cmd = "tellraw "+target+" {\"text\":\"\\n"+mensaje+"\\n\",\"bold\":true,\"italic\":true,\"color\":\""+color+"\"}";
    	jugador.getServer().getCommandManager().executeWithPrefix(jugador.getServer().getCommandSource(),cmd);
    }
    
    public static JsonObject loadConfig() {
        Path configFilePath = Paths.get("config", "ivorankeds", "main", "config.json");

        // Si no existe, se crea la configuración por defecto
        if (!Files.exists(configFilePath)) {
            loadDefaultConfig(configFilePath);
        } else {
            System.out.println("Archivo de configuración encontrado en: " + configFilePath.toAbsolutePath());
        }
        
        try {
            String json = new String(Files.readAllBytes(configFilePath), StandardCharsets.UTF_8);
            Gson gson = new Gson(); // No es necesario prettyPrinting para deserializar
            JsonElement element = gson.fromJson(json, JsonElement.class);
            config=element.getAsJsonObject();
            return element.getAsJsonObject();
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de configuración:");
            e.printStackTrace();
            return new JsonObject();
        }
    }

    private static void loadDefaultConfig(Path configFilePath) {
        try {
            // Crear los directorios necesarios si no existen
            Files.createDirectories(configFilePath.getParent());
            
            // Crear el objeto JSON con las propiedades por defecto
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("matchCD", 30*60*1000);
            defaultConfig.addProperty("matchCDfullTimes", 16);

            defaultConfig.addProperty("textDisplay", true);
            defaultConfig.addProperty("x", 0);
            defaultConfig.addProperty("y", 0);
            defaultConfig.addProperty("z", 0);
            defaultConfig.addProperty("lpIntegration", true);
            JsonArray pistas = new JsonArray();
	
	         JsonArray arr1 = new JsonArray();
	         JsonArray arr11 = new JsonArray();
	         JsonArray arr12 = new JsonArray();
	         arr11.add(30);
	         arr11.add(40);
	         arr12.add(30);
	         arr12.add(40);
	         arr1.add(arr11);
	         arr1.add(arr12);
	         pistas.add(arr1);
	
	         JsonArray arr2 = new JsonArray();
	         JsonArray arr21 = new JsonArray();
	         JsonArray arr22 = new JsonArray();
	         arr21.add(30);
	         arr21.add(40);
	         arr22.add(30);
	         arr22.add(40);
	         arr2.add(arr21);
	         arr2.add(arr22);
	         pistas.add(arr2);
	
	         // Agregamos el array de arrays al objeto JSON
	         defaultConfig.add("pistas", pistas);
            // Convertir el objeto JSON a una cadena con formato
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(defaultConfig);
            config=defaultConfig;
            
            // Escribir el contenido en el archivo
            Files.write(configFilePath, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("Archivo de configuración por defecto creado en: " + configFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error al crear el archivo de configuración por defecto:");
            e.printStackTrace();
        }
    }
    
    private void onServerStarting(MinecraftServer serverNew) {
        server = serverNew;
        System.out.println("Servidor iniciado: " + serverNew);
    }
    
    
    
    
    
    
    
    
    
    
    
}