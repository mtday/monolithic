package monolithic.shell.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import jline.console.completer.StringsCompleter;

import java.util.Optional;

/**
 * Perform testing on the {@link Option} class.
 */
public class OptionTest {
    @Test
    public void testCompareTo() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false, Optional.empty());
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        assertEquals(1, a.compareTo(null));
        assertEquals(0, a.compareTo(a));
        assertEquals(1, a.compareTo(b));
        assertEquals(-1, b.compareTo(a));
        assertEquals(0, b.compareTo(b));
    }

    @Test
    public void testEquals() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false, Optional.empty());
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        assertNotEquals(a, null);
        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(b, a);
        assertEquals(b, b);
    }

    @Test
    public void testHashCode() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false, Optional.empty());
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        assertEquals(-1326618960, a.hashCode());
        assertEquals(-1548936310, b.hashCode());
    }

    @Test
    public void testToString() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false, Optional.empty());
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        assertEquals("Option[description=desc,shortOption=s,longOption=Optional[t],argName=Optional[a],arguments=1,"
                + "required=true,optionalArg=false]", a.toString());
        assertEquals("Option[description=desc,shortOption=s,longOption=Optional.empty,argName=Optional.empty,"
                + "arguments=0,required=false,optionalArg=true]", b.toString());
    }

    @Test
    public void testAsOption() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false, Optional.empty());
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        final org.apache.commons.cli.Option ao = a.asOption();
        final org.apache.commons.cli.Option bo = b.asOption();

        assertEquals(a.getDescription(), ao.getDescription());
        assertEquals(a.getShortOption(), ao.getOpt());
        assertEquals(a.getLongOption().get(), ao.getLongOpt());
        assertEquals(a.getArgName().get(), ao.getArgName());
        assertEquals(a.getArguments(), ao.getArgs());
        assertEquals(a.isRequired(), ao.isRequired());
        assertEquals(a.hasOptionalArg(), ao.hasOptionalArg());

        assertEquals(b.getDescription(), bo.getDescription());
        assertEquals(b.getShortOption(), bo.getOpt());
        assertNull(bo.getLongOpt());
        assertNull(bo.getArgName());
        assertEquals(b.getArguments(), bo.getArgs());
        assertEquals(b.isRequired(), bo.isRequired());
        assertEquals(b.hasOptionalArg(), bo.hasOptionalArg());
    }

    @Test
    public void testGetCompleter() {
        final Option a = new Option("desc", "s", Optional.of("t"), Optional.of("a"), 1, true, false,
                Optional.of(new StringsCompleter("a")));
        final Option b = new Option("desc", "s", Optional.empty(), Optional.empty(), 0, false, true, Optional.empty());

        assertTrue(a.getCompleter().isPresent());
        assertFalse(b.getCompleter().isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeArguments() {
        new Option("desc", "s", Optional.of("t"), Optional.of("a"), -1, true, false, Optional.empty());
    }
}
