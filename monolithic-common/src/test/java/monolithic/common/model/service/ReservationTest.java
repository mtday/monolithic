package monolithic.common.model.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

/**
 * Perform testing on the {@link Reservation} class.
 */
public class ReservationTest {
    @Test
    public void testCompareTo() {
        final Reservation a = new Reservation("host", 1234);
        final Reservation b = new Reservation("host", 1235);
        final Reservation c = new Reservation("host2", 1235);

        assertEquals(1, a.compareTo(null));
        assertEquals(0, a.compareTo(a));
        assertEquals(-1, a.compareTo(b));
        assertEquals(-1, a.compareTo(c));
        assertEquals(1, b.compareTo(a));
        assertEquals(0, b.compareTo(b));
        assertEquals(-1, b.compareTo(c));
        assertEquals(1, c.compareTo(a));
        assertEquals(1, c.compareTo(b));
        assertEquals(0, c.compareTo(c));
    }

    @Test
    public void testEquals() {
        final Reservation a = new Reservation("host", 1234);
        final Reservation b = new Reservation("host", 1235);
        final Reservation c = new Reservation("host2", 1235);

        assertNotEquals(a, null);
        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(b, a);
        assertEquals(b, b);
        assertNotEquals(b, c);
        assertNotEquals(c, a);
        assertNotEquals(c, b);
        assertEquals(c, c);
    }

    @Test
    public void testHashCode() {
        final Reservation a = new Reservation("host", 1234);
        final Reservation b = new Reservation("host", 1235);
        final Reservation c = new Reservation("host2", 1235);

        assertEquals(118743299, a.hashCode());
        assertEquals(118743300, b.hashCode());
        assertEquals(-614658386, c.hashCode());
    }

    @Test
    public void testToString() {
        final Reservation res = new Reservation("host", 1234);
        assertEquals("Reservation[host=host,port=1234]", res.toString());
    }

    @Test
    public void testToJson() {
        final Reservation res = new Reservation("host", 1234);
        assertEquals("{\"host\":\"host\",\"port\":1234}", res.toJson().toString());
    }

    @Test
    public void testJsonConstructor() {
        final Reservation original = new Reservation("host", 1234);
        final Reservation copy = new Reservation(original.toJson());
        assertEquals(original, copy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorNoHost() {
        final String jsonStr = "{\"port\":1234}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new Reservation(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorHostWrongType() {
        final String jsonStr = "{\"host\":[],\"port\":1234}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new Reservation(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorNoPort() {
        final String jsonStr = "{\"host\":\"host\"}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new Reservation(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorPortWrongType() {
        final String jsonStr = "{\"host\":\"host\",\"port\":[]}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new Reservation(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorPortNotInt() {
        final String jsonStr = "{\"host\":\"host\",\"port\":\"a\"}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new Reservation(json);
    }
}
