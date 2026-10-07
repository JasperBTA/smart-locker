package com.smartlocker;

import org.springframework.boot.SpringApplication;

public class TestSmartLockerApplication {

    public static void main(String[] args) {
        SpringApplication.from(SmartLockerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
