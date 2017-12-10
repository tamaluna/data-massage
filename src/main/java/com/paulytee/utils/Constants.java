package com.paulytee.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mike Liu
 * @version $Id$
 */
public class Constants {

    public static Map<String, String> teamAbbreviations = Collections.unmodifiableMap(Stream.of(
                    new SimpleEntry<>("Boston", "BOS"),
                    new SimpleEntry<>("Brooklyn", "BKN"),
                    new SimpleEntry<>("New York", "NY"),
                    new SimpleEntry<>("Philadelphia", "PHI"),
                    new SimpleEntry<>("Toronto", "TOR"),
                    new SimpleEntry<>("Golden State", "GS"),
                    new SimpleEntry<>("LA", "LAC"),
                    new SimpleEntry<>("L.A. Clippers", "LAC"),
                    new SimpleEntry<>("Los Angeles", "LAL"),
                    new SimpleEntry<>("L.A. Lakers", "LAL"),
                    new SimpleEntry<>("Phoenix", "PHX"),
                    new SimpleEntry<>("Sacramento", "SAC"),
                    new SimpleEntry<>("Chicago", "CHI"),
                    new SimpleEntry<>("Cleveland", "CLE"),
                    new SimpleEntry<>("Detroit", "DET"),
                    new SimpleEntry<>("Indiana", "IND"),
                    new SimpleEntry<>("Milwaukee", "MIL"),
                    new SimpleEntry<>("Dallas", "DAL"),
                    new SimpleEntry<>("Houston", "HOU"),
                    new SimpleEntry<>("Memphis", "MEM"),
                    new SimpleEntry<>("New Orleans", "NO"),
                    new SimpleEntry<>("San Antonio", "SA"),
                    new SimpleEntry<>("Atlanta", "ATL"),
                    new SimpleEntry<>("Charlotte", "CHA"),
                    new SimpleEntry<>("Miami", "MIA"),
                    new SimpleEntry<>("Orlando", "ORL"),
                    new SimpleEntry<>("Washington", "WSH"),
                    new SimpleEntry<>("Denver", "DEN"),
                    new SimpleEntry<>("Minnesota", "MIN"),
                    new SimpleEntry<>("Oklahoma City", "OKC"),
                    new SimpleEntry<>("Portland", "POR"),
                    new SimpleEntry<>("Utah", "UTAH"))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    public static Map<String, String> teamAbbreviationsByMascot = Collections.unmodifiableMap(Stream.of(
                    new SimpleEntry<>("Celtics", "BOS"),
                    new SimpleEntry<>("Nets", "BKN"),
                    new SimpleEntry<>("Knicks", "NY"),
                    new SimpleEntry<>("76ers", "PHI"),
                    new SimpleEntry<>("Raptors", "TOR"),
                    new SimpleEntry<>("Warriors", "GS"),
                    new SimpleEntry<>("Clippers", "LAC"),
                    new SimpleEntry<>("Lakers", "LAL"),
                    new SimpleEntry<>("Suns", "PHX"),
                    new SimpleEntry<>("Kings", "SAC"),
                    new SimpleEntry<>("Bulls", "CHI"),
                    new SimpleEntry<>("Cavaliers", "CLE"),
                    new SimpleEntry<>("Pistons", "DET"),
                    new SimpleEntry<>("Pacers", "IND"),
                    new SimpleEntry<>("Bucks", "MIL"),
                    new SimpleEntry<>("Mavericks", "DAL"),
                    new SimpleEntry<>("Rockets", "HOU"),
                    new SimpleEntry<>("Grizzlies", "MEM"),
                    new SimpleEntry<>("Pelicans", "NO"),
                    new SimpleEntry<>("Spurs", "SA"),
                    new SimpleEntry<>("Hawks", "ATL"),
                    new SimpleEntry<>("Hornets", "CHA"),
                    new SimpleEntry<>("Heat", "MIA"),
                    new SimpleEntry<>("Magic", "ORL"),
                    new SimpleEntry<>("Wizards", "WSH"),
                    new SimpleEntry<>("Nuggets", "DEN"),
                    new SimpleEntry<>("Timberwolves", "MIN"),
                    new SimpleEntry<>("Thunder", "OKC"),
                    new SimpleEntry<>("Trail Blazers", "POR"),
                    new SimpleEntry<>("Jazz", "UTAH"))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

}
