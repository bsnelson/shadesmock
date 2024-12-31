package org.bsnelson;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ApplicationConfig {
    private ServerConfig server;
    private SpringConfig spring;
    private LoggingConfig logging;
    private DownstreamConfig downstream;

    @Data
    @NoArgsConstructor
    public static class ServerConfig {
        private int port;
    }

    @Data
    @NoArgsConstructor
    public static class SpringConfig {
        private SpringApplicationConfig application;

        @Data
        @NoArgsConstructor
        public static class SpringApplicationConfig {
            private String name;
        }
    }

    @Data
    @NoArgsConstructor
    public static class LoggingConfig {
        private Map<String, String> level;
    }

    @Data
    @NoArgsConstructor
    public static class DownstreamConfig {
        private SomaConfiguration soma;
        private SunsaConfiguration sunsa;
        private ApiConfig api;
        private List<DeviceConfig> devices;
        private Integer retries;

        @Data
        @NoArgsConstructor
        public static class ApiConfig {
            private SomaApiConfiguration soma;
            private SunsaApiConfiguration sunsa;

            @Data
            @NoArgsConstructor
            public static class SomaApiConfiguration {
                private PathConfig listDevices;
                private PathConfig openShade;
                private PathConfig closeShade;
                private PathConfig closeAllShades;
                private PathConfig setShadePosition;
                private PathConfig stopShade;
                private PathConfig getShadeState;
                private PathConfig getLightLevel;
                private PathConfig getBatteryLevel;
            }

            @Data
            @NoArgsConstructor
            public static class SunsaApiConfiguration {
                private PathConfig listDevices;
                private PathConfig setShadePosition;
            }
        }

        @Data
        @NoArgsConstructor
        public static class SomaConfiguration {
            private String connectIp;
        }

        @Data
        @NoArgsConstructor
        public static class SunsaConfiguration {
            private String baseUrl;
            private String apiKey;
            private String idUser;
        }

        @Data
        @NoArgsConstructor
        public static class DeviceConfig {
            private String id;
            private String type;
            private String name;
            private int seasonalDefault;
            private List<String> groups;
        }

        @Data
        @NoArgsConstructor
        public static class PathConfig {
            private String path;
        }
    }
}