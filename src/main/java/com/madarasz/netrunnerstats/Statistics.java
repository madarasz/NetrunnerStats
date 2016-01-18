package com.madarasz.netrunnerstats;

import com.madarasz.netrunnerstats.database.DOs.Card;
import com.madarasz.netrunnerstats.database.DOs.CardPack;
import com.madarasz.netrunnerstats.database.DOs.Deck;
import com.madarasz.netrunnerstats.database.DOs.relationships.DeckHasCard;
import com.madarasz.netrunnerstats.database.DOs.result.CardCounts;
import com.madarasz.netrunnerstats.database.DOs.result.StatCounts;
import com.madarasz.netrunnerstats.database.DOs.stats.*;
import com.madarasz.netrunnerstats.database.DOs.stats.entries.*;
import com.madarasz.netrunnerstats.database.DRs.*;
import com.madarasz.netrunnerstats.database.DRs.stats.*;
import com.madarasz.netrunnerstats.helper.*;
import com.madarasz.netrunnerstats.helper.comparator.CardCountComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.*;

/**
 * Created by madarasz on 11/7/15.
 * Handling statistical functions.
 */
@Component
public class Statistics {

    private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

    @Autowired
    CardRepository cardRepository;

    @Autowired
    CardPackRepository cardPackRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    TournamentRepository tournamentRepository;

    @Autowired
    StandingRepository standingRepository;

    @Autowired
    DPStatsRepository dpStatsRepository;

    @Autowired
    MultiDimensionalScaling multiDimensionalScaling;

    @Autowired
    IdentityMDSRepository identityMdsRepository;

    @Autowired
    CardPoolStatsRepository cardPoolStatsRepository;

    @Autowired
    DeckInfosRepository deckInfosRepository;

    @Autowired
    DPIdentitiesRepository dpIdentitiesRepository;

    @Autowired
    CardUsageStatsRepository cardUsageStatsRepository;

    @Autowired
    IdentityAverageRepository identityAverageRepository;

    @Autowired
    CardStatRepository cardStatRepository;

    @Autowired
    DeckDigest deckDigest;

    @Autowired
    ColorPicker colorPicker;

    @Autowired
    Operations operations;

    /**
     * Log statistics about deck and stat count for each identity for a cardpool legality
     * @param cardpackName cardpool name
     * @param top just top decks?
     * @return DPStatistics
     */
    public DPStatistics getPackStats(String cardpackName, boolean top) {
        DPStatistics statistics;
        if (top) {
            statistics = dpStatsRepository.findByDpnameOnlyTop(cardpackName);
        } else {
            statistics = dpStatsRepository.findByDpnameAll(cardpackName);
        }

        if (statistics == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            // total count
            int deckcount;
            int standcount;
            int standcountrunner;
            int standcountcorp;
            if (top) {
                deckcount = deckRepository.countTopByCardpool(cardpackName);
                standcount = standingRepository.countTopByCardPool(cardpackName);
                standcountcorp = standingRepository.countTopByCardPoolSidecode(cardpackName, "corp");
                standcountrunner = standingRepository.countTopByCardPoolSidecode(cardpackName, "runner");
            } else {
                deckcount = deckRepository.countByCardpool(cardpackName);
                standcount = standingRepository.countByCardPool(cardpackName);
                standcountcorp = standingRepository.countByCardPoolSidecode(cardpackName, "corp");
                standcountrunner = standingRepository.countByCardPoolSidecode(cardpackName, "runner");
            }
            statistics = new DPStatistics(cardpackName, deckcount, standcount, standcountrunner, standcountcorp, top);
            logger.debug("*********************************************************************");
            logger.debug(String.format("Stats for cardpool: %s (%d decks, %d standings)",
                    cardpackName, deckRepository.countByCardpool(cardpackName), standingRepository.countByCardPool(cardpackName)));
            logger.debug("*********************************************************************");

            List<StatCounts> stats;
            if (top) {
                stats = standingRepository.getTopIdentityStatsByCardPool(cardpackName);
            } else {
                stats = standingRepository.getIdentityStatsByCardPool(cardpackName);
            }

            for (StatCounts stat : stats) {
                String identity = stat.getCategory();
                // count
                int statcount = stat.getCount();
                int count;
                if (top) {
                    count = deckRepository.countTopByIdentityAndCardPool(identity, cardpackName);
                } else {
                    count = deckRepository.countByIdentityAndCardPool(identity, cardpackName);
                }
                // adding stats
                if (stat.getSideCode().equals("runner")) {
                    statistics.addRunnerIdentity(identity, count, statcount, (double)statcount / standcountrunner, colorPicker.colorIdentity(identity));
                } else {
                    statistics.addCorpIdentity(identity, count, statcount, (double)statcount / standcountcorp, colorPicker.colorIdentity(identity));
                }
                logger.debug(String.format("%s - %d (%d)", identity, statcount,
                        deckRepository.countTopByIdentityAndCardPool(identity, cardpackName)));
            }

            logger.debug("*********************************************************************");
            if (top) {
                stats = standingRepository.getTopFactionStatsByCardPool(cardpackName);
            } else {
                stats = standingRepository.getFactionStatsByCardPool(cardpackName);
            }
            for (StatCounts stat : stats) {
                String faction = stat.getCategory();
                // count
                int count;
                int statcount = stat.getCount();
                if (top) {
                    count = deckRepository.countTopByCardPoolAndFaction(cardpackName, faction);
                } else {
                    count = deckRepository.countByCardPoolAndFaction(cardpackName, faction);
                }
                // adding stats
                if (stat.getSideCode().equals("runner")) {
                    statistics.addRunnerFaction(faction, count, statcount, (double)statcount / standcountrunner, colorPicker.colorFaction(faction));
                } else {
                    statistics.addCorpFaction(faction, count, statcount, (double)statcount / standcountcorp, colorPicker.colorFaction(faction));
                }
                logger.debug(String.format("%s - %d (%d)", faction, statcount, deckRepository.countTopByCardPoolAndFaction(cardpackName, faction)));
            }

            stopwatch.stop();
            logger.debug("*********************************************************************");
            logger.info(String.format("Saving DP Statistics: %s - top: %s (%.3f sec)", cardpackName, top,
                    stopwatch.getTotalTimeSeconds()));
            dpStatsRepository.save(statistics);
        }
        return statistics;
    }

    /**
     * Generate multidimensional scaling for identity and cardpool legality
     * @param identityName filter for identity
     * @param cardpackName filter for cardpool
     * @return IdentityMDS
     */
    public IdentityMDS getPackMath(String identityName, String cardpackName) {
        IdentityMDS result = identityMdsRepository.findByDpnameIdentitytitle(cardpackName, identityName);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            ArrayList<Deck> decks = new ArrayList<>(deckRepository.filterByIdentityAndCardPool(identityName, cardpackName));
            ArrayList<Deck> topdecks = new ArrayList<>(deckRepository.filterTopByIdentityAndCardPool(identityName, cardpackName));

            // TODO: better topdecks contain
            ArrayList<String> topURLs = new ArrayList<>();
            for (Deck topdeck : topdecks) {
                topURLs.add(topdeck.getUrl());
            }

            int decknum = deckRepository.countByIdentityAndCardPool(identityName, cardpackName);
            int topdecknum = deckRepository.countTopByIdentityAndCardPool(identityName, cardpackName);
            result = new IdentityMDS(cardpackName, identityName, decknum, topdecknum);
            if (decknum > 0) {
                double[][] distance = multiDimensionalScaling.getDistanceMatrix(decks);
                double[][] scaling = multiDimensionalScaling.calculateMDS(distance);
                for (int i = 0; i < decks.size(); i++) {
                    // NaN fix
                    scaling[0][i] = NaNFix(scaling[0][i]);
                    scaling[1][i] = NaNFix(scaling[1][i]);

                    MDSEntry mds = new MDSEntry(scaling[0][i], scaling[1][i],
                            decks.get(i).getName(), decks.get(i).getUrl(), topURLs.contains(decks.get(i).getUrl()));
                    result.addDeck(mds);
                    logger.debug(String.format("\"%s\",\"%f\",\"%f\"", decks.get(i).getUrl(), scaling[0][i], scaling[1][i]));
                }
            }
            stopwatch.stop();
            logger.debug("*********************************************************************");
            logger.info(String.format("Saving MDS Statistics: %s - %s (%.3f sec)", cardpackName, identityName,
                    stopwatch.getTotalTimeSeconds()));
            identityMdsRepository.save(result);
        }
        return result;
    }

    /**
     * Generates indentity deck groups with links for MDS based on cardpool
     * @param cardpackName cardpool
     * @param sidecode runner or corp
     * @return DPIdentities
     */
    public DPIdentities getIdentityLinksForDataPack(String cardpackName, String sidecode) {
        DPIdentities result = dpIdentitiesRepository.findByCardpoolSidecode(cardpackName, sidecode);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            result = new DPIdentities(cardpackName, sidecode);
            DPStatistics stats = getPackStats(cardpackName, false);

            Set<CountDeckStands> data;
            switch (sidecode) {
                case "runner":
                    data = stats.getRunnerIdentities();
                    break;
                case "corp":
                    data = stats.getCorpIdentities();
                    break;
                default:
                    return new DPIdentities();
            }

            for (CountDeckStands identity : data) {
                int topdecknum = identity.getDecknum();
                String title = identity.getTitle();
                int decknum = deckRepository.countByIdentityAndCardPool(title, cardpackName);
                if (decknum > 0) {
                    DPIdentity entry = new DPIdentity(title,
                            String.format("/MDSIdentity/%s/%s", cardpackName, title),
                            decknum, topdecknum);
                    result.addIdentitiy(entry);
                }
            }
            stopwatch.stop();
            logger.info(String.format("Saving DPIdentities: %s - %s (%.3f sec)", cardpackName, sidecode,
                    stopwatch.getTotalTimeSeconds()));
            dpIdentitiesRepository.save(result);
        }
        return result;
    }

    /**
     * Generates cardpool statistics: deck number, ranking number, tournament number per cardpool
     * @return CardPoolStats
     */
    public CardPoolStats getCardPoolStats() {
        CardPoolStats result = cardPoolStatsRepository.find();
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            result = new CardPoolStats();
            List<CardPack> available = cardPackRepository.findWithStandings();
            for (CardPack cardPack : available) {
                String title = cardPack.getName();
                int standingsnum = standingRepository.countByCardPool(title);
                int decknum = deckRepository.countByCardpool(title);
                int tournamentnum = tournamentRepository.countByCardpool(title);
                CardPool cardPool = new CardPool(title, tournamentnum, decknum, standingsnum,
                        cardPack.getNumber(), cardPack.getCyclenumber());
                result.addCardPool(cardPool);
            }
            stopwatch.stop();
            logger.info(String.format("Saving Cardpool Statistics. (%.3f sec)", stopwatch.getTotalTimeSeconds()));
            cardPoolStatsRepository.save(result);
        }
        return result;
    }

    /**
     * Generates html digest of decks for certain indentity and cardpool
     * @param identityName identity name
     * @param cardpool cardpool name
     * @return DeckInfos
     */
    public DeckInfos getDeckInfos(String identityName, String cardpool) {
        DeckInfos result = deckInfosRepository.findByCardpoolIdentityname(cardpool, identityName);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            List<Deck> decks = deckRepository.filterByIdentityAndCardPool(identityName, cardpool);
            result = new DeckInfos(cardpool, identityName);
            for (Deck deck : decks) {
                DeckInfo info = getDeckInfo(deck);
                result.addDeckInfo(info);
            }
            stopwatch.stop();
            logger.info(String.format("Saving DeckInfos Statistics: %s - %s (%.3f sec)", cardpool, identityName,
                    stopwatch.getTotalTimeSeconds()));
            deckInfosRepository.save(result);
        }
        return result;
    }

    public DeckInfo getDeckInfo(Deck deck) {
        return new DeckInfo(deckDigest.shortHtmlDigest(deck), deckDigest.htmlDigest(deck),
                deckDigest.digest(deck), deck.getUrl());
    }

    /**
     * Calculates most used a card in the cardpack
     * @param cardpack cardpack name
     * @return CardUsageStat
     */
    public CardUsageStat getMostUsedCardsFromCardPack(String cardpack) {
        CardUsageStat result = cardUsageStatsRepository.findByCardPackName(cardpack);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            int runnerdecks = getDeckNumberFromCardpoolOnward(cardpack, "runner", false);
            int toprunnerdecks = getDeckNumberFromCardpoolOnward(cardpack, "runner", true);
            int corpdecks = getDeckNumberFromCardpoolOnward(cardpack, "corp", false);
            int topcorpdecks = getDeckNumberFromCardpoolOnward(cardpack, "corp", true);
            result = new CardUsageStat(cardpack, false,
                    runnerdecks, toprunnerdecks, corpdecks, topcorpdecks);
            List<Card> cards = cardRepository.findByCardPackName(cardpack);
            for (Card card : cards) {
                String code = card.getCode();
                int count = deckRepository.countByUsingCard(code);
                if (count > 0) {
                    int topcount = deckRepository.countTopByUsingCard(code);
                    if (card.getSide_code().equals("runner")) {
                        result.addCardUsage(new CardUsage(card.getTitle(), card.getCardPack().getName(),
                                card.getSide_code(), card.getFaction_code(), count, topcount,
                                (double)count / runnerdecks, (double)topcount / toprunnerdecks));
                    } else {
                        result.addCardUsage(new CardUsage(card.getTitle(), card.getCardPack().getName(),
                                card.getSide_code(), card.getFaction_code(), count, topcount,
                                (double)count / corpdecks, (double)topcount / topcorpdecks));
                    }
                } else {
                        result.addCardUsage(new CardUsage(card.getTitle(), card.getCardPack().getName(),
                            card.getSide_code(), card.getFaction_code(), 0, 0, 0, 0));
                }
            }
            stopwatch.stop();
            logger.info(String.format("Saving Card Usage Statistics, cardpack: %s (%.3f sec)", cardpack,
                    stopwatch.getTotalTimeSeconds()));
            cardUsageStatsRepository.save(result);
        }
        return result;
    }

    /**
     * Calculates most used a card with a cardpool of tournaments
     * @param cardpool cardpool name
     * @return CardUsageStat
     */
    public CardUsageStat getMostUsedCardsForCardpool(String cardpool) {
        CardUsageStat result = cardUsageStatsRepository.findByCardPoolName(cardpool);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            int toprunnerdecks = deckRepository.countTopByCardpoolAndSide(cardpool, "runner");
            int topcorpdecks = deckRepository.countTopByCardpoolAndSide(cardpool, "corp");
            result = new CardUsageStat(cardpool, true, -1, toprunnerdecks, -1, topcorpdecks);
            List<CardCounts> stat = cardRepository.findMostPopularCardsByCardPack(cardpool, "runner");
            for (CardCounts count : stat) {
                result.addCardUsage(new CardUsage(count.getTitle(), count.getCardpack(), "runner", count.getFaction(), -1, count.getCount(),
                        -1, (double)count.getCount() / toprunnerdecks));
                logger.debug(String.format("%s (%s) - %d", count.getTitle(), count.getCardpack(), count.getCount()));
            }
            stat = cardRepository.findMostPopularCardsByCardPack(cardpool, "corp");
            for (CardCounts count : stat) {
                result.addCardUsage(new CardUsage(count.getTitle(), count.getCardpack(), "corp", count.getFaction(), -1, count.getCount(),
                        -1, (double)count.getCount() / topcorpdecks));
                logger.debug(String.format("%s (%s) - %d", count.getTitle(), count.getCardpack(), count.getCount()));
            }
            stopwatch.stop();
            logger.info(String.format("Saving Card Usage Statistics, cardpool: %s (%.3f sec)", cardpool,
                    stopwatch.getTotalTimeSeconds()));
            cardUsageStatsRepository.save(result);
        }
        return result;
    }

    /**
     * Calculates average card usage statistics for identity and cardpool.
     * @param identity identity title
     * @param cardpool cardpool name
     * @return IdentityAverage
     */
    public IdentityAverage getIdentityAverage(String identity, String cardpool) {
        IdentityAverage result = identityAverageRepository.findIdentityCardPool(identity, cardpool);
        if (result == null) {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            result = new IdentityAverage(identity, cardpool);
            List<Deck> decks = deckRepository.filterByIdentityAndCardPool(identity, cardpool);
            Set<Card> cards = new HashSet<>();
            int decknum = decks.size();

            // get all used cards
            for (Deck deck : decks) {
                for (DeckHasCard card : deck.getCards()) {
                    if (!cards.contains(card.getCard())) {
                        cards.add(card.getCard());
                    }
                }
            }

            // examine all used cards
            for (Card card : cards) {
                int used = 0;
                int sumused = 0;
                String cardcode = card.getCode();
                for (Deck deck : decks) {
                    for (DeckHasCard deckHasCard : deck.getCards()) {
                        if (cardcode.equals(deckHasCard.getCard().getCode())) {
                            used++;
                            sumused += deckHasCard.getQuantity();
                        }
                    }
                }
                CardAverage cardAverage = new CardAverage(card,
                        String.format("%.1f%%", (double) used / decknum * 100),
                        String.format("%.2f", (double) sumused / decknum),
                        String.format("%.2f", (double) sumused / used));
                result.addCard(cardAverage);
            }
            stopwatch.stop();
            logger.info(String.format("Saving deck averages: %s - %s (%.3f)", identity, cardpool,
                    stopwatch.getTotalTimeSeconds()));
            identityAverageRepository.save(result);
        }
        return result;
    }

    /**
     * Returns the number of decks from a cardpool and onward
     * @param cardpool cardpool name
     * @param side side_code
     * @param topdeck just top decks?
     * @return deck number
     */
    public int getDeckNumberFromCardpoolOnward(String cardpool, String side, boolean topdeck) {
        List<CardPool> cardPools = getCardPoolStats().getSortedCardpool();
        Collections.reverse(cardPools);
        CardPack cardPack = cardPackRepository.findByName(cardpool);
        int result = 0;
        boolean count = false;
        for (CardPool currentPool : cardPools) {
            if (currentPool.getCyclenumber()*100 + currentPool.getDpnumber()
                    >= cardPack.getCyclenumber()*100 + cardPack.getNumber()) {
                count = true;
            }
            if (count) {
                if (topdeck) {
                    result += deckRepository.countTopByCardpoolAndSide(currentPool.getTitle(), side);
                } else {
                    result += deckRepository.countByCardpoolAndSide(currentPool.getTitle(), side);
                }
            }
        }
        return result;
    }

    /**
     * Statistics for a single card.
     * Includes usage over time, top cards/identities, possible combos, deck links.
     * @param cardTitle card title
     * @return
     */
    public CardStat getCardStats(String cardTitle) {
        CardStat result = cardStatRepository.findbyTitle(cardTitle);
        if (result == null) {
            Card card = cardRepository.findByTitle(cardTitle);

            if (card != null) {
                StopWatch stopwatch = new StopWatch();
                stopwatch.start();

                result = new CardStat(card);
                String side = card.getSide_code();
                List<CardPool> cardPools = getCardPoolStats().getSortedCardpool();
                List<CardPool> validPools = new ArrayList<>(cardPools);
                Collections.reverse(cardPools);
                CardCountComparator comparator = new CardCountComparator();

                // deck numbers over time
                for (CardPool cardPool : cardPools) {
                    String pack = cardPool.getTitle();
                    if (!card.getCardPack().later(cardPackRepository.findByName(pack))) {
                        int decknum = deckRepository.countByCardpoolAndSide(pack, side);
                        int topdecknum = deckRepository.countTopByCardpoolAndSide(pack, side);
                        int using = deckRepository.countByCardpoolUsingCard(pack, cardTitle);
                        int topusing = deckRepository.countTopByCardpoolUsingCard(pack, cardTitle);
                        result.addOverTime(
                                new CardUsage(cardTitle, pack, side, card.getFaction_code(), using, topusing,
                                        (float) using / decknum, (float) topusing / topdecknum));
                        logger.debug(String.format("%s - top:%,.3f%% all:%,.3f%%", pack,
                                (float) topusing / topdecknum, (float) using / decknum));
                    } else {
                        validPools.remove(cardPool);
                    }
                }

                // statistics on last three packs
                // get all decks using card
                List<CardPool> last3Pools = new ArrayList<>(validPools);
                last3Pools = trimCardPool(last3Pools, 3);
                int alldeck = 0;
                List<Deck> decks = new ArrayList<>();
                for (CardPool cardPool : last3Pools) {
                    String pack = cardPool.getTitle();
                    decks.addAll(deckRepository.findByCardpoolUsingCard(pack, cardTitle));
                    alldeck += deckRepository.countByCardpoolAndSide(pack, side);
                }
                int countcard = decks.size();
                logger.debug(String.format("All decks: %d - deck using card: %d", alldeck, countcard));

                // get top cards
                List<CardCount> counts = new ArrayList<>();
                if (card.getType_code().equals("identity")) {
                    List<Card> cards = new ArrayList<>();
                    for (Deck deck : decks) {
                        for (DeckHasCard deckHasCard : deck.getCards()) {
                            Card cardi = deckHasCard.getCard();
                            if (!cards.contains(cardi)) {
                                cards.add(cardi);
                            }
                        }
                    }
                    for (Card cardi : cards) {
                        int count = 0;
                        for (Deck deck : decks) {
                            for (DeckHasCard deckHasCard : deck.getCards()) {
                                if (deckHasCard.getCard().equals(cardi)) {
                                    count++;
                                    break;
                                }
                            }
                        }
                        counts.add(new CardCount(cardi, count));
                    }
                    counts.sort(comparator);
                    counts = trimCardCount(counts, 8);
                } else {
                    // get top identities
                    List<Card> identities = cardRepository.findIdentitiesBySide(side);
                    for (Card identity : identities) {
                        int count = 0;
                        for (Deck deck : decks) {
                            if (deck.getIdentity().equals(identity)) {
                                count++;
                            }
                        }
                        counts.add(new CardCount(identity, count));
                    }
                    counts.sort(comparator);
                    counts = trimCardCount(counts, 8);
                }

                for (CardCount cardCount : counts) {
                    if (cardCount.getCount() > 0) {
                        Card id = cardCount.getCard();
                        result.addTop(new CardUsage(id.getTitle(), id.getCardPack().getName(), side, id.getFaction_code(), cardCount.getCount(), -1,
                                (float) cardCount.getCount() / alldeck, -1));
                        logger.debug(String.format("%s - %,.3f%%",
                                cardCount.getCard().getTitle(), (float) cardCount.getCount() / decks.size()));
                    }
                }

                // get possible combos
                List<Card> cards = new ArrayList<>();
                for (Deck deck : decks) {
                    for (DeckHasCard deckHasCard : deck.getCards()) {
                        Card icard = deckHasCard.getCard();
                        if (!cards.contains(icard)) {
                            cards.add(icard);
                        }
                    }
                }
                cards.remove(card);
                counts.clear();

                for (Card icard : cards) {
                    int icount = 0;
//                    int countoneof = 0;
                    int countboth = 0;
                    for (CardPool cardPool : validPools) {
                        if (icard.getCardPack().getCyclenumber()*100 + icard.getCardPack().getNumber() <=
                                cardPool.getCyclenumber()*100 + cardPool.getDpnumber()) {
                            icount += deckRepository.countByCardpoolUsingCard(cardPool.getTitle(), icard.getTitle());
//                            countoneof += deckRepository.countByCardpoolUsingCardOneOf(cardPool.getTitle(), cardTitle, icard.getTitle());
                            countboth += deckRepository.countByCardpoolUsingCardBoth(cardPool.getTitle(), cardTitle, icard.getTitle());
                        }
                    }
//                    counts.add(new CardCount(icard, (int) ((float) countboth / countoneof * 100))); //Jaccard similarity
                    counts.add(new CardCount(icard, (int) ((float) countboth / icount * countboth / countcard * 100)));
//                    logger.trace(String.format("***,%s,%d,%d", icard.getTitle(), countboth, countoneof));
                    logger.trace(String.format("***,%s,%d,%d", icard.getTitle(), countboth, icount));
                }
                counts.sort(comparator);
                counts = trimCardCount(counts, 10);
                for (CardCount cardCount : counts) {
                    Card combo = cardCount.getCard();
                    result.addCombos(new CardCombo(combo.getTitle(), combo.getCardPack().getName(), combo.getFaction_code(), cardCount.getCount()));
                    logger.debug(String.format("%s - %d",
                            combo.getTitle(), cardCount.getCount()));
                }

                // generate deck links
                for (CardPool cardPool : validPools) {
                    String dptitle = cardPool.getTitle();
                    DPDecks dpDecks = new DPDecks(dptitle, deckRepository.countByCardpoolUsingCard(dptitle, cardTitle));
                    List<Deck> decksInDP = deckRepository.findBestByCardpoolUsingCard(dptitle, cardTitle);
                    for (Deck deck : decksInDP) {
                        dpDecks.addDeckLink(deckDigest.getDeckLink(deck));
                    }
                    if (dpDecks.getCount() > 10) {
                        dpDecks.addDeckLink("..."); // symbol for more decks
                    }
                    result.addDecks(dpDecks);
                }
                stopwatch.stop();
                logger.info(String.format("Saving stats for card: %s (%.3f sec)", cardTitle ,
                        stopwatch.getTotalTimeSeconds()));
                cardStatRepository.save(result);
            } else {
                logger.error("No such card: " + cardTitle);
            }
        }
        return result;
    }

    /**
     * WARNING: cycle names are not in DB. They are coming from Enums.
     * @return Cycle-Datapack structure
     */
    public List<Cycle> getDPStructure() {
        List<Cycle> result = new ArrayList<>();
        for (int i = 1; i< Enums.CardCycles.values().length; i++) {
            Cycle cycle = new Cycle(i);
            List<String> dps = cardPackRepository.getSortedPackNamesInCycle(i);
            if (dps.size() > 1) {
                cycle.addDatapacks(dps);
            }
            result.add(cycle);
        }
        return result;
    }

    private double NaNFix(double input) {
        if (Double.isNaN(input)) {
            return 0;
        } else {
            return input;
        }
    }

    private List<CardPool> trimCardPool(List<CardPool> list, int size) {
        if (list.size() > size) {
            return new ArrayList<>(list.subList(0, size));
        } else {
            return list;
        }
    }

    private List<CardCount> trimCardCount(List<CardCount> list, int size) {
        if (list.size() > size) {
            return new ArrayList<>(list.subList(0, size));
        } else {
            return list;
        }
    }
}
