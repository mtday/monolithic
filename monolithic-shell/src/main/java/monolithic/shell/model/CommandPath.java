package monolithic.shell.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import monolithic.common.util.CollectionComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An immutable representation of the fully qualified path to a shell command.
 */
public class CommandPath implements Comparable<CommandPath> {
    @Nonnull
    private final List<String> path;

    /**
     * @param path the fully qualified path (typically of strings or tokens) representing a shell command
     */
    public CommandPath(@Nonnull final List<?> path) {
        this.path = new ArrayList<>();
        final List<String> parts =
                Objects.requireNonNull(path).stream().map(String::valueOf).collect(Collectors.toList());
        for (final String part : parts) {
            final String trimmed = StringUtils.trimToEmpty(part);
            if (StringUtils.isEmpty(trimmed)) {
                continue;
            }
            if (StringUtils.startsWith(trimmed, "-")) {
                // Found the beginning of the options, no more command path parts.
                break;
            }

            this.path.add(part);
        }
    }

    /**
     * @param path the fully qualified path representing a shell command
     */
    public CommandPath(@Nonnull final String... path) {
        this(Arrays.asList(Objects.requireNonNull(path)));
    }

    /**
     * @return the individual path components uniquely identifying a shell command
     */
    @Nonnull
    public List<String> getPath() {
        return Collections.unmodifiableList(this.path);
    }

    /**
     * Check to see if the provided path is a prefix for this path. Since command paths can have multiple parts, this
     * check is used to determine whether the provided path matches the beginning part of this path. Examples:
     * <p>
     * {@code
     * "a b c".isPrefix("a") => true
     * "a b c".isPrefix("a b c") => true
     * "a b c".isPrefix("a b c d") => false
     * "one two three".isPrefix("one tw") => true
     * "one two three".isPrefix("one three") => false
     * }
     *
     * @param other the path to check to see if it is a prefix for this path
     * @return whether the provided path is a prefix of this path
     */
    public boolean isPrefix(@Nonnull final CommandPath other) {
        Objects.requireNonNull(other);

        final Iterator<String> iterA = getPath().iterator();
        final Iterator<String> iterB = other.getPath().iterator();

        while (iterA.hasNext() && iterB.hasNext()) {
            final String a = iterA.next();
            final String b = iterB.next();
            if (!a.equals(b) && !a.startsWith(b)) {
                // Found a mis-match path element
                return false;
            }
        }

        // If iterB.hasNext() == true, then the other path is longer than this path and is a prefix.
        return !iterB.hasNext();
    }

    /**
     * The parent of a command path is generated by removing the last element in the current command path, if present.
     * For example:
     * <p>
     * {@code
     * CommandPath ab = CommandPath.Builder("a b").build();
     * CommandPath a = ab.getParent().get();
     * ab.toString(); // a b
     * a.toString(); // a
     * }
     *
     * @return the parent {@link CommandPath} to this one, possibly empty if this path has only one path element
     */
    @Nonnull
    public Optional<CommandPath> getParent() {
        if (getPath().size() == 1) {
            return Optional.empty();
        }

        final List<String> parentPath = new ArrayList<>(getPath());
        parentPath.remove(parentPath.size() - 1);
        return Optional.of(new CommandPath(parentPath));
    }

    /**
     * The child of a command path is generated by removing the first element in the current command path, if present.
     * For example:
     * <p>
     * {@code
     * CommandPath ab = CommandPath.Builder("a b").build();
     * CommandPath b = ab.getChild().get();
     * ab.toString(); // a b
     * b.toString(); // b
     * b.getChild().isPresent(); // false
     * }
     *
     * @return the parent {@link CommandPath} to this one, possibly empty if this path has only one path element
     */
    @Nonnull
    public Optional<CommandPath> getChild() {
        if (getPath().size() == 1) {
            return Optional.empty();
        }

        final List<String> childPath = new LinkedList<>(getPath());
        childPath.remove(0);
        return Optional.of(new CommandPath(childPath));
    }

    /**
     * @return the size of this command path
     */
    public int getSize() {
        return getPath().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String toString() {
        return String.join(" ", getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nullable final CommandPath other) {
        if (other == null) {
            return 1;
        }

        final CompareToBuilder cmp = new CompareToBuilder();
        cmp.append(getPath(), other.getPath(), new CollectionComparator<>());
        return cmp.toComparison();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@CheckForNull final Object other) {
        return (other instanceof CommandPath) && compareTo((CommandPath) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(getPath());
        return hash.toHashCode();
    }
}
