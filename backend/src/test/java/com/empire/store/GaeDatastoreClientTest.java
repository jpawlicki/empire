package com.empire.store;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class GaeDatastoreClientTest {
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private final DatastoreClient client = GaeDatastoreClient.getInstance();

    @Before
    public void setUp(){
        helper.setUp();
    }

    @After
    public void tearDown(){
        helper.tearDown();
    }

    @Test
    public void playerPutGetTest(){
        String email = "TEST_EMAIL";
        Player player = new Player(email, "TEST_PASSHASH");

        assertNull(null, client.getPlayer(email));
        assertTrue(client.putPlayer(player));
        assertEquals(player, client.getPlayer(email));
    }

    @Test
    public void nationPutGetTest(){
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
        nation.trait1 = "11";
        nation.trait2 = "12";
        nation.bonus = "13";
        nation.email = "14";
        nation.password = "15";

        assertNull(null, client.getNation(gameId, nationName));
        assertTrue(client.putNation(gameId, nationName, nation));
        assertEquals(nation, client.getNation(gameId, nationName));
    }

    @Test
    public void ordersPutGetTest(){
        long gameId = 3;
        String kingdom = "TEST_KINGDOM";
        int turn = 4;
        Map<String, String> ordersMap = new HashMap<>();
        ordersMap.put("Key0", "Order0");
        ordersMap.put("Key1", "Order1");
        ordersMap.put("Key2", "Order2");
        Orders orders = new Orders(gameId, kingdom, turn, ordersMap, 2);

        assertNull(null, client.getOrders(gameId, kingdom, turn));
        assertTrue(client.putOrders(orders));
        assertEquals(orders, client.getOrders(gameId, kingdom, turn));
    }
}
