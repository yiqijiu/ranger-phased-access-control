package com.ranger.governance.server.whitelist;

public interface TaskWhitelistLookup {
    boolean isWhitelisted(String taskName);
}
