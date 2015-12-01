package com.madarasz.netrunnerstats.springMVC.controllers;

import com.madarasz.netrunnerstats.Operations;
import com.madarasz.netrunnerstats.brokers.AcooBroker;
import com.madarasz.netrunnerstats.brokers.NetrunnerDBBroker;
import com.madarasz.netrunnerstats.database.DOs.*;
import com.madarasz.netrunnerstats.database.DOs.relationships.DeckHasCard;
import com.madarasz.netrunnerstats.database.DOs.stats.*;
import com.madarasz.netrunnerstats.database.DOs.stats.entries.*;
import com.madarasz.netrunnerstats.database.DRs.DeckRepository;
import com.madarasz.netrunnerstats.database.DRs.StandingRepository;
import com.madarasz.netrunnerstats.database.DRs.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by madarasz on 11/27/15.
 * Controller for the admin page
 */
@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    Neo4jOperations template;

    @Autowired
    TournamentRepository tournamentRepository;

    @Autowired
    DeckRepository deckRepository;

    @Autowired
    StandingRepository standingRepository;

    @Autowired
    Operations operations;

    @Autowired
    AcooBroker acooBroker;

    @Autowired
    NetrunnerDBBroker netrunnerDBBroker;

    private final DateFormat df = new SimpleDateFormat("yyyy.MM.dd.");

    /**
     * Handles date conversion. Null date returns empty string.
     * @param date date
     * @return formatted date
     */
    private String safeDateFormat(Date date) {
        if (date == null) {
            return "";
        } else {
            return df.format(date);
        }
    }

    // html output

    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin", method = RequestMethod.GET)
    public String getAdminPage(Map<String, Object> model) {
        // Stimhack count
        model.put("countStimhackTournaments", tournamentRepository.countStimhackTournaments());
        model.put("lastStimhackTournament", safeDateFormat(tournamentRepository.getLastStimhackTournamentDate()));
        model.put("countStimhackDecks", deckRepository.countStimhackDecks());
        model.put("countStimhackStandings", standingRepository.countStimhackStandings());
        // Acoo count
        model.put("countAcooTournaments", tournamentRepository.countAcooTournaments());
        model.put("lastAcooTournament", safeDateFormat(tournamentRepository.getLastAcooTournamentDate()));
        model.put("countAcooDecks", deckRepository.countAcooDecks());
        model.put("countAcooStandings", standingRepository.countAcooStandings());
        // NetrunnerDB count
        model.put("countCards", template.count(Card.class));
        model.put("countCardPacks", template.count(CardPack.class));
        model.put("countNetrunnerDBDecks", deckRepository.countNetrunnerDBDecks());
        // DB count
        model.put("countTournaments", template.count(Tournament.class));
        model.put("countDecks", template.count(Deck.class));
        model.put("countStandings", template.count(Standing.class));
//        model.put("countDeckHasCards", template.count(DeckHasCard.class));
        // stat coung
        model.put("countCardPoolStats", template.count(CardPoolStats.class));
        model.put("countCardPool", template.count(CardPool.class));
        model.put("countDPStatistics", template.count(DPStatistics.class));
        model.put("countCountDeckStands", template.count(CountDeckStands.class));
        model.put("countIdentityMDS", template.count(IdentityMDS.class));
        model.put("countMDSEntry", template.count(MDSEntry.class));
        model.put("countDeckInfos", template.count(DeckInfos.class));
        model.put("countDeckInfo", template.count(DeckInfo.class));
        model.put("countDPIdentities", template.count(DPIdentities.class));
        model.put("countDPIdentity", template.count(DPIdentity.class));
        model.put("countCardUsageStat", template.count(CardUsageStat.class));
        model.put("countCardUsage", template.count(CardUsage.class));
        model.put("countIdentityAverage", template.count(IdentityAverage.class));
        model.put("countCardAverage", template.count(CardAverage.class));

        return "Admin";
    }

    // submissions

    // verify data
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Verify", method = RequestMethod.POST)
    public String verify(Map<String, Object> model) {
        operations.checkDataValidity();
        return "redirect:/muchadmin";
    }

    // add stimhack tournament
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Stimhack/AddTournament", method = RequestMethod.POST)
    public String addStimhackTournament(String url, final RedirectAttributes redirectAttributes) {
        Tournament exists = tournamentRepository.findByUrl(url);
        if (exists == null) {
            try {
                operations.loadStimhackTournament(url);
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournament is added to DB: %s", url));
            } catch (Exception ex) {
                logger.error("logged exception", ex);
                redirectAttributes.addFlashAttribute("errorMessage",
                        String.format("Error loading tournament with url: %s", url));
            }
        } else {
            redirectAttributes.addFlashAttribute("warningMessage",
                    String.format("Tournament is already in DB: %s", url));
        }
        return "redirect:/muchadmin";
    }

    // add multiple stimhack tournament by datapack
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Stimhack/AddDataPack", method = RequestMethod.POST)
    public String addStimhackTournamentsByDP(String datapack, final RedirectAttributes redirectAttributes) {
        try {
            long count = template.count(Tournament.class);
            operations.loadStimhackPackTournaments(datapack);
            count = template.count(Tournament.class) - count;
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("%d new tournaments are added to DB for datapack: %s", count, datapack));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        String.format("No new tournaments are added to DB for datapack: %s", datapack));
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    String.format("Error loading Stimhack tournaments with datapack: %s", datapack));
        }
        return "redirect:/muchadmin";
    }

    // add acoo deck
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Acoo/AddDeck", method = RequestMethod.POST)
    public String addAcooDeck(String deckid, final RedirectAttributes redirectAttributes) {
        try {
            int id = Integer.parseInt(deckid);
            Deck exists = deckRepository.findByUrl(acooBroker.deckUrlFromId(id));
            if (exists == null) {
                operations.loadAcooDeck(id);
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournament is added to DB with ID: %d", id));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        String.format("Tournament is already in DB with Acoo ID: %d", id));
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    String.format("Error loading tournament with ID: %s", deckid));
            return "redirect:/muchadmin";
        }
        return "redirect:/muchadmin";
    }

    // add acoo tournament
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Acoo/AddTournament", method = RequestMethod.POST)
    public String addAcooTournament(String tournamentid, final RedirectAttributes redirectAttributes) {
        try {
            int id = Integer.parseInt(tournamentid);
            long tournamentcount = template.count(Tournament.class);
            long deckcount = template.count(Deck.class);
            long standingcount = template.count(Standing.class);
            operations.loadAcooTournamentDecks(id);
            tournamentcount = template.count(Tournament.class) - tournamentcount;
            deckcount = template.count(Deck.class) - deckcount;
            standingcount = template.count(Standing.class) - standingcount;
            if (tournamentcount + deckcount + standingcount > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournament is added to DB with ID: %d - new decks: %d, new standings: %d", id, deckcount, standingcount));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        String.format("No new data was added with Acoo tournament ID: %d", id));
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    String.format("Error loading tournament with ID: %s", tournamentid));
            return "redirect:/muchadmin";
        }
        return "redirect:/muchadmin";
    }

    // add acoo tournament from page
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Acoo/AddTournaments", method = RequestMethod.POST)
    public String addAcooTournaments(String url, boolean paging, final RedirectAttributes redirectAttributes) {
        try {
            long tournamentcount = template.count(Tournament.class);
            long deckcount = template.count(Deck.class);
            long standingcount = template.count(Standing.class);
            operations.loadAcooTournamentsFromUrl(url, paging, false);
            tournamentcount = template.count(Tournament.class) - tournamentcount;
            deckcount = template.count(Deck.class) - deckcount;
            standingcount = template.count(Standing.class) - standingcount;
            if (tournamentcount + deckcount + standingcount > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournaments added to DB from URL: %s - new tournaments: %d, new decks: %d, new standings: %d",
                                url, tournamentcount, deckcount, standingcount));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        String.format("No new data was added with Acoo URL: %s", url));
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    String.format("Error loading tournament with URL: %s", url));
            return "redirect:/muchadmin";
        }
        return "redirect:/muchadmin";
    }

    // load Netrunner DB cards and cardpacks
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/NetrunnerDB/LoadDB", method = RequestMethod.POST)
    public String loadNetrunnerDB(final RedirectAttributes redirectAttributes) {
        try {
            long cardcount = template.count(Card.class);
            long cardpackcount = template.count(CardPack.class);
            operations.loadNetrunnerDB();
            cardcount = template.count(Card.class) - cardcount;
            cardpackcount = template.count(CardPack.class) - cardpackcount;
            if (cardcount + cardpackcount > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("NetrunnerDB refreshed - new cards: %d, new cardpacks: %d",
                                cardcount, cardpackcount));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "No new data was added from NetrunnerDB.");
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading NetrunnerDB");
            return "redirect:/muchadmin";
        }
        return "redirect:/muchadmin";
    }

    // add NetrunnerDB deck
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/NetrunnerDB/LoadDeck", method = RequestMethod.POST)
    public String addNetrunnerDBDeck(String deckid, final RedirectAttributes redirectAttributes) {
        try {
            int id = Integer.parseInt(deckid);
            Deck exists = deckRepository.findByUrl(netrunnerDBBroker.deckUrlFromId(id));
            if (exists == null) {
                operations.loadNetrunnerDbDeck(id);
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournament is added to DB with NetrunnerDB ID: %d", id));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        String.format("Tournament is already in DB with NetrunnerDB ID: %d", id));
            }
        } catch (Exception ex) {
            logger.error("logged exception", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    String.format("Error loading tournament with NetrunnerDB ID: %s", deckid));
            return "redirect:/muchadmin";
        }
        return "redirect:/muchadmin";
    }

    // delete deck
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Delete/Deck", method = RequestMethod.POST)
    public String deleteDeck(String url, final RedirectAttributes redirectAttributes) {
        Deck exists = deckRepository.findByUrl(url);
        if (exists != null) {
            try {
                operations.deleteDeck(url);
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Deck deleted with URL: %s", url));
            } catch (Exception ex) {
                logger.error("logged exception", ex);
                redirectAttributes.addFlashAttribute("errorMessage",
                        String.format("Error deleting deck with url: %s", url));
            }
        } else {
            redirectAttributes.addFlashAttribute("warningMessage",
                    String.format("No deck with URL: %s", url));
        }
        return "redirect:/muchadmin";
    }

    // delete deck
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/Delete/Tournament", method = RequestMethod.POST)
    public String deleteTournament(String url, final RedirectAttributes redirectAttributes) {
        Tournament exists = tournamentRepository.findByUrl(url);
        if (exists != null) {
            try {
                long tournamentcount = template.count(Tournament.class);
                long deckcount = template.count(Deck.class);
                long standingcount = template.count(Standing.class);
                operations.deleteTournament(url);
                tournamentcount -= template.count(Tournament.class);
                deckcount -= template.count(Deck.class);
                standingcount -= template.count(Standing.class);
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Tournament deleted with URL: %s - deleted tournaments: %d, decks: %d, standings: %d",
                                url, tournamentcount, deckcount, standingcount));
            } catch (Exception ex) {
                logger.error("logged exception", ex);
                redirectAttributes.addFlashAttribute("errorMessage",
                        String.format("Error deleting tournament with url: %s", url));
            }
        } else {
            redirectAttributes.addFlashAttribute("warningMessage",
                    String.format("No tournament with URL: %s", url));
        }
        return "redirect:/muchadmin";
    }

    // reset all data
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/PurgeAll", method = RequestMethod.POST)
    public String purgeAll(Map<String, Object> model) {
        operations.cleanDB();
        return "redirect:/muchadmin";
    }

    // reset statistical data
    @PreAuthorize("hasRole(@roles.ADMIN)")
    @RequestMapping(value="/muchadmin/PurgeStat", method = RequestMethod.POST)
    public String purgeStats(Map<String, Object> model) {
        operations.resetStats();
        return "redirect:/muchadmin";
    }
}
