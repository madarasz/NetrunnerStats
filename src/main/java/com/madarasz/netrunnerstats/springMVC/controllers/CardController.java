package com.madarasz.netrunnerstats.springMVC.controllers;

import com.madarasz.netrunnerstats.Statistics;
import com.madarasz.netrunnerstats.database.DOs.stats.entries.CardUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by madarasz on 11/22/15.
 * Controller class for card information.
 */
@Controller
public class CardController {

    @Autowired
    Statistics statistics;

    // JSON output
    @RequestMapping(value="/JSON/Cards/{sidecode}/{DPName}", method = RequestMethod.GET)
    public @ResponseBody
    List<CardUsage> getAllDeckInfos(
            @PathVariable(value="sidecode") String sidecode,
            @PathVariable(value="DPName") String DPName) {
        return statistics.getMostUsedCardsForCardPack(DPName).getSortedCards(sidecode);
    }
}
