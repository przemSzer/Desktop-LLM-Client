package dev.local.ai.ui.utils;

import javafx.application.HostServices;

public class HostServicesProvider {

    private HostServices hostServices;

    private static class InternalInstanceHolder {
        private static final HostServicesProvider INSTANCE = new HostServicesProvider();
    }

    public static HostServicesProvider getInstance() {
        return InternalInstanceHolder.INSTANCE;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
