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
    public static final int SHORT_DELAY = 300;

    public static void main(String[] args) {
        log.info("Hello shadesmock!");
        ApplicationConfig appConfig = getConfig();
        ApplicationConfig.DownstreamConfig downstreamConfig = appConfig.getDownstream();

        WireMockServer wireMockServer = new WireMockServer(options()
                .port(8083)
                .notifier(new ConsoleNotifier(true))
                .extensions(new CustomResponseTransformer()));
        wireMockConfig().notifier(new ConsoleNotifier(true));

        wireMockServer.start();

        somaMock(downstreamConfig, wireMockServer);

        sunsaMock(downstreamConfig, wireMockServer, appConfig);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down WireMock server...");
            wireMockServer.stop();
        }));
    }

    private static void somaMock(ApplicationConfig.DownstreamConfig downstreamConfig, WireMockServer wireMockServer) {
        downstreamConfig.getDevices().stream()
            .filter(device -> device.getType().equals("soma"))
            .filter(device -> device.getGroups().contains("Office"))
            .forEach(device -> {
                somaSetPosition(device, wireMockServer, downstreamConfig, false);
                somaMockTwoSteps(device, wireMockServer, downstreamConfig);
            });

        downstreamConfig.getDevices().stream()
            .filter(device -> device.getType().equals("soma"))
            .filter(device -> device.getGroups().contains("Kitchen"))
            .forEach(device -> {
  //              setPosition(device, wireMockServer, downstreamConfig, false);
                somaSetPositionTwo(device, wireMockServer, downstreamConfig, false);
                somaMockTwoSteps(device, wireMockServer, downstreamConfig);
            });

        downstreamConfig.getDevices().stream()
            .filter(device -> device.getType().equals("soma"))
            .filter(device -> device.getGroups().contains("Living"))
            .forEach(device -> {
                somaSetPosition(device, wireMockServer, downstreamConfig, false);
                somaMockThreeSteps(device, wireMockServer, downstreamConfig);
            });
    }

    private static void sunsaMock(ApplicationConfig.DownstreamConfig downstreamConfig, WireMockServer wireMockServer, ApplicationConfig appConfig) {
        downstreamConfig.getDevices().stream()
                .filter(device -> device.getType().equals("sunsa"))
                .filter(device -> device.getName().equals("MikiOffice"))
                .forEach(device -> {
                    sunsaSetPosition(device, wireMockServer, downstreamConfig, appConfig, false);
                });
    }

    private static void sunsaSetPosition(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig, ApplicationConfig appConfig, boolean makeFail) {
        wireMockServer.stubFor(put(urlMatching(downstreamConfig.getApi().getSunsa().getSetShadePosition().getPath()
            .replace("{idUser}", appConfig.getDownstream().getSunsa().getIdUser())
            .replace("{idDevice}", device.getId()) + "\\?publicApiKey=.*"))
            .inScenario("sunsasetpos" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(SHORT_DELAY, 0.1)
                .withTransformers("custom-response-transformer")
                .withTransformerParameter("deviceName", device.getName())
                .withTransformerParameter("deviceId", device.getId())
                .withBody("{\"device\":{\"idDevice\":" + device.getId() + ",\"name\":\"" + device.getName() + "\",\"position\":100}}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }
    // Will always success.
    private static void somaSetPosition(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig, boolean makeFail) {
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getSoma().getSetShadePosition().getPath()
            .replace("{id}", device.getId().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (makeFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }
    private static void somaSetPositionTwo(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig, boolean finalFail) {
        boolean makeFail = true;
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getSoma().getSetShadePosition().getPath()
            .replace("{id}", device.getId().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (makeFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("nextTime"));
        wireMockServer.stubFor(get(urlMatching(downstreamConfig.getApi().getSoma().getSetShadePosition().getPath()
            .replace("{id}", device.getId().replaceAll(":", SEPER))
            .replace("{position}", "([0-9]*)")))
            .inScenario("setpos" + device.getName())
            .whenScenarioStateIs("nextTime")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"" + (finalFail ? "error" : "success") + "\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\"}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }
    // Will always return closed. simulating retry failure
    private static void somaMockOneStep(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo(STARTED));
    }

    // Will return seasonal setting after one attempt, simulating a single retru
    private static void somaMockTwoSteps(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("adjustedState"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("adjustedState")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":" + device.getSeasonalDefault() + ",\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("adjustedState"));
    }

    // Will return seasonal setting after two attempts, simulating an extended retry.
    private static void somaMockThreeSteps(ApplicationConfig.DownstreamConfig.DeviceConfig device, WireMockServer wireMockServer, ApplicationConfig.DownstreamConfig downstreamConfig) {
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("attemptOne"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("attemptOne")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":100,\"closed_upwards\":true}")
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("attemptTwo"));
        wireMockServer.stubFor(get(urlEqualTo(downstreamConfig.getApi().getSoma().getGetShadeState().getPath().replace("{id}", device.getId().replaceAll(":", SEPER))))
            .inScenario("reopen" + device.getName())
            .whenScenarioStateIs("attemptTwo")
            .willReturn(aResponse()
                .withStatus(200)
                .withLogNormalRandomDelay(DELAY, 0.1)
                .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getId() + "\",\"position\":" + device.getSeasonalDefault() + ",\"closed_upwards\":true}")
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
        System.out.println("Shade API list devices path: " + config.getDownstream().getApi().getSoma().getListDevices().getPath());
        return config;
    }
}