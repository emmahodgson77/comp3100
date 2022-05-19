import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DSClientTest {
    @Test
    public void parseServerStateInfo() throws Exception{
        String[] serverInfo = new String[]{"small", "0", "inactive", "-1", "2", "8000", "64000", "0", "0"};
        ServerState actual = DSClient.getServerState(serverInfo);
        ServerState expected = new ServerState("small",0,"inactive","-1",2,8000,64000);
        
        assertTrue(actual!=null);
        assertEquals(actual.type, expected.type);
        assertEquals(actual.state, expected.state);
        assertEquals(actual.core, expected.core);
        assertEquals(actual.serverID, expected.serverID);
        assertEquals(actual.memory, expected.memory);
        assertEquals(actual.disk, expected.disk);
        assertEquals(actual.startTime, expected.startTime);
    }
}