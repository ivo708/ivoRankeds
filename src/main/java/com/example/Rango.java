package com.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.network.ServerPlayerEntity;
enum Rangos{
    Entrenador,
    Lider,
    AltoMando,
    Campeon
}
public class Rango {

	private String jugador;
	private UUID uuid; 
    private Rangos rango;
    private int subRango;
    private int puntos;
    
    
	public String getJugador() {
		return jugador;
	}	
	
	public Rango(String jugador, UUID uuid, Rangos rango, int subRango, int puntos) {
		super();
		this.jugador = jugador;
		this.uuid = uuid;
		this.rango = rango;
		this.subRango = subRango;
		this.puntos = puntos;
	}

	public void setJugador(String jugador) {
		this.jugador = jugador;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	public Rangos getRango() {
		return rango;
	}
	
	public void setRango(Rangos rango) {
		this.rango = rango;
	}
	
	public int getSubRango() {
		return subRango;
	}
	
	public void setSubRango(int subRango) {
		this.subRango = subRango;
	}
	
	public int getPuntos() {
		return puntos;
	}
	
	public void setPuntos(int puntos) {
    	if(puntos>99 && this.rango.ordinal()!=3) {
    		subirSubRango();
    	}
    	else if(puntos<0 && !(this.rango.ordinal()==0 && this.subRango==3)) {
    		bajarSubRango();
    	}
    	else if(puntos<0 && (this.rango.ordinal()==0 && this.subRango==3)) {
    		this.puntos = 0;
    	}
    	else {
    		this.puntos = puntos;
    	}
	}
	
	public void subirSubRango() {
		this.puntos=0;
		updateLP(jugador,true);
		if(this.subRango>1) {
			this.subRango=this.subRango-1;
		}
		else {
			subirRango();
		}
	}
	public void bajarSubRango() {
		this.puntos=66;
        updateLP(jugador,false);
		if(this.subRango<3) {
			this.subRango=this.subRango+1;
		}
		else {
			bajarRango();
		}
	}
	
	
	public void subirRango() {
		this.rango=Rangos.values()[this.rango.ordinal()+1];
		this.subRango=3;
        
    }
	
	
	public void bajarRango() {
		this.rango=Rangos.values()[this.rango.ordinal()-1];
		this.subRango=1;
	}
	
	
    public void guardar() {
        Path directory = Paths.get("config/ivorankeds");
        if (!Files.exists(directory)) {
            try {
				Files.createDirectories(directory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        Path path = directory.resolve(this.jugador+".json");
        Gson gson = new Gson();
        // Convertir la instancia actual a JSON
        String json = gson.toJson(this);
        try {
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    public static Rango cargar(ServerPlayerEntity jugador) {
        Path directory = Paths.get("config/ivorankeds");
        
        // Crear el directorio si no existe
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Manejo de error según convenga
        }
        
        Path path = directory.resolve(jugador.getName().getLiteralString() + ".json");
        Gson gson = new Gson();
        
        if (!Files.exists(path)) {
            System.out.println("Archivo no encontrado, se inicializa Rango por defecto");
            Rango rangoDefault = new Rango(jugador.getName().getLiteralString(), jugador.getUuid(), Rangos.Entrenador, 3, 50);
            try {
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(rangoDefault);
                Files.write(path, json.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateLP(jugador.getName().getLiteralString(),true);
            return rangoDefault;
        }
        
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return gson.fromJson(json, Rango.class);
        } catch (IOException e) {
            e.printStackTrace();
            updateLP(jugador.getName().getLiteralString(),true);
            return new Rango(jugador.getName().getLiteralString(), jugador.getUuid(), Rangos.Entrenador, 3, 50);
        }
    }
    
    public static void tellRango(ServerPlayerEntity jugador) {
    	Rango rango=cargar(jugador);
    	tellraw(jugador,jugador.getName().getLiteralString(),"ACTUALMENTE ESTÁS EN: "+rango.getRango()+" "+rango.getSubRango()+" ("+rango.getPuntos()+" Puntos)","gold");    	
    }
    
    public static void tellraw(ServerPlayerEntity jugador,String target,String mensaje, String color) {
    	String cmd = "tellraw "+target+" {\"text\":\"\\n"+mensaje+"\\n\",\"bold\":true,\"italic\":true,\"color\":\""+color+"\"}";
    	jugador.getServer().getCommandManager().executeWithPrefix(jugador.getServer().getCommandSource(),cmd);
    }
    
    public static void updateLP(String jugador,boolean promote) {
        if (Ivorankeds.config == null) {
            System.out.println("La configuración aún no está inicializada. Abortando updateLP.");
            return;
        }
        if(!Ivorankeds.config.get("lpIntegration").getAsBoolean()) {
        	return;
        }
        String rangoCmd;
        if(promote) {
        	rangoCmd="promote ligapokemon";
        }
        else {
        	rangoCmd="demote ligapokemon";

        }
    	//AÑADIR COMANDO
    	String cmd="lp user "+jugador+ " " +rangoCmd;
    	Ivorankeds.server.getCommandManager().executeWithPrefix(Ivorankeds.server.getCommandSource(), cmd);
    }
}
