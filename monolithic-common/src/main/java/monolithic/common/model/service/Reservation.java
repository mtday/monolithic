package monolithic.common.model.service;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import monolithic.common.model.Model;

import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An immutable object representing a reservation for a host and port. Prevents two different services from
 * attempting to start and listen on the same host and port.
 */
public class Reservation implements Model, Comparable<Reservation> {
    @Nonnull
    private final String host;
    private final int port;

    /**
     * @param host the host on which the service will run
     * @param port the port on which the service will run
     */
    public Reservation(@Nonnull final String host, final int port) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
    }

    /**
     * @param json the {@link JsonObject} from which the reservation should be built
     */
    public Reservation(@Nonnull final JsonObject json) {
        Objects.requireNonNull(json);
        Preconditions.checkArgument(json.has("host"), "Host field required");
        Preconditions.checkArgument(json.get("host").isJsonPrimitive(), "Host field must be a primitive");
        Preconditions.checkArgument(json.has("port"), "Port field required");
        Preconditions.checkArgument(json.get("port").isJsonPrimitive(), "Port field must be a primitive");

        this.host = json.get("host").getAsString();
        this.port = json.get("port").getAsInt();
    }

    /**
     * @return the host on which the service will run
     */
    @Nonnull
    public String getHost() {
        return this.host;
    }

    /**
     * @return the port on which the service will run
     */
    public int getPort() {
        return this.port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nullable final Reservation other) {
        if (other == null) {
            return 1;
        }

        final CompareToBuilder cmp = new CompareToBuilder();
        cmp.append(getHost(), other.getHost());
        cmp.append(getPort(), other.getPort());
        return cmp.toComparison();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@CheckForNull final Object other) {
        return (other instanceof Reservation) && compareTo((Reservation) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(getHost());
        hash.append(getPort());
        return hash.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String toString() {
        final ToStringBuilder str = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        str.append("host", getHost());
        str.append("port", getPort());
        return str.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        json.addProperty("host", getHost());
        json.addProperty("port", getPort());
        return json;
    }
}
