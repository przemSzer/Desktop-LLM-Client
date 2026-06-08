package dev.local.ai;

public class Launcher {

    public static void main(String[] args) {
        GlobalExceptionHandler.install();
        MainApplication.launch(MainApplication.class, args);
    }
}
