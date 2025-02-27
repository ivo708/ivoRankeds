package com.example;

import java.util.UUID;

public class BattleRegistry {
    public String uuidP1;
    public String uuidP2;
    public String P1;
    public String P2;
    public String uuidBattle;
    public String winner;
	public long matchEpoch;

    
	public BattleRegistry(String uuidP1, String uuidP2, String p1, String p2,String uuidBattle, String winner, long matchEpoch) {
	    // Convertir las cadenas a objetos UUID para poder compararlas
	    UUID u1 = UUID.fromString(uuidP1);
	    UUID u2 = UUID.fromString(uuidP2);	    
	    if (u1.compareTo(u2) <= 0) {
	        this.uuidP1 = uuidP1;
	        this.uuidP2 = uuidP2;
	        this.P1 = p1;
	        this.P2 = p2;
	    } else {
	        this.uuidP1 = uuidP2;
	        this.uuidP2 = uuidP1;
	        this.P1 = p2;
	        this.P2 = p1;
	    }
	    this.uuidBattle = uuidBattle;
	    this.winner = winner;
	    this.matchEpoch = matchEpoch;
	}

}
