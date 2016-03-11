package monolithic.common.model.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

/**
 * Perform testing on the {@link ServiceInfo} class.
 */
public class ServiceInfoTest {
    @Test
    public void testCompareTo() {
        final ServiceInfo a = new ServiceInfo("name", "1.2.3");
        final ServiceInfo b = new ServiceInfo("name", "1.2.4");
        final ServiceInfo c = new ServiceInfo("name2", "1.2.3");

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
        final ServiceInfo a = new ServiceInfo("name", "1.2.3");
        final ServiceInfo b = new ServiceInfo("name", "1.2.4");
        final ServiceInfo c = new ServiceInfo("name2", "1.2.3");

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
        final ServiceInfo a = new ServiceInfo("name", "1.2.3");
        final ServiceInfo b = new ServiceInfo("name", "1.2.4");
        final ServiceInfo c = new ServiceInfo("name2", "1.2.3");

        assertEquals(171522874, a.hashCode());
        assertEquals(171522875, b.hashCode());
        assertEquals(-378627802, c.hashCode());
    }

    @Test
    public void testToString() {
        final ServiceInfo res = new ServiceInfo("name", "1.2.3");
        assertEquals("ServiceInfo[systemName=name,systemVersion=1.2.3]", res.toString());
    }

    @Test
    public void testToJson() {
        final ServiceInfo res = new ServiceInfo("name", "1.2.3");
        assertEquals("{\"systemName\":\"name\",\"systemVersion\":\"1.2.3\"}", res.toJson().toString());
    }

    @Test
    public void testJsonConstructor() {
        final ServiceInfo original = new ServiceInfo("name", "1.2.3");
        final ServiceInfo copy = new ServiceInfo(original.toJson());
        assertEquals(original, copy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorNoName() {
        final String jsonStr = "{\"systemVersion\":\"1.2.3\"}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new ServiceInfo(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorNameWrongType() {
        final String jsonStr = "{\"systemName\":[],\"systemVersion\":\"1.2.3\"}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new ServiceInfo(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorNoVersion() {
        final String jsonStr = "{\"systemName\":\"systemName\"}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new ServiceInfo(json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testJsonConstructorVersionWrongType() {
        final String jsonStr = "{\"systemName\":\"systemName\",\"systemVersion\":[]}";
        final JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        new ServiceInfo(json);
    }

    @Test
    public void testConverter() {
        final ServiceInfo.ServiceInfoConverter converter = new ServiceInfo.ServiceInfoConverter();
        final ServiceInfo original = new ServiceInfo("system-name", "1.2.3");
        final JsonObject json = converter.doBackward(original);
        final ServiceInfo copy = converter.doForward(json);
        assertEquals(original, copy);
    }

    @Test
    public void testConverterEquals() {
        final ServiceInfo.ServiceInfoConverter a = new ServiceInfo.ServiceInfoConverter();
        final ServiceInfo.ServiceInfoConverter b = new ServiceInfo.ServiceInfoConverter();
        final Object c = 5;

        assertNotEquals(a, null);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(b, a);
        assertEquals(b, b);
        assertNotEquals(b, c);
    }

    @Test
    public void testConverterHashCode() {
        final ServiceInfo.ServiceInfoConverter a = new ServiceInfo.ServiceInfoConverter();
        assertEquals(-484927233, a.hashCode());
    }
}
