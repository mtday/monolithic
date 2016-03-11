package monolithic.server.client;

import com.google.common.base.Converter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;

import monolithic.common.model.service.ServiceControlStatus;
import monolithic.common.model.service.ServiceException;
import monolithic.common.model.service.ServiceInfo;
import monolithic.common.model.service.ServiceMemory;
import monolithic.crypto.CryptoFactory;
import monolithic.crypto.model.ServiceRequest;
import monolithic.crypto.model.ServiceResponse;
import monolithic.discovery.model.Service;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides remote access over REST to the base service routes.
 */
public class ServerClient {
    @Nonnull
    private final Config config;
    @Nonnull
    private final ExecutorService executor;
    @Nonnull
    private final OkHttpClient httpClient;
    @Nonnull
    private final CryptoFactory cryptoFactory;

    /**
     * @param config        the static system configuration information
     * @param executor      used to execute asynchronous processing of the client
     * @param httpClient    the HTTP client used to perform REST communication
     * @param cryptoFactory the {@link CryptoFactory} used to verify remote service response signatures
     */
    public ServerClient(
            @Nonnull final Config config, @Nonnull final ExecutorService executor,
            @Nonnull final OkHttpClient httpClient, @Nonnull final CryptoFactory cryptoFactory) {
        this.config = Objects.requireNonNull(config);
        this.executor = Objects.requireNonNull(executor);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.cryptoFactory = Objects.requireNonNull(cryptoFactory);
    }

    /**
     * @return the static system configuration information
     */
    @Nonnull
    protected Config getConfig() {
        return this.config;
    }

    /**
     * @return the {@link ExecutorService} used to execute asynchronous processing of the configuration client
     */
    @Nonnull
    protected ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * @return the service discovery manager used to find configuration service end-points
     */
    @Nonnull
    protected OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * @return the {@link CryptoFactory} used to verify remote service response signatures
     */
    @Nonnull
    protected CryptoFactory getCryptoFactory() {
        return this.cryptoFactory;
    }

    /**
     * Retrieve a {@link JsonObject} by calling the specified {@link Service} at the specified url path
     *
     * @param service   the {@link Service} to call with a REST request
     * @param url       the URL path to invoke with a {@code GET} request on the service
     * @param converter the {@link Converter} object used to transform the {@link JsonObject} into the return object
     * @return the {@link JsonObject} returned from the service
     * @throws IOException if there is a problem with I/O when fetching the data
     * @throws ServiceException if there was a problem on the remote service
     */
    @Nonnull
    protected <T> T get(
            @Nonnull final Service service, @Nonnull final String url,
            @Nonnull final Converter<JsonObject, T> converter) throws IOException, ServiceException {
        final ServiceRequest serviceRequest = new ServiceRequest();
        final Request request = new Builder().url(service.asUrl() + url)
                .header(ServiceRequest.SERVICE_REQUEST_HEADER, serviceRequest.toJson().toString()).get().build();
        final Response response = getHttpClient().newCall(request).execute();
        final String responseBody = response.body().string();
        switch (response.code()) {
            case HttpServletResponse.SC_OK:
                ServiceResponse.verify(getConfig(), getCryptoFactory(), serviceRequest, response);
                final T ret = converter.convert(new JsonParser().parse(responseBody).getAsJsonObject());
                if (ret != null) {
                    return ret;
                }
                // The Converter.convert method is defined as returning @Nullable, which is true if a null value is
                // passed in. But we will never pass in a null value so we don't expect to get here.
                throw new ServiceException("Unexpected null conversion");
            default:
                throw new ServiceException(responseBody);
        }
    }

    /**
     * Retrieve a {@link Map} containing the provided {@link Service} objects mapped to the {@link JsonObject}
     * response by calling the specified {@link Service} at the specified url path.
     *
     * @param services  the {@link Service} objects to call with a REST request
     * @param url       the URL path to invoke with a {@code GET} request on the service
     * @param converter the {@link Converter} object used to transform the {@link JsonObject} into the return object
     * @return the {@link JsonObject} returned from the service
     */
    @Nonnull
    protected <T> Future<Map<Service, T>> getMap(
            @Nonnull final Collection<Service> services, @Nonnull final String url,
            @Nonnull final Converter<JsonObject, T> converter) {
        return getExecutor().submit(() -> {
            final Map<Service, Future<T>> futureMap = new TreeMap<>();
            services.forEach(
                    service -> futureMap.put(service, getExecutor().submit(() -> get(service, url, converter))));

            final Map<Service, T> map = new TreeMap<>();
            for (final Entry<Service, Future<T>> entry : futureMap.entrySet()) {
                try {
                    map.put(entry.getKey(), entry.getValue().get(10, TimeUnit.SECONDS));
                } catch (final InterruptedException | TimeoutException | ExecutionException failed) {
                    final String service =
                            String.format("%s:%d (%s)", entry.getKey().getHost(), entry.getKey().getPort());
                    throw new ServiceException("Failed to retrieve response data for " + service, failed);
                }
            }
            return map;
        });
    }

    /**
     * @param service the {@link Service} for which a {@link ServiceInfo} will be retrieved
     * @return the {@link ServiceInfo} returned from the service wrapped in a {@link Future}
     */
    @Nonnull
    public Future<ServiceInfo> getInfo(@Nonnull final Service service) {
        Objects.requireNonNull(service);
        return getExecutor().submit(() -> get(service, "service/info", new ServiceInfo.ServiceInfoConverter()));
    }

    /**
     * @param services the {@link Collection} of {@link Service} objects for which a {@link ServiceInfo} will be
     *                 retrieved
     * @return a {@link Map} of {@link Service} object mapping to the associated {@link ServiceInfo} returned from the
     * individual services, wrapped in a {@link Future}
     */
    @Nonnull
    public Future<Map<Service, ServiceInfo>> getInfo(@Nonnull final Collection<Service> services) {
        Objects.requireNonNull(services);
        return getExecutor().submit(() -> getMap(services, "service/info", new ServiceInfo.ServiceInfoConverter())
                .get(10, TimeUnit.SECONDS));
    }

    /**
     * @param service the {@link Service} for which a {@link ServiceMemory} will be retrieved
     * @return the {@link ServiceMemory} returned from the service wrapped in a {@link Future}
     */
    @Nonnull
    public Future<ServiceMemory> getMemory(@Nonnull final Service service) {
        Objects.requireNonNull(service);
        return getExecutor().submit(() -> get(service, "service/memory", new ServiceMemory.ServiceMemoryConverter()));
    }

    /**
     * @param services the {@link Collection} of {@link Service} objects for which a {@link ServiceMemory} will be
     *                 retrieved
     * @return a {@link Map} of {@link Service} object mapping to the associated {@link ServiceMemory} returned from
     * the individual services, wrapped in a {@link Future}
     */
    @Nonnull
    public Future<Map<Service, ServiceMemory>> getMemory(@Nonnull final Collection<Service> services) {
        Objects.requireNonNull(services);
        return getExecutor().submit(() -> getMap(services, "service/memory", new ServiceMemory.ServiceMemoryConverter())
                .get(10, TimeUnit.SECONDS));
    }

    /**
     * @param service the {@link Service} to be controlled
     * @param url     the URL path of the control operation
     * @return the resulting {@link ServiceControlStatus} returned from the service
     */
    @Nonnull
    protected Future<ServiceControlStatus> control(@Nonnull final Service service, @Nonnull final String url) {
        Objects.requireNonNull(service);
        return getExecutor().submit(() -> get(service, url, new ServiceControlStatus.ServiceControlStatusConverter()));
    }

    /**
     * @param services the {@link Collection} of {@link Service} objects to be controlled
     * @param url      the URL path of the control operation
     * @return a {@link Map} of {@link Service} object mapping to the associated {@link ServiceControlStatus} returned
     * from the individual controlled services, wrapped in a {@link Future}
     */
    @Nonnull
    protected Future<Map<Service, ServiceControlStatus>> control(
            @Nonnull final Collection<Service> services, @Nonnull final String url) {
        Objects.requireNonNull(services);
        return getExecutor()
                .submit(() -> getMap(services, url, new ServiceControlStatus.ServiceControlStatusConverter())
                        .get(10, TimeUnit.SECONDS));
    }

    /**
     * @param service the {@link Service} to be stopped
     * @return the resulting {@link ServiceControlStatus} returned from the stopped service
     */
    @Nonnull
    public Future<ServiceControlStatus> stop(@Nonnull final Service service) {
        return control(service, "service/control/stop");
    }

    /**
     * @param services the {@link Collection} of {@link Service} objects to be stopped
     * @return a {@link Map} of {@link Service} object mapping to the associated {@link ServiceControlStatus} returned
     * from the individual stopped services, wrapped in a {@link Future}
     */
    @Nonnull
    public Future<Map<Service, ServiceControlStatus>> stop(@Nonnull final Collection<Service> services) {
        return control(services, "service/control/stop");
    }

    /**
     * @param service the {@link Service} to be restarted
     * @return the resulting {@link ServiceControlStatus} returned from the restarted service
     */
    @Nonnull
    public Future<ServiceControlStatus> restart(@Nonnull final Service service) {
        return control(service, "service/control/restart");
    }

    /**
     * @param services the {@link Collection} of {@link Service} objects to be restarted
     * @return a {@link Map} of {@link Service} object mapping to the associated {@link ServiceControlStatus} returned
     * from the individual restarted services, wrapped in a {@link Future}
     */
    @Nonnull
    public Future<Map<Service, ServiceControlStatus>> restart(@Nonnull final Collection<Service> services) {
        return control(services, "service/control/restart");
    }
}
