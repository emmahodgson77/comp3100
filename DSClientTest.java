import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class DSClientTest {

    @Test
    public void testSortingAlgs() {
        Server s1 = new Server("tiny", 0, "active", 10, 1, 1000, 1000, 0, 0);
        Server s2 = new Server("tiny", 1, "active", 10, 1, 1000, 1000, 0, 0);
        Server s3 = new Server("small", 0, "active", 10, 2, 1000, 1000, 0, 0);
        Server s4 = new Server("medium", 0, "active", 10, 3, 1000, 1000, 0, 0);

        s1.setWaitTime(3);
        s2.setWaitTime(1);
        s3.setWaitTime(2);
        s4.setWaitTime(1);

        Comparator<Server> cores = Server::compareTo;

        List<Server> servers = new ArrayList<>();
        servers.add(s1);
        servers.add(s3);
        servers.add(s2);
        servers.add(s4);

        Collections.sort(servers, cores);
        assertTrue(servers.get(3).serverID == 0 && servers.get(3).type.equalsIgnoreCase("tiny"));
        assertTrue(servers.get(2).serverID == 1 && servers.get(2).type.equalsIgnoreCase("tiny"));
        assertTrue(servers.get(1).serverID == 0 && servers.get(1).type.equalsIgnoreCase("small"));
        assertTrue(servers.get(0).serverID == 0 && servers.get(0).type.equalsIgnoreCase("medium"));


    }
}
