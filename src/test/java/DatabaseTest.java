import com.madarasz.netrunnerstats.Application;
import com.madarasz.netrunnerstats.DOs.Card;
import com.madarasz.netrunnerstats.DOs.CardPack;
import com.madarasz.netrunnerstats.DOs.Deck;
import com.madarasz.netrunnerstats.DOs.Tournament;
import com.madarasz.netrunnerstats.DOs.relationships.DeckHasCard;
import com.madarasz.netrunnerstats.DOs.relationships.TournamentHasDeck;
import com.madarasz.netrunnerstats.DRs.CardPackRepository;
import com.madarasz.netrunnerstats.DRs.CardRepository;
import com.madarasz.netrunnerstats.DRs.DeckRepository;
import com.madarasz.netrunnerstats.DRs.TournamentRepository;
import com.madarasz.netrunnerstats.Operations;
import com.madarasz.netrunnerstats.brokers.AcooBroker;
import com.madarasz.netrunnerstats.brokers.NetrunnerDBBroker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests for DB
 * Created by madarasz on 2015-06-11.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@Transactional
public class DatabaseTest {

    @Autowired
    Operations operations;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    CardPackRepository cardPackRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    TournamentRepository tournamentRepository;

    @Autowired
    NetrunnerDBBroker netrunnerDBBroker;

    @Autowired
    AcooBroker acooBroker;

    @Autowired
    Neo4jOperations template;

    /**
     * Checking that DB can be populated.
     * NetrunnerDB: cards, card packs, decks
     * Acoo: decks, tournaments
     */
    @Test
    public void createDB() {
        // populate DB
        operations.cleanDB();
        populateDB();
        Assert.assertTrue("No cards in DB.", template.count(Card.class) > 0);
        Assert.assertTrue("No card packs in DB.", template.count(CardPack.class) > 0);
        Assert.assertTrue("No decks in DB.", template.count(Deck.class) > 1);
        Assert.assertTrue("No deck-card relationships in DB.", template.count(DeckHasCard.class) > 0);
        Assert.assertTrue("No tournaments in DB.", template.count(Tournament.class) > 0);
    }

    /**
     * Checking that duplicates are not saved.
     * NetrunnerDB: cards, card packs, decks
     * Acoo: decks, tournaments
     */
    @Test
    public void duplicatesDB() {
        // populate DB
        operations.cleanDB();
        populateDB();
        long countCard = template.count(Card.class);
        long countCardPack = template.count(CardPack.class);
        long countDeck = template.count(Deck.class);
        long countDeckHasCard = template.count(DeckHasCard.class);
        long countTournaments = template.count(Tournament.class);
        // try reading same data
        populateDB();

        Assert.assertTrue("Duplicate cards should not be added.", countCard == template.count(Card.class));
        Assert.assertTrue("Duplicate card packs should not be added.", countCardPack == template.count(CardPack.class));
        Assert.assertTrue("Duplicate decks should not be added.", countDeck == template.count(Deck.class));
        Assert.assertTrue("Duplicate deck-card relationships should not be added.", countDeckHasCard == template.count(DeckHasCard.class));
        Assert.assertTrue("Duplicate tournaments should not be added.", countTournaments == template.count(Tournament.class));
    }

    @Test
    public void retrieveData() {
        // populate DB
        operations.cleanDB();
        populateDB();

        Card card = cardRepository.findByTitle("Déjà Vu");
        CardPack cardPack = cardPackRepository.findByName("Core Set");
        Deck deck_nrdb = deckRepository.findByUrl(netrunnerDBBroker.deckUrlFromId(20162));
        Deck deck_acoo = deckRepository.findByUrl(acooBroker.deckUrlFromId(10890));
        Tournament tournament = tournamentRepository.findByUrl(acooBroker.tournamentUrlFromId(526));

        // positive assertions
        Assert.assertTrue("Could not retrieve card.", card != null);
        Assert.assertTrue("Could not retrieve card pack.", cardPack != null);
        Assert.assertTrue("Could not retrieve NetrunnerDB deck.", deck_nrdb != null);
        Assert.assertTrue("Could not retrieve Acoo deck.", deck_acoo != null);
        Assert.assertTrue("Could not retrieve Acoo tournament.", tournament != null);

        // value assertions
        System.out.println("Retrieved card: " + card.toString());
        System.out.println("Retrieved card pack: " + cardPack.toString());
        System.out.println("Retrieved NetrunnerDB deck: " + deck_nrdb.toString());
        System.out.println("Retrieved Acoo deck: " + deck_acoo.toString());
        System.out.println("Retrieved tournament: " + tournament.toString());
        Assert.assertTrue("NetrunnerDB deck info is not correct.",
                deck_nrdb.toString().equals("RP with the flow (Jinteki: Replicating Perfection) - 49 cards (15 inf) up to: The Source - http://netrunnerdb.com/api/decklist/20162"));
        Assert.assertTrue("Acoo deck info is not correct",
                deck_acoo.toString().equals("Everything (The Professor: Keeper of Knowledge) - 45 cards (1 inf) up to: The Valley - http://www.acoo.net/deck/10890"));
        Assert.assertTrue("Tournament info is not correct",
                tournament.toString().equals("Regional Paris Ludiworld (2015-06-07) - 47 players - cardpool: Breaker Bay - http://www.acoo.net/anr-tournament/526"));

        // negative assertions
        Assert.assertTrue("Could retrieve non-existent card.", cardRepository.findByTitle("I love Siphon") == null);
        Assert.assertTrue("Could not retrieve non-existent card pack.", cardPackRepository.findByName("No such pack") == null);
        Assert.assertTrue("Could not retrieve non-existent deck.", deckRepository.findByUrl("http://www.google.com") == null);
    }

    @Test
    public void cardCaseCheck() {
        // populate DB
        operations.cleanDB();
        populateDB();

        Card card = cardRepository.findByTitle("I've had worse");
        Assert.assertTrue("Could not retrive card by worng case.", card != null);
        System.out.println("Retrieved card: " + card.toString());
    }

    @Test
    public void acooTournament() {
        operations.cleanDB();
        operations.loadNetrunnerDB();
        operations.loadAcooTournamentDecks(526);
        operations.logDBCount();
        long countDecks = template.count(Deck.class);
        long countTournament = template.count(Tournament.class);
        long countTournamentHasDeck = template.count(TournamentHasDeck.class);

        Assert.assertTrue("Did not load any decks.", countDecks > 0);
        Assert.assertTrue("Did not load any tournaments.", countTournament > 0);
        Assert.assertTrue("Did not load any tournament-deck relations.", countTournamentHasDeck > 0);

        // duplicate checks
        operations.loadAcooTournamentDecks(526);
        operations.logDBCount();
        Assert.assertTrue("Duplicate decks should not be added.", countDecks == template.count(Deck.class));
        Assert.assertTrue("Duplicate tournaments should not be added.", countTournament == template.count(Tournament.class));
        Assert.assertTrue("Duplicate tournament-deck relations should not be added.", countTournamentHasDeck == template.count(TournamentHasDeck.class));
    }

    private void populateDB() {
        operations.loadNetrunnerDB();
        operations.loadAcooDeck(10890);
        operations.loadNetrunnerDbDeck(20162);
        operations.loadAcooTournament(526);
        operations.logDBCount();
    }
}
