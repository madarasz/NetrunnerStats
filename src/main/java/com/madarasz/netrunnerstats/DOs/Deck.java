package com.madarasz.netrunnerstats.DOs;

import com.madarasz.netrunnerstats.DOs.relationships.DeckHasCard;
import org.springframework.data.neo4j.annotation.*;

import java.util.Set;

/**
 * Created by madarasz on 2015-06-09.
 */
@NodeEntity
public class Deck {
    @GraphId Long id;
    @RelatedToVia(type = "HAS_CARD") private @Fetch Set<DeckHasCard> cards;
    private String name;
    private String player;
    private String url;

    public Deck() {
    }

    public Deck(String name, String player, String url) {
        this.name = name;
        this.player = player;
        this.url = url;
    }

    public DeckHasCard hasCard(Card card, int quantity) {
        DeckHasCard deckHasCard = new DeckHasCard(this, card, quantity);
        this.cards.add(deckHasCard);
        return deckHasCard;
    }

    public Set<DeckHasCard> getCards() {
        return cards;
    }

    public String getName() {
        return name;
    }

    public String getPlayer() {
        return player;
    }

    public String getUrl() {
        return url;
    }

    // TODO
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
