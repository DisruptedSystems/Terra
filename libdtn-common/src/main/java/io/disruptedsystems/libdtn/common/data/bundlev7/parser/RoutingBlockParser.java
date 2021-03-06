package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.RoutingBlock;

/**
 * Parser for the {@link RoutingBlock}.
 *
 * @author Lucien Loiseau on 19/01/19.
 */
public class RoutingBlockParser {

    static CborParser getParser(RoutingBlock block, Log logger) {
        return CBOR.parser(); //todo
    }

}

