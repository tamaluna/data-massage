package com.paulytee;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.paulytee.utils.CSV;
import com.paulytee.utils.Constants;
import com.paulytee.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * @author Paul Tamalunas
 * @version $Id$
 */
//@Slf4j
public class DataMassageTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String testCaseFile = "/nba-2016.csv";
    private final String unformattedOddsFile = "/matchups-odds-unformatted-";
    private final String unformattedInsiderFile = "/matchups-odds-unformatted-insider-";
    private final int lowYear = 2017;
    private final Map<Date, List<String>> teamsByDate = new HashMap<>();

    /*@Before private void init() {}*/

    private class Game {
        @Getter @Setter int sequence;
        @Getter @Setter String team;
        @Getter @Setter String opponent;
        @Getter @Setter Date date;
        @Getter @Setter boolean home;
        @Getter @Setter int score;
        @Getter @Setter int scoreOpp;
        @Getter @Setter boolean playedYesterday;
        @Getter @Setter boolean playedYesterdayOpp;
        @Getter @Setter boolean win; // needed because game could be a tie

        Game(int seq, String team, String opponent, Date date, boolean home, int score, int scoreOpp, boolean win) {
            this.sequence = seq;
            this.team = team;
            this.opponent = opponent;
            this.date = date;
            this.home = home;
            this.score = score;
            this.scoreOpp = scoreOpp;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(this.date);
            cal.add(Calendar.DATE, -1);
            Date yesterday = cal.getTime();
            this.playedYesterday = teamsByDate.containsKey(yesterday) && teamsByDate.get(yesterday).contains(team);
            this.playedYesterdayOpp = teamsByDate.containsKey(yesterday) && teamsByDate.get(yesterday).contains(opponent);
            this.win = win;
        }

        @Override
        public String toString() {
            return "Game{" +
                    "team='" + team + "\'" +
                    ", opponent='" + opponent + "\'" +
                    ", date=" + date +
                    ", home=" + home +
                    ", score=" + score +
                    ", scoreOpp=" + scoreOpp +
                    ", playedYesterday=" + playedYesterday +
                    ", playedYesterdayOpp=" + playedYesterdayOpp +
                    ", win=" + win +
                    "}";
        }
    }

    private Map<String, List<Game>> teamGames = new HashMap<>();
    private List<Game> gamesInsequence = new ArrayList<>();

    @JsonSerialize
    private class Record {
        @Getter @Setter int win;
        @Getter @Setter int loss;
        @Getter @Setter int gamesHome;
        @Getter @Setter int gamesAway;
        @Getter @Setter int gamesHomeVsWinners;
        @Getter @Setter int gamesAwayVsWinners;
        @Getter @Setter int homeWin;
        @Getter @Setter int homeWinVsWinners;
        @Getter @Setter int awayWinVsWinners;
        @Getter @Setter float winPct;
        @Getter @Setter float winPctVsWinners;
        @Getter @Setter float homeWinPctVsWinners;
        @Getter @Setter float awayWinPctVsWinners;
        @Getter @Setter int pointsForHome;
        @Getter @Setter int pointsForAway;
        @Getter @Setter int pointsAgainstHome;
        @Getter @Setter int pointsAgainstAway;
        @Getter @Setter float avgPointsFor;
        @Getter @Setter float avgPointsAgainst;
        @Getter @Setter float avgPointsForHome;
        @Getter @Setter float avgPointsForAway;
        @Getter @Setter float avgPointsAgainstHome;
        @Getter @Setter float avgPointsAgainstAway;

        @Override
        public String toString() {
            return "Record{" +
                    "win=" + win +
                    ", loss=" + loss +
                    ", gamesHome=" + gamesHome +
                    ", gamesAway=" + gamesAway +
                    ", gamesHomeVsWinners=" + gamesHomeVsWinners +
                    ", gamesAwaVsWinnersy=" + gamesAwayVsWinners +
                    ", homeWin=" + homeWin +
                    ", homeWinVsWinners=" + homeWinVsWinners +
                    ", awayWinVsWinners=" + awayWinVsWinners +
                    ", winPct=" + String.format("%.2f", winPct) +
                    ", winPctVsWinners=" + String.format("%.2f", winPctVsWinners) +
                    ", homeWinPctVsWinners=" + String.format("%.2f", homeWinPctVsWinners) +
                    ", awayWinPctVsWinners=" + String.format("%.2f", awayWinPctVsWinners) +
                    ", pointsForHome=" + pointsForHome +
                    ", pointsForAway=" + pointsForAway +
                    ", pointsAgainstHome=" + pointsAgainstHome +
                    ", pointsAgainstAway=" + pointsAgainstAway +
                    ", avgPointsFor=" + String.format("%.2f", avgPointsFor) +
                    ", avgPointsAgainst=" + String.format("%.2f", avgPointsAgainst) +
                    ", avgPointsForHome=" + String.format("%.2f", avgPointsForHome) +
                    ", avgPointsForAway=" + String.format("%.2f", avgPointsForAway) +
                    ", avgPointsAgainstHome=" + String.format("%.2f", avgPointsAgainstHome) +
                    ", avgPointsAgainstAway=" + String.format("%.2f", avgPointsAgainstAway) +
                    "}";
        }
    }

    private Map<String, Record> teamRecords = new HashMap<>();

    private class Odds {
        @Getter @Setter String awayTeam;
        @Getter @Setter String homeTeam;
        @Getter @Setter float startSpread = -100;
        @Getter @Setter float lowSpread = -100;
        @Getter @Setter float highSpread;
        @Getter @Setter float startOU;
        @Getter @Setter float lowOU;
        @Getter @Setter float highOU;
    }

    @Test
    public void processFile() throws Exception {
        objectMapper.setVisibility(/*JsonMethod.FIELD*/ PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        List<Map<String, String>> data = csvToMap(testCaseFile);
        //System.out.println("\nmap:\n"+data);

        createRecords(data);

        calculatePercentages();

        updateGameInfoWithRecords();

        calculatePercentagesAdvanced();

        for (String team : teamRecords.keySet()) {
            System.out.println(team+" - " + teamRecords.get(team));
        }

        writeCsvFile("C:/temp/teamRecords.csv", teamRecords);

        Map<String, Map<String, Object>> gameData = produceFinalGameData();

        writeCsvFile("C:/temp/gameData.csv", gameData);
    }

    @Test
    public void produceBasicGameData() throws Exception {
        objectMapper.setVisibility(/*JsonMethod.FIELD*/ PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        Map<String, Map<String, Object>> gameData = produceFinalGameData();

        writeCsvFile("C:/temp/gameData.csv", gameData);
    }

    @Test
    public void createMatchupPredictionFile() throws Exception {
        objectMapper.setVisibility(/*JsonMethod.FIELD*/ PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        final String matchupsFileRaw = "/matchups.csv";
        final String matchupsFileOut = "C:/temp/matchups-orig-"+predictionDate+".csv";
        final String matchupsFileMAIN = "C:/temp/matchups-coarse-"+predictionDate+".csv";

        List<Map<String, String>> data = csvToMap("/nba-2017.csv");
        //System.out.println("\nmap:\n"+data);

        createRecords(data);

        calculatePercentages();

        updateGameInfoWithRecords();

        calculatePercentagesAdvanced();

        for (String team : teamRecords.keySet()) {
            System.out.println(team+" - " + teamRecords.get(team));
        }

        //writeCsvFile("C:/temp/teamRecords.csv", teamRecords);

        // Reuse teamGames
        teamGames = new HashMap<>();
        gamesInsequence = new ArrayList<>();
        data = csvToMap(matchupsFileRaw);
        createMatchupRecords(data);

        Map<String, Map<String, Object>> matchupsData = produceFinalGameData(predictionDate);

        writeCsvFile(matchupsFileOut, matchupsData);

        matchupsData = produceFinalGameData(true, predictionDate);

        writeCsvFile(matchupsFileMAIN, matchupsData);
    }

    @Test
    public void createFormattedOddsFile() throws Exception {
        final String oddsFileSuffix = "2017-11-28.csv";
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        List<Map<String, String>> data = csvToMapForOddsFile(unformattedOddsFile + oddsFileSuffix);
        Map<String, List> map = new HashMap<>();
        map.put("data", data);

        writeCsvFile("C:/temp/odds-"+oddsFileSuffix, map);
    }

    final String predictionDate = "2017-12-12";
    final String dateSuffix =     "2017-12-12.csv";

    @Test
    public void createFormattedInsiderOddsFile() throws Exception {
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        List<Map<String, String>> data = csvToMapForInsiderFile(unformattedInsiderFile + dateSuffix);
        Map<String, List> map = new HashMap<>();
        map.put("data", data);

        writeCsvFile("C:/temp/odds-insider-"+ dateSuffix, map);
    }

    @Test
    public void processAwsPrediction() throws Exception {
        List<String> predictions = csvToPredictionArray("C:/Temp/Predictions/Predictions-"+dateSuffix);
        List<String> teamList = csvToTeamList("C:/Temp/matchups-orig-"+dateSuffix);
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < predictions.size()/2; i++) {
            List<String> list = new ArrayList<>();
            out.add(list);
            list.add(predictions.get(2*i));
            list.add(predictions.get(2*i+1));
            list.add(teamList.get(2*i));
            list.add(teamList.get(2*i+1));
        }
        writeCsvFile("C:/Temp/Predictions/Predictions-processed-"+dateSuffix, out);
    }

    private void createRecords(List<Map<String, String>> data) {
        int sequence = 0;
        for (Map<String, String> game : data) {
            String[] score = game.get("Score").replaceAll("\"","").split(", ");
            System.out.println("socre: "+score[0]+", "+score[1]);
            String home = Constants.teamAbbreviations.get(game.get("Home"));
            String away = Constants.teamAbbreviations.get(game.get("Away"));
            int homeIndex = score[0].split(" ")[0].equals(home) ? 0 : 1, awayIndex = 1 - homeIndex,
                homeScore = Integer.parseInt(score[homeIndex].split(" ")[1]),
                awayScore = Integer.parseInt(score[awayIndex].split(" ")[1]);
            boolean otWinHome = false, otWinAway = false;
            if (homeScore == awayScore) {
                String[] otScore = game.get("ScoreOT").replaceAll("\"","").split(", ");
                int homeScoreOT = Integer.parseInt(otScore[homeIndex].split(" ")[1]),
                    awayScoreOT = Integer.parseInt(otScore[awayIndex].split(" ")[1]);
                if (homeScoreOT > awayScoreOT) { otWinHome = true; }
                else                           { otWinAway = true; }
            }
            String[] dateFields = game.get("Date").split("-");
            Date date = new Calendar.Builder().setDate(
                    Integer.parseInt(dateFields[0]), Integer.parseInt(dateFields[1])-1, Integer.parseInt(dateFields[2]))
                    .build().getTime();
            List<String> playedToday = teamsByDate.computeIfAbsent(date, k -> new ArrayList<>());
            playedToday.add(home);
            playedToday.add(away);

            addHomeRecord(home, away, homeScore, awayScore, date, otWinHome);
            boolean win = homeScore > awayScore || otWinHome;
            addGameToTeamGames(home, away, date, true, homeScore, awayScore, sequence, win);

            addAwayRecord(away, home, awayScore, homeScore, date, otWinAway);
            win = awayScore > homeScore || otWinAway;
            addGameToTeamGames(away, home, date, false, awayScore, homeScore, sequence, win);

            sequence++;
        }
    }

    private void createMatchupRecords(List<Map<String, String>> data) {
        for (Map<String, String> game : data) {
            int sequence = Integer.parseInt(game.get("Sequence"));
            String home = Constants.teamAbbreviationsByMascot.keySet().contains(game.get("Home")) ?
                    Constants.teamAbbreviationsByMascot.get(game.get("Home")) : game.get("Home");
            String away = Constants.teamAbbreviationsByMascot.keySet().contains(game.get("Away")) ?
                    Constants.teamAbbreviationsByMascot.get(game.get("Away")) : game.get("Away");
            String[] dateFields = game.get("Date").split("-");
            Date date = new Calendar.Builder().setDate(
                    Integer.parseInt(dateFields[0]), Integer.parseInt(dateFields[1])-1, Integer.parseInt(dateFields[2]))
                    .build().getTime();
            List<String> playedToday = teamsByDate.computeIfAbsent(date, k -> new ArrayList<>());
            playedToday.add(home);
            playedToday.add(away);

            addGameToTeamGames(away, home, date, false, 2*sequence, 0, sequence, false);

            addGameToTeamGames(home, away, date, true, 2*sequence+1, 0, sequence, false);
        }
    }

    private void addHomeRecord(String home, String away, int homeScore, int awayScore, Date date, boolean otWinner) {
        Record recHome;
        if (!teamRecords.containsKey(home)) { teamRecords.put(home, new Record()); }
        recHome = teamRecords.get(home);
        recHome.gamesHome = ++recHome.gamesHome;
        if (homeScore == awayScore) {
            if (otWinner) {
                recHome.win = ++recHome.win;
            } else {
                recHome.loss = ++recHome.loss;
            }
        } else if (homeScore > awayScore) {
            recHome.win = ++recHome.win;
        } else {
            recHome.loss = ++recHome.loss;
        }
        recHome.pointsForHome = recHome.pointsForHome + homeScore;
        recHome.pointsAgainstHome = recHome.pointsAgainstHome + awayScore;
        recHome.avgPointsForHome = (float) recHome.pointsForHome / recHome.gamesHome;
        recHome.avgPointsAgainstHome = (float) recHome.pointsAgainstHome / recHome.gamesHome;
    }

    private void addAwayRecord(String away, String home, int awayScore, int homeScore, Date date, boolean otWinner) {
        Record recAway;
        if (!teamRecords.containsKey(away)) { teamRecords.put(away, new Record()); }
        recAway = teamRecords.get(away);
        recAway.gamesAway = ++recAway.gamesAway;
        if (homeScore == awayScore) {
            if (otWinner) {
                recAway.win = ++recAway.win;
            } else {
                recAway.loss = ++recAway.loss;
            }
        } else if (awayScore > homeScore) {
            recAway.win = ++recAway.win;
        } else {
            recAway.loss = ++recAway.loss;
        }
        recAway.pointsForAway = recAway.pointsForAway + awayScore;
        recAway.pointsAgainstAway = recAway.pointsAgainstAway + homeScore;
        recAway.avgPointsForAway = (float) recAway.pointsForAway / recAway.gamesAway;
        recAway.avgPointsAgainstAway = (float) recAway.pointsAgainstAway / recAway.gamesAway;
    }

    private void addGameToTeamGames(String team, String opponent, Date date, boolean home, int teamScore, int oppScore,
                                    int sequence, boolean win) {
        List<Game> games = teamGames.computeIfAbsent(team, k -> new ArrayList<>());
        Game game = new Game(sequence, team, opponent, date, home, teamScore, oppScore, win);
        games.add(game);
        gamesInsequence.add(game);
    }

    private void calculatePercentages() {
        for (String team : teamRecords.keySet()) {
            Record rec = teamRecords.get(team);
            rec.winPct = (float) rec.win / (rec.gamesAway + rec.gamesHome);
        }
    }

    private void updateGameInfoWithRecords() {
        for (String team : teamGames.keySet()) {
            Record rec = teamRecords.get(team);
            rec.avgPointsFor = (float) (rec.pointsForHome + rec.pointsForAway) / (rec.gamesHome + rec.gamesAway);
            rec.avgPointsAgainst = (float) (rec.pointsAgainstHome + rec.pointsAgainstAway) / (rec.gamesHome + rec.gamesAway);
            for (Game game : teamGames.get(team)) {
                // Count games vs. winners
                if (teamRecords.get(game.opponent).winPct > 0.5) {              // check for opponent = winner
                    if (game.home) {
                        rec.gamesHomeVsWinners = ++rec.gamesHomeVsWinners;
                    } else {
                        rec.gamesAwayVsWinners = ++rec.gamesAwayVsWinners;
                    }
                }
                // Count wins
                if (!game.win) { continue; }                   // Skip if loss
                if (game.home) {
                    rec.homeWin = ++rec.homeWin;
                }
                if (teamRecords.get(game.opponent).winPct <= 0.5) { continue; } // Skip if opponent is a loser
                if (game.home) {
                    rec.homeWinVsWinners = ++rec.homeWinVsWinners;
                } else {
                    rec.awayWinVsWinners = ++rec.awayWinVsWinners;
                }
            }
        }
    }

    private void calculatePercentagesAdvanced() {
        for (String team : teamRecords.keySet()) {
            System.out.println(team);
            Record rec = teamRecords.get(team);
            rec.winPctVsWinners = (float) (rec.homeWinVsWinners + rec.awayWinVsWinners) / (rec.gamesHomeVsWinners + rec.gamesAwayVsWinners);
            rec.homeWinPctVsWinners = (float) rec.homeWinVsWinners / rec.gamesHomeVsWinners;
            rec.awayWinPctVsWinners = (float) rec.awayWinVsWinners / rec.gamesAwayVsWinners;
            System.out.println(rec);
        }
    }

    private Map<String, Map<String, Object>> produceFinalGameData() throws IOException {
        return produceFinalGameData(false, null);
    }

    private Map<String, Map<String, Object>> produceFinalGameData(String thisDateOnly) throws IOException {
        return produceFinalGameData(false, thisDateOnly);
    }

    private Map<String, Map<String, Object>> produceFinalGameData(boolean noNames, String thisDateOnly) throws IOException {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        int i = 0;
        for (Game game : gamesInsequence) {
            if (thisDateOnly != null && !thisDateOnly.equals(dateToString(game.getDate()))) {
                continue;
            }
            JsonNode json = JsonUtils.objectAsJsonNode(game);
            Map<String, Object> resultMap = objectMapper.readValue(json.toString(), Map.class);
            if (noNames) {
                resultMap.remove("team");
                resultMap.remove("opponent");
                resultMap.remove("date");
                resultMap.remove("sequence");
                resultMap.remove("scoreOpp");
            }
            //resultMap.remove("date");
            // Replace TRUE with 1, FALSE with 0
            for (String s : resultMap.keySet()) {
                if (resultMap.get(s) instanceof Boolean) {
                    resultMap.put(s, (Boolean)resultMap.get(s) ? 1 : 0);
                } else if (resultMap.get(s) instanceof Long) {
                    resultMap.put(s, dateToString(new Date((Long) resultMap.get(s))));
                }
            }
            // Add team stats
            Record rec = teamRecords.get(game.getTeam());
            //System.out.println(game.getTeam());
            resultMap.put("winPctTeam", rec.winPct);
            resultMap.put("winPctVsWinnersTeam", rec.winPctVsWinners);
            //resultMap.put("venueWinPctVsWinnersTeam", game.home ? rec.homeWinPctVsWinners : rec.awayWinPctVsWinners);
            resultMap.put("avgPointsForTeam", rec.avgPointsFor);
            resultMap.put("avgPointsForVenueTeam", game.home ? rec.avgPointsForHome : rec.avgPointsForAway);
            //resultMap.put("avgPointsAgainstVenueTeam", game.home ? rec.avgPointsAgainstHome : rec.avgPointsAgainstAway);
            rec = teamRecords.get(game.getOpponent());
            resultMap.put("winPctOpp", rec.winPct);
            resultMap.put("winPctVsWinnersOpp", rec.winPctVsWinners);
            //resultMap.put("venueWinPctVsWinnersOpp", game.home ? rec.awayWinPctVsWinners : rec.homeWinPctVsWinners);
            resultMap.put("avgPointsAgainstOpp",rec.avgPointsAgainst);
            //resultMap.put("avgPointsForVenueOpp", game.home ? rec.avgPointsForAway : rec.avgPointsForHome);
            resultMap.put("avgPointsAgainstVenueOpp", game.home ? rec.avgPointsAgainstAway : rec.avgPointsAgainstHome);
            // Add opponent stats
            out.put(""+i, resultMap);
            i++;
        }
        return out;
    }

    private Map<String, Map<String, Object>> produceMatchupsData(List<Map<String, String>> data) throws IOException {
        Map<String, Map<String, Object>> out = new HashMap<>();
        for (Map<String, String> matchup : data) {
            String away = Constants.teamAbbreviations.get(matchup.get("Away"));
            String home = Constants.teamAbbreviations.get(matchup.get("Home"));
        }
        return out;
    }

    private List<Map<String, String>> csvToMap(String filename) throws IOException {
        try (InputStream in = classpathToStream(filename)) {
            CSV csv = new CSV(true, ',', in );
            List < String > fieldNames = new ArrayList<>();
            if (csv.hasNext()) fieldNames = new ArrayList<>(csv.next());
            List < Map < String, String > > list = new ArrayList<> ();
            String sGameDate = "";
            int sequence = 0;
            while (csv.hasNext()) {
                List < String > x = csv.next();
                if ("".equals(x.get(1)) || "MATCHUP".equals(x.get(0))) {
                    if (x.get(0).contains(",")) {
                        sGameDate = x.get(0);
                        String[] date = sGameDate.contains("Scores") ? sGameDate.split("for ")[1].split(",")[0].split(" ") :
                            sGameDate.split(", ")[1].split(" ");
                        int month = "October".equals(date[0]) ? Calendar.OCTOBER :
                                    "November".equals(date[0]) ? Calendar.NOVEMBER :
                                    "December".equals(date[0]) ? Calendar.DECEMBER :
                                    "January".equals(date[0]) ? Calendar.JANUARY :
                                    "February".equals(date[0]) ? Calendar.FEBRUARY :
                                    "March".equals(date[0]) ? Calendar.MARCH :
                                    "April".equals(date[0]) ? Calendar.APRIL :
                                    "May".equals(date[0]) ? Calendar.MAY :
                                    "June".equals(date[0]) ? Calendar.JUNE : Calendar.JULY;
                        month++; // Calendar.month is zero-based
                        int year = month >= Calendar.OCTOBER ? lowYear : lowYear + 1;
                        sGameDate = createDateString(year, month, Integer.parseInt(date[1]));
                    }
                    continue;
                }
                Map < String, String > obj = new LinkedHashMap<>();
                for (int i = 0; i < fieldNames.size(); i++) {
                    if (x.size() > i) {
                        obj.put(fieldNames.get(i), x.get(i));
                    }
                }
                obj.put("Date", sGameDate);
                obj.put("Sequence", ""+sequence);
                list.add(obj);
                System.out.println("\nobj:\n"+obj);
                sequence++;
            }
            //ObjectMapper mapper = new ObjectMapper();
            //mapper.enable(SerializationFeature.INDENT_OUTPUT);
            //mapper.writeValue(System.out, list);
            return list;
        }
    }

    private List<Map<String, String>> csvToMapForOddsFile(String filename) throws IOException {
        try (InputStream in = classpathToStream(filename)) {
            CSV csv = new CSV(true, ',', in );
            List < Map < String, String > > list = new ArrayList<> ();
            boolean foundHome = false;
            Map < String, String > row = null;
            while (csv.hasNext()) {
                String s = csv.next().get(0);
                if (Constants.teamAbbreviationsByMascot.containsKey(s)) {
                    if (row == null) {
                        row = new LinkedHashMap<>();
                        row.put("Away", Constants.teamAbbreviationsByMascot.get(s));
                    } else {
                        if (foundHome) {
                            System.out.println("row: "+row);
                            list.add(row);
                            row = new LinkedHashMap<>();
                            foundHome = false;
                            row.put("Away", Constants.teamAbbreviationsByMascot.get(s));
                        } else {
                            row.put("Home", Constants.teamAbbreviationsByMascot.get(s));
                            foundHome = true;
                        }
                    }
                } else if (s.contains(":")) {
                    String[] ss = s.split(": ");
                    if ("O/U".equals(ss[0])) {
                        row.put("O/U", ss[1]);
                        System.out.println("row: "+row);
                        list.add(row);
                        foundHome = false;
                        row = null;
                    } else if ("Line".equals(ss[0])) {
                        String[] line = ss[1].split(" ");
                        if (row.get("Away").equals(line[0])) {
                            line[1] = line[1].substring(1); // Remove "-" when Away team is favored
                        }
                        row.put("Line", line[1]);
                    }
                }
            }
            if (row != null) {
                System.out.println("row: "+row);
                list.add(row);
            }
            return list;
        }
    }

    private List<Map<String, String>> csvToMapForInsiderFile(String filename) throws IOException {
        try (InputStream in = classpathToStream(filename)) {
            CSV csv = new CSV(true, ',', in );
            List < Map < String, String > > list = new ArrayList<> ();
            boolean away = true;
            Map < String, String > row = new LinkedHashMap<>();
            Odds odds = new Odds();
            String team;
            float ou, line;
            int i = 0;
            while (csv.hasNext()) {
                i++;
                List<String> input = csv.next();
                String s = input.get(0);
                if (s.length() == 0) {
                    continue;
                }
                System.out.println(i+". "+s);
                if (s.contains("/") && !s.contains(" TV:")) {
                    row = new LinkedHashMap<>();
                    odds = new Odds();
                } else if (s.contains("TV:") || s.contains("|") || s.contains("Doubt") || s.contains("?") || s.contains("OUT")) {
                    row.put("Start Spread", ""+odds.getStartSpread());
                    row.put("Low Spread", ""+odds.getLowSpread());
                    row.put("High Spread", ""+odds.getHighSpread());
                    row.put("Start O/U", ""+odds.getStartOU());
                    row.put("Low O/U", ""+odds.getLowOU());
                    row.put("High O/U", ""+odds.getHighOU());
                    String injuries = !s.contains("TV:") ? s : s.contains(" TV:") ? s.substring(0, s.indexOf(" | TV: ")) : "None";
                    row.put("Injuries", injuries);
                    list.add(row);
                } else {
                    team = Constants.teamAbbreviations.get(s.split("[^\\x00-\\x7F]")[1]);
                    if (away) {
                        row.put("Away", team);
                        odds.setAwayTeam(team);
                    } else {
                        row.put("Home", team);
                        odds.setHomeTeam(team);
                    }
                    boolean first = true;
                    for (String field : input) {
                        if (field.contains("XX")) {
                            continue;
                        }
                        if (first) {
                            first = false;
                        } else if (field.length() > 0) {
                            if (field.contains("o") || field.contains("u")) {
                                ou = resolveOverUnderValue(field);
                                if (odds.getStartOU() == 0) {
                                    odds.setStartOU(ou);
                                } else if (odds.getLowOU() == 0) {
                                    odds.setLowOU(ou);
                                    odds.setHighOU(ou);
                                } else if (ou < odds.getLowOU()) {
                                    odds.setLowOU(ou);
                                } else if (ou > odds.getHighOU()) {
                                    odds.setHighOU(ou);
                                }
                            } else if (field.charAt(0) == '-') {
                                line = resolveLine(field);
                                if (away) {
                                    line = -line;
                                }
                                if (odds.getStartSpread() == -100) {
                                    odds.setStartSpread(line);
                                } else if (odds.getLowSpread() == -100) {
                                    odds.setLowSpread(line);
                                    odds.setHighSpread(line);
                                } else if (line < odds.getLowSpread()) {
                                    odds.setLowSpread(line);
                                } else if (line > odds.getHighSpread()) {
                                    odds.setHighSpread(line);
                                }
                            }
                        }
                    }
                    away = !away;
                }
            }
            return list;
        }
    }

    private List<String> csvToPredictionArray(String filename) throws IOException {
        try (InputStream in = new FileInputStream(filename)) {
            System.out.println(filename+": Total file size to read (in bytes) : " + in.available());
            CSV csv = new CSV(true, ',', in );
            List<String> list = new ArrayList<> ();
            boolean first = true;
            while (csv.hasNext()) {
                if (first) {
                    first = false;
                    continue;
                }
                List<String> input = csv.next();
                if (input.size() < 2) {
                    continue;
                }
                String val = input.get(1);
                if (val.contains("E")) {
                    String[] vals = val.split("E");
                    double power = Math.pow(10, Integer.parseInt(vals[1]));
                    double d = Double.parseDouble(vals[0]) * power;
                    val = "" + d;
                }
                double dVal = round(Double.parseDouble(val), 2);
                list.add(""+dVal);
            }
            return list;
        }
    }

    private List<String> csvToTeamList(String filename) throws IOException {
        try (InputStream in = new FileInputStream(filename)) {
            System.out.println(filename+"Total file size to read (in bytes) : " + in.available());
            CSV csv = new CSV(true, ',', in );
            List<String> list = new ArrayList<> ();
            boolean first = true;
            while (csv.hasNext()) {
                if (first) {
                    first = false;
                    continue;
                }
                List<String> input = csv.next();
                String val = input.get(1);
                list.add(val);
            }
            return list;
        }
    }

    private float resolveOverUnderValue(String field) {
        String ou = field.split(field.contains("o") ? "o" : "u")[0];
        boolean plusHalf = field.split("[^\\x00-\\x7F]").length > 1;
        if (plusHalf) {
            ou = field.split("[^\\x00-\\x7F]")[0];
        }
        float out = Float.parseFloat(ou);
        if (plusHalf) {
            out = out + 0.5f;
        }
        return out;
    }

    private float resolveLine(String field) {
        if (field.contains("PK")) {
            return 0;
        }
        String line = field.split("-")[1];
        boolean plusHalf = line.split("[^\\x00-\\x7F]")[0].length() + 2 == line.length();
        line = line.split("[^\\x00-\\x7F]")[0];
        float out = Float.parseFloat(line);
        if (plusHalf) {
            out = out + 0.5f;
        }
        return -out;
    }

    private String dateToString(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar.month is zero-based
        return createDateString(cal.get(Calendar.YEAR), month, cal.get(Calendar.DATE));
    }

    private String createDateString(int year, int month, int day) {
        String sMonth = (month < 10 ? "0" : "") + month;
        String sDay = (day < 10 ? "0" : "") + day;
        return ""+year+"-"+sMonth+"-"+sDay;
    }

    private Date stringToDate(String date) {
        String[] parts = date.split("-");
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
        cal.set(Calendar.MONTH, Integer.parseInt(parts[1]));
        cal.set(Calendar.DATE, Integer.parseInt(parts[2]));
        return cal.getTime();
    }

    private InputStream classpathToStream(String classPath) {
        try {
            return this.getClass().getResourceAsStream(classPath);
        } catch (Exception var3) {
            throw new RuntimeException("Unable to open resource at classPath : " + classPath, var3);
        }
    }

    private CSV absolutePathToCSV(String path) throws IOException {
        try (InputStream in = new FileInputStream(path)) {
            System.out.println("Total file size to read (in bytes) : " + in.available());
            return new CSV(true, ',', in );
        }
    }

    private void writeCsvFile(String filename, Map<String, ?> map) {
        // Get first element for header
        String key1 = map.keySet().iterator().next();
        Object obj = map.get(key1);
        if (obj instanceof List) {
            obj = ((List) obj).get(0);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            // Write header
            writeObjToBufferedWriter(obj, writer, true);

            // Write rows
            for (String key : map.keySet()) {
                obj = map.get(key);
                if (!(obj instanceof List)) { obj = Arrays.asList(obj); }
                for (Object o : (List)obj) {
                    writeObjToBufferedWriter(o, writer);
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCsvFile(String filename, List<List<String>> list) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            // Write rows
            for (List<String> l : list) {
                StringBuilder sb = new StringBuilder();
                for (String s : l) {
                    if (sb.length() > 0) { sb.append(","); }
                    sb.append(s);
                }
                sb.append("\n");
                writer.write(sb.toString());
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeObjToBufferedWriter(Object obj, BufferedWriter writer) throws IOException {
        writeObjToBufferedWriter(obj, writer, false);
    }

    private void writeObjToBufferedWriter(Object obj, BufferedWriter writer, boolean keysOnly) throws IOException {
        JsonNode json = JsonUtils.objectAsJsonNode(obj);
        Iterator<String> it = json.fieldNames();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            if (sb.length() > 0) { sb.append(","); }
            String val = keysOnly ? it.next() : json.get(it.next()).asText();
            sb.append(val);
        }
        sb.append("\n");
        writer.write(sb.toString());
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
