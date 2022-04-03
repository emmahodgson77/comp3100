import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DSClientTest {
    /*@org.junit.jupiter.api.Test
    void parsedDSSystemXMLgeneratesServerObjects() throws Exception{
        List<Server> actual = DSClient.parseDSSystemXML();
        List<Server> expected = new ArrayList<Server>();
        expected.add(new Server("tiny",10, 60,0.10,1,2000,16000));
        expected.add(new Server("small",10,60,0.20,2,4000,32000));
        expected.add(new Server("medium",10,60,0.40,4,8000,64000));
        expected.add(new Server("large",10,60,0.80,8,16000,128000));
        expected.add(new Server("xlarge",10,60,1.60,16,32000,256000));

        assertTrue(actual.size()>0);
        assertEquals(actual.get(0).type, expected.get(0).type);
        assertEquals(actual.get(1).type, expected.get(1).type);
        assertEquals(actual.get(0).availableCores, expected.get(0).availableCores);
        assertEquals(actual.get(1).availableMemory, expected.get(1).availableMemory);
    }*/
}