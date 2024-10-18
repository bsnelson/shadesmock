package org.bsnelson;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sun.tools.javac.Main;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

@Slf4j
public class ShadesMockServerApp {
    public static void main(String[] args) {
        System.out.println("Hello shadesmock!");
        ApplicationConfig appConfig = getConfig();
        ApplicationConfig.DownstreamConfig downstreamConfig = appConfig.getDownstream();
        Iterator<ApplicationConfig.DownstreamConfig.DeviceConfig> iterator = downstreamConfig.getDevices().iterator();

        WireMockServer wireMockServer = new WireMockServer(options().port(8083)); //No-args constructor will start on port 8080, no HTTPS
        wireMockServer.start();
        while(iterator.hasNext()) {
            ApplicationConfig.DownstreamConfig.DeviceConfig device = iterator.next();
            wireMockServer.stubFor(get(urlEqualTo("/get_shade_state/" + device.getMac().replaceAll(":", "%3A")))
                .inScenario("reopen" + device.getName())
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                    .withStatus(200)
                    .withLogNormalRandomDelay(500, 0.1)
                    .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":0,\"closed_upwards\":true}")
                    .withHeader("Content-Type", "application/json"))
                .willSetStateTo("adjustedState"));
            wireMockServer.stubFor(get(urlEqualTo("/get_shade_state/" + device.getMac().replaceAll(":", "%3A")))
                .inScenario("reopen" + device.getName())
                .whenScenarioStateIs("adjustedState")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withLogNormalRandomDelay(500, 0.1)
                    .withBody("{\"result\":\"success\",\"version\":\"2.3.2\",\"mac\":\"" + device.getMac() + "\",\"position\":" + device.getSeasonalDefault() + ",\"closed_upwards\":true}")
                    .withHeader("Content-Type", "application/json"))
                .willSetStateTo(STARTED));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down WireMock server...");
            wireMockServer.stop();
        }));
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