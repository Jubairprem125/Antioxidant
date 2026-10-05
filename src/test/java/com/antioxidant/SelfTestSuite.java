package com.antioxidant;

public final class SelfTestSuite {
    private SelfTestSuite() {}

    public static void main(String[] args) {
        JsonTest.run();
        MappingGeneratorTest.run();
        ResourcePackScannerTest.run();
    }
}