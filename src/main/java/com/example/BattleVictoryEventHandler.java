package com.example;

import java.util.List;
import java.util.UUID;

import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.example.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

    private boolean isPlayerVictory(BattleVictoryEvent event) {
        ServerPlayerEntity player = event.getBattle().getPlayers().get(0);
        return event.getWinners().stream().anyMatch(battleActor -> battleActor.isForPlayer(player));
    }

    private void onPlayerVictory(ServerPlayerEntity jugador,ServerPlayerEntity oponente) {
    	updateBattleWinner(jugador,oponente);
    	Rango rango=Rango.cargar(jugador);
    	Rango rangoOponente=Rango.cargar(oponente);
    	int puntos=rango.getPuntos();
    	int rangoGanador= (rango.getRango().ordinal()*3)+ (4-rango.getSubRango());
    	int rangoPerdedor= (rangoOponente.getRango().ordinal()*3)+ (4-rangoOponente.getSubRango());
    	
    	int ganancia=30+(2*(rangoPerdedor-rangoGanador));
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

}

