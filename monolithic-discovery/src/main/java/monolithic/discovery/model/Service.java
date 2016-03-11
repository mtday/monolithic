package monolithic.discovery.model;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.curator.x.discovery.ServiceInstance;

import monolithic.common.model.Model;

import java.util.Date;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An immutable class representing the registration of a service for automatic discovery.
 */
public class Service implements Model, Comparable<Service> {
    @Nonnull
    private final String systemName;
    @Nonnull
    private final String systemVersion;
    @Nonnull
    private final String host;
    private final int port;
    private final boolean secure;

    /**
     * @param systemName the name of the system in which this service is running
     * @param systemVersion the version of the service
     * @param host the host on which the service is running
     * @param port the port on which the service has bound
     * @param secure whether the service is operating with SSL enabled on the connection
     */
    public Service(@Nonnull final String systemName, @Nonnull final String systemVersion,
            @Nonnull final String host, final int port, final boolean secure) {
        this.systemName = systemName;
        this.systemVersion = systemVersion;
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.secure = secure;
    }

    /**
     * @param service the {@link ServiceInstance} as stored and managed by zookeeper
     */
    public Service(@Nonnull final ServiceInstance<String> service) {
        this(new JsonParser().parse(String.valueOf(Objects.requireNonNull(service).getPayload())).getAsJsonObject());
    }

    /**
     * @param json the JSON representation of a {@link Service} object
     */
    public Service(@Nonnull final JsonObject json) {
        Objects.requireNonNull(json);
        Preconditions.checkArgument(json.has("systemName"), "System name field required");
        Preconditions.checkArgument(json.get("systemName").isJsonPrimitive(), "System name must be a primitive");
        Preconditions.checkArgument(json.has("systemVersion"), "System version field required");
        Preconditions.checkArgument(json.get("systemVersion").isJsonPrimitive(), "System version must be a primitive");
        Preconditions.checkArgument(json.has("host"), "Host field required");
        Preconditions.checkArgument(json.get("host").isJsonPrimitive(), "Host field must be a primitive");
        Preconditions.checkArgument(json.has("port"), "Port field required");
        Preconditions.checkArgument(json.get("port").isJsonPrimitive(), "Port field must be a primitive");
        Preconditions.checkArgument(json.has("secure"), "Secure field required");
        Preconditions.checkArgument(json.get("secure").isJsonPrimitive(), "Secure field must be a primitive");

        this.systemName = json.get("systemName").getAsString();
        this.systemVersion = json.get("systemVersion").getAsString();
        this.host = json.get("host").getAsString();
        this.port = json.get("port").getAsInt();
        this.secure = json.get("secure").getAsBoolean();
    }

    /**
     * @return the unique id used to describe this service
     */
    @Nonnull
    public String getId() {
        return String.format("%s:%d", getHost(), getPort());
    }

    /**
     * @return the name of the system that is running
     */
    @Nonnull
    public String getSystemName() {
        return this.systemName;
    }

    /**
     * @return the version of the service that is running
     */
    @Nonnull
    public String getSystemVersion() {
        return this.systemVersion;
    }

    /**
     * @return the host on which the service is running
     */
    @Nonnull
    public String getHost() {
        return this.host;
    }

    /**
     * @return the port on which the service has bound
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @return whether the service is running with SSL enabled
     */
    public boolean isSecure() {
        return this.secure;
    }

    /**
     * @return a URL representation capable of being used to communicate with the service
     */
    @Nonnull
    public String asUrl() {
        return String.format("%s://%s:%d/", isSecure() ? "https" : "http", getHost(), getPort());
    }

    /**
     * @return the {@link ServiceInstance} object used to represent this service when stored in zookeeper for service
     * discovery
     */
    @Nonnull
    public ServiceInstance<String> asServiceInstance() {
        return new ServiceInstance<>(getSystemName(), getId(), getHost(), getPort(), null, toJson().toString(),
                new Date().getTime(), org.apache.curator.x.discovery.ServiceType.DYNAMIC, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nullable final Service other) {
        if (other == null) {
            return 1;
        }

        final CompareToBuilder cmp = new CompareToBuilder();
        cmp.append(getSystemName(), other.getSystemName());
        cmp.append(getSystemVersion(), other.getSystemVersion());
        cmp.append(getHost(), other.getHost());
        cmp.append(getPort(), other.getPort());
        cmp.append(isSecure(), other.isSecure());
        return cmp.toComparison();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@CheckForNull final Object other) {
        return (other instanceof Service) && compareTo((Service) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(getSystemName());
        hash.append(getSystemVersion());
        hash.append(getHost());
        hash.append(getPort());
        hash.append(isSecure());
        return hash.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String toString() {
        final ToStringBuilder str = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        str.append("systemName", getSystemName());
        str.append("systemVersion", getSystemVersion());
        str.append("host", getHost());
        str.append("port", getPort());
        str.append("secure", isSecure());
        return str.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        json.addProperty("systemName", getSystemName());
        json.addProperty("systemVersion", getSystemVersion());
        json.addProperty("host", getHost());
        json.addProperty("port", getPort());
        json.addProperty("secure", isSecure());
        return json;
    }
}
