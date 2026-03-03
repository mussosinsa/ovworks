package org.ovirt.engine.core.bll;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TerminalIpConfigUtilsTest {

    @Test
    void shouldKeepRequireIpOnItsOwnLineInsideRequireAllBlock() throws Exception {
        String original = "<LocationMatch ^/ovirt-engine($|/)>\n"
                + "    <RequireAll>\n"
                + "         Require ip 10.10.10.0/24\n"
                + "    </RequireAll>\n"
                + "\n"
                + "    ProxyPassMatch ajp://127.0.0.1:8702 timeout=3600 retry=5\n"
                + "</LocationMatch>\n";

        String updated = TerminalIpConfigUtils.updateRequireIpInContent(original, "192.168.40.0/24");

        assertTrue(updated.contains("<RequireAll>\n         Require ip 192.168.40.0/24\n    </RequireAll>"));
    }
}
