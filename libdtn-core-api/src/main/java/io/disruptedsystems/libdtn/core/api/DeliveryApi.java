package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.reactivex.rxjava3.core.Completable;

/**
 * DeliveryApi is the registrar-side contract facing DTNCore. It lets Bundle to be delivered
 * to the correct application agent.
 *
 * @author Lucien Loiseau on 24/10/18.
 */
public interface DeliveryApi extends CoreComponentApi {

    class DeliveryDisabled extends Exception {
    }

    class PassiveRegistration extends Exception {
    }

    class UnregisteredSink extends Exception {
    }

    class DeliveryRefused extends Exception {
    }

    /**
     * deliver a bunde to a registered sink. If the action completes, it means that the
     * application agent has accepted the delivery. It may fail if the sink is not currently
     * registered, passive or if the application agent has refused the delivery.
     *
     * @param sink to send the bundle to
     * @param bundle to deliver
     * @return completable that completes upon successful delivery, onerror otherwise.
     */
    Completable deliver(String sink, Bundle bundle);

    /**
     * take care of this bundle for later delivery.
     * todo: probably not an ApiEid of delivery
     *
     * @param sink to deliver the bundle to
     * @param bundle to deliver
     */
    void deliverLater(String sink, final Bundle bundle);

}
