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
    public void basicTest(){
        GaeDatastoreClient client = GaeDatastoreClient.getInstance();
        long gameId = -1;
        int turn = 4;
        int version = 2;
        String kingdom = "Test Kingdom";
        Map<String, String> ordersMap = new HashMap<>();
        ordersMap.put("Key0", "Order0");
        ordersMap.put("Key1", "Order1");
        ordersMap.put("Key2", "Order2");

        Orders ordersRead0 = client.getOrders(gameId, kingdom, turn);

        Orders orders = new Orders(gameId, kingdom, turn, ordersMap, version);
        boolean resultPut = client.putOrders(orders);

        Orders ordersRead = client.getOrders(gameId, kingdom, turn);
        System.out.println("Done");
    }

    @Test
    public void playerPutGetTest(){
        String email = "TEST_EMAIL";
        Player player = new Player(email, "TEST_PASSHASH");

        assertNull(null, client.getPlayer(email));
        assertTrue(client.putPlayer(player));
        assertEquals(player, client.getPlayer(email));
    }
}
