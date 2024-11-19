package org.bsnelson;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.sun.tools.javac.Main;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;


@Slf4j
public class ShadesMockServerApp {
    public static final String SEPER = ":";    
    public static final int DELAY = 3000;
    public static void main(String[] args) {
        log.info("Hello shadesmock!");
        ApplicationConfig appConfig = getConfig();
        ApplicationConfig.DownstreamConfig downstreamConfig = appConfig.getDownstream();

        WireMockServer wireMockServer = new WireMockServer(options().port(8083)); //No-args constructor will start on port 8080, no HTTPS
        wireMockConfig().notifier(new ConsoleNotifier(true));

        wireMockServer.start();

        downstreamConfig.getDevices().stream()
            .filter(device -> device.getGroups().contains("Office"))
            .forEach(device -> {
                setPosition(device, wireMockServer, downstreamConfig, false);
                mockTwoSteps(device, wireMockServer, downstreamConfig);
            });

        downstreamConfig.getDevices().stream()
            .filter(device -> device.getGroups().contains("Kitchen"))
            .forEach(device -> {
  //              setPosition(device, wireMockServer, downstreamConfig, false);
                setPositionTwo(device, wireMockServer, downstreamConfig, true);
                mockTwoSteps(device, wireMockServer, downstreamConfig);
            });

        downstreamConfig.getDevices().stream()
            .filter(device -> device.getGroups().contains("Living"))
            .forEach(device -> {
                setPosition(device, wireMockServer, downstreamConfig, false);
                mockThreeSteps(device, wireMockServer, downstreamConfig);
            });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down WireMock server...");
            wireMockServer.stop();
        }));
    }

    // Will always success.
    private static void setPosition(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig, boolean makeFail) {
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getShade().getSetShadePosition().getPath()
            .replace("{mac}", device.getMac().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (makeFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }
    private static void setPositionTwo(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig, boolean finalFail) {
        boolean makeFail = true;
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getShade().getSetShadePosition().getPath()
            .replace("{mac}", device.getMac().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (makeFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("nextTime"));
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getShade().getSetShadePosition().getPath()
            .replace("{mac}", device.getMac().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs("nextTime")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (finalFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }
    // Will always return closed. simulating retry failure
    private static void mockOneStep(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }

    // Will return seasonal setting after one attempt, simulating a single retru
    private static void mockTwoSteps(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("adjustedState"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("adjustedState")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":" + device.getSeasonalDefault() + ",\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("adjustedState"));
    }

    // Will return seasonal setting after two attempts, simulating an extended retry.
    private static void mockThreeSteps(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("attemptOne"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("attemptOne")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("attemptTwo"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getShade().getGetShadeState().getPath().replace("{mac}", device.getMac().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("attemptTwo")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":" + device.getSeasonalDefault() + ",\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("attemptTwo"));
    }

    public static ApplicationConfig getConfig() {
        Yaml yaml = new Yaml(new Constructor(ApplicationConfig.class, new LoaderOptions()));

        // Load the YAML file
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("application.yml");
        ApplicationConfig config = yaml.load(inputStream);

        // Access the properties
        System.out.println("Server port: " + config.getServer().getPort());
        System.out.println("Spring application name: " + config.getSpring().getApplication().getName());
        System.out.println("Logging level ROOT: " + config.getLogging().getLevel().get("ROOT"));
        System.out.println("First device name: " + config.getDownstream().getDevices().getFirst().getName());
        System.out.println("Shade API list devices path: " + config.getDownstream().getApi().getShade().getListDevices().getPath());
        return config;
    }
}