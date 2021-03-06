package io.disruptedsystems.libdtn.core.aa;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.events.RegistrationActive;
import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.core.storage.EventListener;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.eid.ApiEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registrar Routing keeps track of all the registered application agent.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class Registrar extends CoreComponent implements RegistrarApi, DeliveryApi {

    private static final String TAG = "Registrar";

    public class Registration {
        String registeredSink;
        String cookie;
        ActiveRegistrationCallback cb;

        boolean isActive() {
            return cb != passiveRegistration;
        }

        Registration(String sink, ActiveRegistrationCallback cb) {
            this.registeredSink = sink;
            this.cb = cb;
            this.cookie = UUID.randomUUID().toString();
        }
    }

    private CoreApi core;
    private Map<String, Registration> registrations;
    private DeliveryListener listener;

    /**
     * Constructor.
     * @param core reference to the core
     */
    public Registrar(CoreApi core) {
        this.core = core;
        registrations = new ConcurrentHashMap<>();
        listener = new DeliveryListener(core);
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
    }

    @Override
    protected void componentDown() {
    }

    /* ---- helper methods ---- */

    private void checkEnable() throws RegistrarDisabled {
        if (!isEnabled()) {
            throw new RegistrarDisabled();
        }
    }

    private void checkArgumentNotNull(Object obj) throws NullArgument {
        if (obj == null) {
            throw new NullArgument();
        }
    }

    private Registration checkRegisteredSink(String sink)
            throws RegistrarDisabled, SinkNotRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(sink);
        Registration registration = registrations.get(sink);
        if (registration == null) {
            throw new SinkNotRegistered();
        }
        return registration;
    }

    private Registration checkRegisteredSink(String sink, String cookie)
            throws RegistrarDisabled, SinkNotRegistered, BadCookie, NullArgument {
        checkEnable();
        checkArgumentNotNull(sink);
        checkArgumentNotNull(cookie);
        Registration registration = registrations.get(sink);
        if (registration == null) {
            throw new SinkNotRegistered();
        }
        if (!registration.cookie.equals(cookie)) {
            throw new BadCookie();
        }
        return registration;
    }

    private void replaceApiMe(Bundle bundle) throws BundleMalformed {
        try {
            if (bundle.getSource().matches(ApiEid.me())) {
                bundle.setSource(core.getExtensionManager().getEidFactory().create(
                        core.getLocalEid().localEid().getEidString()
                                + ((ApiEid) bundle.getSource()).getPath()));
            }
            if (bundle.getReportto().matches(ApiEid.me())) {
                bundle.setReportto(core.getExtensionManager().getEidFactory().create(
                        core.getLocalEid().localEid().getEidString()
                                + ((ApiEid) bundle.getReportto()).getPath()));
            }
            if (bundle.getDestination().matches(ApiEid.me())) {
                bundle.setDestination(core.getExtensionManager().getEidFactory().create(
                        core.getLocalEid().localEid().getEidString()
                                + ((ApiEid) bundle.getDestination()).getPath()));
            }
        } catch (EidFormatException efe) {
            throw new BundleMalformed(efe.getMessage());
        }
    }

    /* ------  RegistrarApi  is the contract facing ApplicationAgentAdapter ------- */

    @Override
    public boolean isRegistered(String sink) throws RegistrarDisabled, NullArgument {
        checkEnable();
        checkArgumentNotNull(sink);
        return registrations.containsKey(sink);
    }

    @Override
    public String register(String sink)
            throws RegistrarDisabled, SinkAlreadyRegistered, NullArgument {
        return register(sink, passiveRegistration);
    }

    @Override
    public String register(String sink, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, SinkAlreadyRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(sink);
        checkArgumentNotNull(cb);

        Registration registration = new Registration(sink, cb);
        if (registrations.putIfAbsent(sink, registration) == null) {
            core.getLogger().i(TAG, "sink registered: " + sink
                    + " (cookie=" + registration.cookie + ") - "
                    + (cb == passiveRegistration ? "passive" : "active"));
            RxBus.post(new RegistrationActive(sink, registration.cb));
            return registration.cookie;
        }

        throw new SinkAlreadyRegistered();
    }

    @Override
    public boolean unregister(String sink, String cookie)
            throws RegistrarDisabled, SinkNotRegistered, BadCookie, NullArgument {
        checkRegisteredSink(sink, cookie);
        if (registrations.remove(sink) == null) {
            throw new SinkNotRegistered();
        }
        core.getLogger().i(TAG, "sink unregistered: " + sink);
        return true;
    }


    @Override
    public boolean send(Bundle bundle) throws RegistrarDisabled, NullArgument, BundleMalformed {
        checkEnable();
        checkArgumentNotNull(bundle);
        replaceApiMe(bundle);
        core.getBundleProtocol().bundleDispatching(bundle);
        return true;
    }

    @Override
    public boolean send(String sink, String cookie, Bundle bundle)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument, BundleMalformed {
        checkRegisteredSink(sink, cookie);
        checkArgumentNotNull(bundle);
        replaceApiMe(bundle);
        core.getBundleProtocol().bundleDispatching(bundle);
        return true;
    }

    @Override
    public Set<BundleId> checkInbox(String sink, String cookie)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        checkRegisteredSink(sink, cookie);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle get(String sink, String cookie, String bundleId)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        checkRegisteredSink(sink, cookie);
        checkArgumentNotNull(bundleId);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle fetch(String sink, String cookie, String bundleId)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        checkRegisteredSink(sink, cookie);
        checkArgumentNotNull(bundleId);
        return null;
    }

    @Override
    public Flowable<Bundle> fetch(String sink, String cookie)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        checkRegisteredSink(sink, cookie);
        return null;
    }

    @Override
    public boolean setActive(String sink, String cookie, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        checkArgumentNotNull(cb);
        Registration registration = checkRegisteredSink(sink, cookie);
        registration.cb = cb;
        core.getLogger().i(TAG, "registration active: " + sink);
        RxBus.post(new RegistrationActive(sink, registration.cb));
        return true;
    }

    @Override
    public boolean setPassive(String sink)
            throws RegistrarDisabled, SinkNotRegistered, NullArgument {
        Registration registration = checkRegisteredSink(sink);
        registration.cb = passiveRegistration;
        core.getLogger().i(TAG, "registration passive: " + sink);
        return true;
    }

    @Override
    public boolean setPassive(String sink, String cookie)
            throws RegistrarDisabled, BadCookie, SinkNotRegistered, NullArgument {
        Registration registration = checkRegisteredSink(sink, cookie);
        registration.cb = passiveRegistration;
        core.getLogger().i(TAG, "registration passive: " + sink);
        return true;
    }

    /**
     * print the state of the registration table.
     *
     * @return String
     */
    public String printTable() {
        StringBuilder sb = new StringBuilder("\n\ncurrent registration table:\n");
        sb.append("---------------------------\n\n");
        if (isEnabled()) {
            registrations.forEach(
                    (sink, reg) -> {
                        sb.append(sink).append(" ");
                        if (reg.isActive()) {
                            sb.append("ACTIVE\n");
                        } else {
                            sb.append("PASSIVE\n");
                        }
                    }
            );
        } else {
            sb.append("disabled");
        }
        sb.append("\n");
        return sb.toString();
    }

    /* ------  DeliveryApi is the contract facing CoreApi ------- */

    /**
     * DeliveryListener listen for active registration and forward matching undelivered bundle.
     */
    public class DeliveryListener extends EventListener<String> {
        DeliveryListener(CoreApi core) {
            super(core);
        }

        @Override
        public String getComponentName() {
            return "DeliveryListener";
        }

        /**
         * React to RegistrationActive event and forward the relevent bundles.
         * @param event active registration event
         */
        @Subscribe
        public void onEvent(RegistrationActive event) {
            /* deliver every bundle of interest */
            getBundlesOfInterest(event.sink).subscribe(
                    bundleID -> {
                        /* retrieve the bundle */
                        core.getStorage().get(bundleID).subscribe(
                                /* deliver it */
                                bundle -> event.cb.recv(bundle).subscribe(
                                        () -> {
                                            listener.unwatch(event.sink, bundle.bid);
                                            core.getBundleProtocol()
                                                    .bundleLocalDeliverySuccessful(bundle);
                                        },
                                        e -> core.getBundleProtocol()
                                                .bundleLocalDeliveryFailure(event.sink, bundle)),
                                e -> {
                                });
                    });
        }
    }

    /**
     * Deliver a bundle to the registration.
     *
     * @param sink   registered
     * @param bundle to deliver
     * @return completes if the bundle was successfully delivered, onError otherwise
     */
    public Completable deliver(String sink, Bundle bundle) {
        if (!isEnabled()) {
            return Completable.error(new DeliveryDisabled());
        }

        /* first prefix matching strategy */
        for (String registeredSink : registrations.keySet()) {
            if (sink.startsWith(registeredSink)) {
                Registration registration = registrations.get(registeredSink);
                if (registration == null) {
                    return Completable.error(new UnregisteredSink());
                }

                if (!registration.isActive()) {
                    return Completable.error(new PassiveRegistration());
                }

                return registration.cb.recv(bundle);
            }
        }
        return Completable.error(new UnregisteredSink());
    }

    public void deliverLater(String sink, final Bundle bundle) {
        listener.watch(sink, bundle.bid);
    }

    /* passive registration */
    private static ActiveRegistrationCallback passiveRegistration
            = (payload) -> Completable.error(new PassiveRegistration());
}
