package io.disruptedsystems.libdtn.core.routing;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.LinkLocalTableApi;
import io.disruptedsystems.libdtn.core.events.ChannelClosed;
import io.disruptedsystems.libdtn.core.events.ChannelOpened;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryDown;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryUp;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * LinkLocalTable is the link-local routing linkLocalTable. It contains all the linklocal Eid
 * associated with their ClaChannelSpi.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class LinkLocalTable extends CoreComponent implements LinkLocalTableApi {

    private static final String TAG = "LinkLocalTable";

    private Set<ClaChannelSpi> linkLocalTable;
    private CoreApi core;

    public LinkLocalTable(CoreApi core) {
        this.core = core;
        linkLocalTable = new HashSet<>();
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
        RxBus.register(this);
    }

    @Override
    protected void componentDown() {
        RxBus.unregister(this);
    }

    private void channelOpened(ClaChannelSpi channel) {
        if (linkLocalTable.add(channel)) {
            channel.recvBundle(
                    core.getExtensionManager(),
                    core.getStorage().getBlobFactory())
                    .subscribe(
                            b -> {
                                core.getLogger().i(TAG, "channel "
                                        + channel.channelEid().getEidString()
                                        + " received a new bundle from "
                                        + b.getSource().getEidString());
                                b.tag("cla-origin-iid", channel.channelEid());
                                core.getBundleProtocol().bundleReception(b);
                            },
                            e -> channelClosed(channel),
                            () -> channelClosed(channel));
            RxBus.post(new LinkLocalEntryUp(channel));
        }
    }

    private void channelClosed(ClaChannelSpi channel) {
        if (linkLocalTable.remove(channel)) {
            RxBus.post(new LinkLocalEntryDown(channel));
        }
    }

    @Override
    public ClaEid isEidLinkLocal(Eid eid) {
        if (!isEnabled()) {
            return null;
        }

        for (ClaChannelSpi cla : linkLocalTable) {
            if (eid.matches(cla.localEid())) {
                return cla.localEid();
            }
        }
        return null;
    }

    @Override
    public Maybe<ClaChannelSpi> findCla(Eid destination) {
        if (!isEnabled()) {
            return Maybe.error(new ComponentIsDownException(TAG));
        }

        return Observable.fromIterable(linkLocalTable)
                .filter(c -> destination.matches(c.channelEid()))
                .lastElement();
    }

    @Override
    public Set<ClaChannelSpi> dumpTable() {
        return Collections.unmodifiableSet(linkLocalTable);
    }


    @Subscribe
    public void onEvent(ChannelOpened event) {
        channelOpened(event.channel);
    }

    @Subscribe
    public void onEvent(ChannelClosed event) {
        channelClosed(event.channel);
    }
}
