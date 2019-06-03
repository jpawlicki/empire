package com.empire.store;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.svc.LoginKey;
import com.empire.svc.Player;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GaeDatastoreClientTest {
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private GaeDatastoreClient client;

    @Before
    public void setUp(){
        helper.setUp();
        client = GaeDatastoreClient.getInstance();
    }

    @After
    public void tearDown(){
        helper.tearDown();
    }

    @Test
    public void playerNormalRoundtripTest(){
        String email = "TEST_EMAIL";
        Player player = new Player(email, "TEST_PASSHASH");

        assertFalse(client.getPlayer(email).isPresent());
        assertTrue(client.putPlayer(player));
        assertEquals(player, client.getPlayer(email).get());
    }

    @Test
    public void nationNormalRoundtripTest(){
        long gameId = 3;
        String nationName = "TEST_NATION";
        Nation nation = new Nation();
        nation.rulerName = "0";
        nation.title = "1";
        nation.food = "2";
        nation.happiness = "3";
        nation.territory = "4";
        nation.glory = "5";
        nation.religion = "6";
        nation.ideology = "7";
        nation.security = "8";
        nation.riches = "9";
        nation.culture = "10";
//        nation.trait1 = NationData.Tag.DEFENSIVE;
//        nation.trait2 = NationData.Tag.DISCIPLINED;
        nation.bonus = "13";
        nation.email = "14";
        nation.password = "15";

        assertFalse(client.getNation(gameId, nationName).isPresent());
        assertTrue(client.putNation(gameId, nationName, nation));
        assertEquals(nation, client.getNation(gameId, nationName).get());
    }

    @Test
    public void ordersNormalRoundtripTest(){
        long gameId = 3;
        String kingdom = "TEST_KINGDOM";
        int turn = 4;
        Map<String, String> ordersMap = new HashMap<>();
        ordersMap.put("Key0", "Order0");
        ordersMap.put("Key1", "Order1");
        ordersMap.put("Key2", "Order2");
        Orders orders = new Orders(gameId, kingdom, turn, ordersMap, 2);

        assertFalse(client.getOrders(gameId, kingdom, turn).isPresent());
        assertTrue(client.putOrders(orders));
        assertEquals(orders, client.getOrders(gameId, kingdom, turn).get());
    }

    @Test
    public void worldNormalRoundtripTest(){
        long gameId = 3;
        int turn = 4;
        World world = new World();
        world.date = turn;
        world.characters = new ArrayList<>();
        world.gmPasswordHash = "TEST_PASSHASH_GM";
        world.obsPasswordHash = "TEST_PASSHASH_OBS";
//        world.harvests = Arrays.asList(1.5, 2.5);
//        world.cultRegions = Arrays.asList(0, 3, 6);
//        world.inspiresHint = 6;
        world.nextTurn = 3;
        world.gameover = false;

        assertFalse(client.getWorld(gameId, turn).isPresent());
        assertTrue(client.putWorld(gameId, world));
        assertEquals(world, client.getWorld(gameId, turn).get());
    }

    @Test
    public void loginNormalRoundtripTest(){
        String email = "TEST_EMAIL";
        long gameId = 3;
        int date = 4;
        LoginKey login = new LoginKey(email, gameId, date);

        assertFalse(client.getLogin(email, gameId, date).isPresent());
        assertTrue(client.putLogin(email, gameId, date));
        assertEquals(login, client.getLogin(email, gameId, date).get());
    }

    @Test
    public void activeGamesNormalRoundtripTest(){
        Set<Long> activeGames = new HashSet<>(Arrays.asList(0L, 1L, 2L, 3L, 4L));

        assertFalse(client.getActiveGames().isPresent());
        assertTrue(client.putActiveGames(activeGames));
        assertEquals(activeGames, client.getActiveGames().get());
    }
}
