package com.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.GenericContainerScreenHandler;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


import net.minecraft.inventory.SimpleInventory;

public class TeamDisplay {

    public static void openMenu(ServerPlayerEntity player,ServerPlayerEntity opponent) {
        int rows = 3;
        SimpleInventory inventory = new SimpleInventory(rows * 9) {
            @Override
            public boolean isValid(int slot, ItemStack stack) {
                return false;
            }
        };
        int[] pokemonPositions = {10, 12, 14, 16};
    	Style style = Style.EMPTY.withItalic(false);
		Style white = style.withColor(TextColor.parse("white").getOrThrow());
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(opponent);
        List<BattlePokemon> team = party.toBattleTeam(false, false, party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid());
        for (int i = 0; i < team.size() && i < pokemonPositions.length; i++) {
            Pokemon pokemon = team.get(i).getOriginalPokemon();
            ItemStack item = PokemonItem.from(pokemon);
            List<Text> textList = new ArrayList<>(getLore(pokemon));
            LoreComponent loreComponent = new LoreComponent(textList);
            item.set(DataComponentTypes.LORE, loreComponent);
            inventory.setStack(pokemonPositions[i], item);
        }	
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) -> {
            return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, rows) {
                @Override
                public boolean canUse(net.minecraft.entity.player.PlayerEntity player) {
                    return true; // Permite que el jugador solo vea el inventario sin interacción
                }

                @Override
                public void onSlotClick(int slotIndex, int button, SlotActionType actionType, net.minecraft.entity.player.PlayerEntity player) {
                    // Evita cualquier acción de clic en los ítems
                }
            };
        }, Text.literal("Equipo de "+opponent.getName().getLiteralString()).setStyle(white) ));
    }
    
    private static Collection<MutableText> getLore(Pokemon pokemon) {
    	Style style = Style.EMPTY.withItalic(false);

    	Collection<MutableText> lore = new ArrayList<>();
    	Style dark_green = style.withColor(TextColor.parse("dark_green").getOrThrow());
    	Style green = style.withColor(TextColor.parse("green").getOrThrow());
		Style gold = style.withColor(TextColor.parse("gold").getOrThrow());
		Style white = style.withColor(TextColor.parse("white").getOrThrow());
		Style dark_blue = style.withColor(TextColor.parse("dark_blue").getOrThrow());
		Style blue = style.withColor(TextColor.parse("blue").getOrThrow());

    	lore.add(Text.empty().setStyle(dark_green).append("Pokémon").setStyle(dark_green)
    	    .append(": ")
    	    .append(pokemon.getSpecies().getTranslatedName().setStyle(green)));
		lore.add(Text.empty().setStyle(dark_green).append("Item: ").setStyle(dark_blue).append(Text.translatable(pokemon.getHeldItem$common().getTranslationKey()).setStyle(blue)));
    	MutableText types=Text.empty().setStyle(green);
		for (ElementalType type : pokemon.getSpecies().getTypes()) {
			types.append(" ").append(type.getDisplayName());
		}
		lore.add(Text.empty().setStyle(dark_green).append("Tipos").setStyle(dark_green)
				.append(":")
				.append(types)
				);
    	lore.add(Text.empty().setStyle(dark_green).append("Naturaleza: ").setStyle(dark_green)
    	    .append(Text.translatable(pokemon.getNature().getDisplayName()).setStyle(green)));
    	lore.add(Text.empty().setStyle(dark_green).append("Habilidad: ").setStyle(dark_green)
        	    .append(Text.translatable(pokemon.getAbility().getDisplayName()).setStyle(green)));
		lore.add(Text.empty().setStyle(dark_green).append("Movimientos").setStyle(gold).append(": "));
		for (Move move : pokemon.getMoveSet().getMoves()) {
			lore.add(Text.empty().setStyle(dark_green).append(move.getTemplate().getDisplayName().getString()).setStyle(white));
		}
    	return lore;
    }
}
