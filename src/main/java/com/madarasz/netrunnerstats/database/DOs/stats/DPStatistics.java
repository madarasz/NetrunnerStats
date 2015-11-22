package com.madarasz.netrunnerstats.database.DOs.stats;

import com.madarasz.netrunnerstats.database.DOs.stats.entries.CountDeckStands;
import com.madarasz.netrunnerstats.helper.comparator.CountDeckStatsComparator;
import org.springframework.data.neo4j.annotation.*;

import java.util.*;

/**
 * Created by madarasz on 2015.11.08..
 * Output data for Data Pack statistics on factions and indentities
 */
@NodeEntity
public class DPStatistics {
    @GraphId
    private Long id;
    @Indexed private String dpname;
    private int decknum;
    private int statnum;
    @Indexed private boolean top;
    @RelatedTo(type = "RFACTION") private @Fetch Set<CountDeckStands> runnerFactions;
    @RelatedTo(type = "RIDENTITY") private @Fetch Set<CountDeckStands> runnerIdentities;
    @RelatedTo(type = "CFACTION") private @Fetch Set<CountDeckStands> corpFactions;
    @RelatedTo(type = "CIDENTITY") private @Fetch Set<CountDeckStands> corpIdentities;

    public DPStatistics() {
    }

    public DPStatistics(String DPname, int decknum, int statnum, boolean top) {
        this.statnum = statnum;
        this.dpname = DPname;
        this.decknum = decknum;
        this.top = top;
        runnerFactions = new HashSet<CountDeckStands>();
        runnerIdentities = new HashSet<CountDeckStands>();
        corpFactions = new HashSet<CountDeckStands>();
        corpIdentities = new HashSet<CountDeckStands>();
    }

    public void addRunnerFaction(String title, int decks, int standings, String colorcode) {
        runnerFactions.add(new CountDeckStands(title, decks, standings, colorcode));
    }

    public void addCorpFaction(String title, int decks, int standings, String colorcode) {
        corpFactions.add(new CountDeckStands(title, decks, standings, colorcode));
    }

    public void addRunnerIdentity(String title, int decks, int standings, String colorcode) {
        runnerIdentities.add(new CountDeckStands(title, decks, standings, colorcode));
    }

    public void addCorpIdentity(String title, int decks, int standings, String colorcode) {
        corpIdentities.add(new CountDeckStands(title, decks, standings, colorcode));
    }


    public void setDpname(String dpname) {
        this.dpname = dpname;
    }

    public int getDecknum() {
        return decknum;
    }

    public void setDecknum(int decknum) {
        this.decknum = decknum;
    }

    public int getStatnum() {
        return statnum;
    }

    public void setStatnum(int statnum) {
        this.statnum = statnum;
    }

    public String getDpname() {
        return dpname;
    }

    public boolean isTop() {
        return top;
    }

    public Set<CountDeckStands> getRunnerFactions() {
        return runnerFactions;
    }

    public Set<CountDeckStands> getRunnerIdentities() {
        return runnerIdentities;
    }

    public Set<CountDeckStands> getCorpFactions() {
        return corpFactions;
    }

    public Set<CountDeckStands> getCorpIdentities() {
        return corpIdentities;
    }

    public List<CountDeckStands> getSortedRunnerFactions() {
        return sortSet(runnerFactions);
    }

    public List<CountDeckStands> getSortedRunnerIdentities() {
        return sortSet(runnerIdentities);
    }

    public List<CountDeckStands> getSortedCorpFactions() {
        return sortSet(corpFactions);
    }

    public List<CountDeckStands> getSortedCorpIdentities() {
        return sortSet(corpIdentities);
    }

    private List<CountDeckStands> sortSet(Set<CountDeckStands> inputset) {
        CountDeckStatsComparator comparator = new CountDeckStatsComparator();
        List<CountDeckStands> list = new ArrayList<CountDeckStands>(inputset);
        list.sort(comparator);
        return list;
    }

    public int getRunnerStatnum() {
        int result = 0;
        for (CountDeckStands stands : runnerFactions) {
            result += stands.getStandingnum();
        }
        return result;
    }

    public int getCorpStatnum() {
        int result = 0;
        for (CountDeckStands stands : corpFactions) {
            result += stands.getStandingnum();
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s - top: %s - decks: %d, stands: %d", dpname, top, decknum, statnum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DPStatistics stats = (DPStatistics) obj;
        if (dpname == null) return super.equals(obj);
        return (dpname.equals(stats.getDpname())) && (top == stats.isTop());
    }

}
