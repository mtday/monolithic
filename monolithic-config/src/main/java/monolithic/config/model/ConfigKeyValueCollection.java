package monolithic.config.model;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import monolithic.common.model.Model;
import monolithic.common.util.CollectionComparator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a collection of dynamic configuration key and value pairs.
 */
public class ConfigKeyValueCollection implements Model, Comparable<ConfigKeyValueCollection> {
    @Nonnull
    private final Map<String, ConfigKeyValue> map = new TreeMap<>();

    /**
     * @param values the configuration key
     */
    public ConfigKeyValueCollection(@Nonnull final Collection<ConfigKeyValue> values) {
        Objects.requireNonNull(values).forEach(kv -> getMap().put(kv.getKey(), kv));
    }

    /**
     * @param values the configuration key
     */
    public ConfigKeyValueCollection(@Nonnull final ConfigKeyValue... values) {
        this(Arrays.asList(Objects.requireNonNull(values)));
    }

    /**
     * @param json a {@link JsonObject} from which a {@link ConfigKeyValueCollection} will be parsed
     */
    public ConfigKeyValueCollection(@Nonnull final JsonObject json) {
        // Validate the json object
        Objects.requireNonNull(json);
        Preconditions.checkArgument(json.has("config"), "Config field required");
        Preconditions.checkArgument(json.get("config").isJsonArray(), "Config field must be an array");

        final JsonArray arr = json.getAsJsonArray("config");
        arr.forEach(element -> {
            Preconditions.checkArgument(element.isJsonObject(), "Config element must be an object");
            final ConfigKeyValue kv = new ConfigKeyValue(element.getAsJsonObject());
            getMap().put(kv.getKey(), kv);
        });
    }

    /**
     * @return the internal map that holds the configuration data
     */
    @Nonnull
    protected Map<String, ConfigKeyValue> getMap() {
        return this.map;
    }

    /**
     * @param key the configuration key for which a configuration value should be retrieved
     * @return the requested configuration value, possibly empty if the specified key was not found
     */
    @Nonnull
    public Optional<ConfigKeyValue> get(@Nonnull final String key) {
        return Optional.ofNullable(getMap().get(Objects.requireNonNull(key)));
    }

    /**
     * @return a {@link Map} containing all of the configuration keys and values
     */
    @Nonnull
    public Map<String, ConfigKeyValue> asMap() {
        return Collections.unmodifiableMap(getMap());
    }

    /**
     * @return a {@link SortedSet} containing all of the configuration keys and values
     */
    @Nonnull
    public SortedSet<ConfigKeyValue> asSet() {
        return new TreeSet<>(getMap().values());
    }

    /**
     * @return the number of configuration key/value pairs in this collection
     */
    public int size() {
        return getMap().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nullable final ConfigKeyValueCollection other) {
        if (other == null) {
            return 1;
        }

        final CompareToBuilder cmp = new CompareToBuilder();
        cmp.append(asSet(), other.asSet(), new CollectionComparator<ConfigKeyValue>());
        return cmp.toComparison();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@CheckForNull final Object other) {
        return (other instanceof ConfigKeyValueCollection) && compareTo((ConfigKeyValueCollection) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(asMap());
        return hash.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String toString() {
        final ToStringBuilder str = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        str.append("configs", asSet());
        return str.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public JsonObject toJson() {
        final JsonArray arr = new JsonArray();
        asSet().stream().map(ConfigKeyValue::toJson).forEach(arr::add);
        final JsonObject json = new JsonObject();
        json.add("config", arr);
        return json;
    }
}
