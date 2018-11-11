package io.left.rightmesh.libdtn.common.data.bundleV7.parser;

import io.left.rightmesh.libcbor.CBOR;
import io.left.rightmesh.libcbor.CborParser;
import io.left.rightmesh.libdtn.common.data.FlowLabelBlock;
import io.left.rightmesh.libdtn.common.utils.Log;

/**
 * @author Lucien Loiseau on 04/11/18.
 */
public class FlowLabelBlockParser {

    static CborParser getParser(FlowLabelBlock block,  Log logger) {
        return CBOR.parser(); //todo
    }

}