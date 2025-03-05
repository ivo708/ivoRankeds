package com.example;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.Reader;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;

import com.google.gson.Gson;


import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class BattleVictoryEventHandler implements Function1<BattleVictoryEvent, Unit> {
    @Override
    public Unit invoke(BattleVictoryEvent event) {
    	Ivorankeds.LOGGER.info("--BATTLE ENDED--");
        if (!isRankedBattle(event)) {
            return Unit.INSTANCE;
        }
    	Ivorankeds.LOGGER.info("--BATTLE ENDED--");
        PlayerBattleActor winnerActor=(PlayerBattleActor) event.getWinners().get(0);
        ServerPlayerEntity winner=winnerActor.getEntity();
        
        PlayerBattleActor loserActor=(PlayerBattleActor) event.getLosers().get(0);
        ServerPlayerEntity loser=loserActor.getEntity();
        

        onPlayerVictory(winner,loser);
        onPlayerDefeat(loser,winner);
      
        Ivorankeds.borrarBatalla(event.getBattle());
        
        if(Ivorankeds.config.get("textDisplay").getAsBoolean()) {
        	updateTextDisplay(winner);
        }
        
        return Unit.INSTANCE;
    }

    private boolean isRankedBattle(BattleVictoryEvent event) {
        try {
            boolean isRanked=false;
            for(int i=0;i<Ivorankeds.battleList.size();i++) {
            	Ivorankeds.LOGGER.info("COMPARANDO ["+Ivorankeds.battleList.get(i).getBattleId().toString()+"] CON ["+event.getBattle().getBattleId().toString()+"]");
            	if(Ivorankeds.battleList.get(i).getBattleId()==event.getBattle().getBattleId()) {
            		isRanked=true;
            	}
            }
            return isRanked;

        } catch (NullPointerException e) {
            return false;
        }
    }
    
    
    private void onPlayerVictory(ServerPlayerEntity jugador,ServerPlayerEntity oponente) {
    	updateBattleWinner(jugador,oponente);
    	Rango rango=Rango.cargar(jugador);
    	Rango rangoOponente=Rango.cargar(oponente);
    	int puntos=rango.getPuntos();
    	int rangoGanador= (rango.getRango().ordinal()*3)+ (4-rango.getSubRango());
    	int rangoPerdedor= (rangoOponente.getRango().ordinal()*3)+ (4-rangoOponente.getSubRango());
    	
    	int ganancia=30+(2*(rangoPerdedor-rangoGanador));
    	long tsl=Ivorankeds.timeSinceLast(jugador,oponente);
    	Ivorankeds.LOGGER.info("TSL: "+tsl);
    	int matchCD = Ivorankeds.config.get("matchCD").getAsInt();
    	int matchCDfullTimes = Ivorankeds.config.get("matchCDfullTimes").getAsInt();
    	if(tsl<matchCD*matchCDfullTimes) {
    		double intervalsPassed = (((double) tsl) / matchCD)+1;
        	Ivorankeds.LOGGER.info("INTERVALS PASSED: "+intervalsPassed);
    		double multiplier = (double) intervalsPassed / matchCDfullTimes;
        	Ivorankeds.LOGGER.info("MULTIPLIER: "+multiplier);
    		ganancia=(int) (ganancia*multiplier);
    	}
    	tellraw(jugador,jugador.getName().getLiteralString(),"¡HAS GANADO "+ganancia+" PUNTOS!","green");
    	int subRangoPre=rango.getSubRango();
    	puntos= puntos+ganancia;
    	rango.setPuntos(puntos);
    	if(subRangoPre!=rango.getSubRango()) {
        	tellraw(jugador,jugador.getName().getLiteralString(),"¡HAS ASCENDIDO A "+rango.getRango()+" "+rango.getSubRango()+"!","dark_green");
    	}
    	rango.guardar();    	
    }

    private void onPlayerDefeat(ServerPlayerEntity jugador,ServerPlayerEntity oponente) {
    	Rango rango=Rango.cargar(jugador);
    	Rango rangoOponente=Rango.cargar(oponente);
    	int puntos=rango.getPuntos();
    	int rangoGanador= (rango.getRango().ordinal()*3)+ (4-rango.getSubRango());
    	int rangoPerdedor= (rangoOponente.getRango().ordinal()*3)+ (4-rangoOponente.getSubRango());

    	int perdida=25-(2*(rangoPerdedor-rangoGanador));
    	long tsl=Ivorankeds.timeSinceLast(jugador,oponente);
    	int matchCD = Ivorankeds.config.get("matchCD").getAsInt();
    	int matchCDfullTimes = Ivorankeds.config.get("matchCDfullTimes").getAsInt();
    	if(tsl<matchCD*matchCDfullTimes) {
    		double intervalsPassed = (((double) tsl) / matchCD)+1;
    		double multiplier = (double) intervalsPassed / matchCDfullTimes;
    		perdida=(int) (perdida*multiplier);
    	}
    	tellraw(jugador,jugador.getName().getLiteralString(),"HAS PERDIDO "+perdida+" PUNTOS","red");
    	int subRangoPre=rango.getSubRango();
    	puntos= puntos-perdida;
    	rango.setPuntos(puntos);
    	if(subRangoPre!=rango.getSubRango()) {
        	tellraw(jugador,jugador.getName().getLiteralString(),"HAS DESCENDIDO A "+rango.getRango()+" "+rango.getSubRango(),"dark_red");
    	}
    	rango.guardar();  
    }
    
    public void tellraw(ServerPlayerEntity jugador,String target,String mensaje, String color) {
    	String cmd = "tellraw "+target+" {\"text\":\"\\n"+mensaje+"\\n\",\"bold\":true,\"italic\":true,\"color\":\""+color+"\"}";
    	jugador.getServer().getCommandManager().executeWithPrefix(jugador.getServer().getCommandSource(),cmd);
    }
    
    public static void updateBattleWinner(ServerPlayerEntity winner, ServerPlayerEntity loser) {
        List<BattleRegistry> battles = Ivorankeds.loadBattleRegistryList();
        if (battles.isEmpty() || battles==null) {
            return;
        }
        BattleRegistry lastBattle= Ivorankeds.getLastestMatch(winner, loser);
        if (lastBattle==null) {
            return;
        }
        for (int i = 0; i < battles.size(); i++) {
            BattleRegistry batalla = battles.get(i);
            if (batalla.uuidBattle.equals(lastBattle.uuidBattle)) {
            	batalla.winner = winner.getName().getLiteralString();
                battles.set(i, batalla);
                break;
            }
        }
        Ivorankeds.saveBattleRegistryList(battles);
    }
    
    
    public static void updateTextDisplay(ServerPlayerEntity jugador) {
    	int x = Ivorankeds.config.get("x").getAsInt();
    	int y = Ivorankeds.config.get("y").getAsInt();
    	int z = Ivorankeds.config.get("z").getAsInt();
    	
        BlockPos pos = new BlockPos(x, y, z);

        // Definir un radio de búsqueda (por ejemplo, 1 bloque) y crear un "Box" de búsqueda
        double radio = 2.0;
        Box areaBusqueda = new Box(pos).expand(radio);
        List<TextDisplayEntity> entidades = jugador.getServer().getOverworld().getEntitiesByClass(TextDisplayEntity.class, areaBusqueda, entity -> true);
        if (!entidades.isEmpty()) {
            // Se obtiene la primera entidad encontrada y se actualiza su texto
            TextDisplayEntity textDisplay = (TextDisplayEntity) entidades.get(0);
            textDisplay.setText(Text.of(getRanking()));
        }
        return;
    }
    
    
    public static String getRanking() {
    	List<Rango> rangoList = new ArrayList<>();
        Gson gson = new Gson();
        Path folder = Paths.get("config/ivorankeds");

        // Iterar sobre cada archivo de la carpeta
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        Rango rango = gson.fromJson(reader, Rango.class);
                        rangoList.add(rango);
                    } catch (Exception e) {
                        System.err.println("Error leyendo archivo " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error accediendo a la carpeta: " + e.getMessage());
        }
        List<Rango> top10 = rangoList.stream()
        	    .sorted((r1, r2) -> {
        	        int score1 = (r1.getRango().ordinal() * 3) + (4 - r1.getSubRango());
        	        int score2 = (r2.getRango().ordinal() * 3) + (4 - r2.getSubRango());
        	        int cmp = Integer.compare(score2, score1);
        	        if(cmp == 0) {
        	            // En caso de empate, comparamos por puntos (mayor a menor)
        	            cmp = Integer.compare(r2.getPuntos(), r1.getPuntos());
        	        }
        	        return cmp;
        	    })
        	    .limit(10)
        	    .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Top 10 Jugadores:\n");
        for (int i = 0; i < top10.size(); i++) {
            Rango r = top10.get(i);
            sb.append((i + 1)).append(". ")
              .append(r.getJugador())
              .append(" - ").append(r.getRango()+" ").append(r.getSubRango() +" (").append(r.getPuntos()+" Puntos)")
              .append("\n");
        }
        for (int i = top10.size(); i < 10; i++) {
            sb.append("\n");
        }
        return sb.toString();
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

}

