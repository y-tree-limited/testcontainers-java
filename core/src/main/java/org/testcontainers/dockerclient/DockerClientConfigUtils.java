package org.testcontainers.dockerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.core.DockerClientConfig;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.DockerClientFactory;

import java.io.File;
import java.net.URI;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class DockerClientConfigUtils {

    // See https://github.com/docker/docker/blob/a9fa38b1edf30b23cae3eade0be48b3d4b1de14b/daemon/initlayer/setup_unix.go#L25
    public static final boolean IN_A_CONTAINER = new File("/.dockerenv").exists();

    @Deprecated
    @Getter(lazy = true)
    private static final Optional<String> detectedDockerHostIp = IN_A_CONTAINER ? getDefaultGateway() : Optional.empty();

    @Getter(lazy = true)
    private static final Optional<String> defaultGateway = Optional
            .ofNullable(
                    (isRunningInK8s()) ? getK8sHost() :
                    DockerClientFactory.instance().runInsideDocker(
                    cmd -> cmd.withCmd("sh", "-c", "ip route|awk '/default/ { print $3 }'"),
                    (client, id) -> {
                        try {
                            LogToStringContainerCallback loggingCallback = new LogToStringContainerCallback();
                            client.logContainerCmd(id).withStdOut(true)
                                    .withFollowStream(true)
                                    .exec(loggingCallback)
                                    .awaitStarted();
                            loggingCallback.awaitCompletion(3, SECONDS);
                            return loggingCallback.toString();
                        } catch (Exception e) {
                            log.warn("Can't parse the default gateway IP", e);
                            return null;
                        }
                    }
            ))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank);

    @SneakyThrows
    private static String getK8sHost() {
        String podName = System.getenv("HOSTNAME");
        KubernetesClient client = new DefaultKubernetesClient();
        Pod k8sPod = client.pods().withName(podName).get();
        return k8sPod.getStatus().getHostIP();
    }

    private static boolean isRunningInK8s() {
        return System.getenv().containsKey("KUBERNETES_SERVICE_HOST");
    }

    /**
     * Use {@link DockerClientFactory#dockerHostIpAddress()}
     */
    @Deprecated
    public static String getDockerHostIpAddress(DockerClientConfig config) {
        return getDockerHostIpAddress(config.getDockerHost());
    }

    public static String getDockerHostIpAddress(URI dockerHost) {
        switch (dockerHost.getScheme()) {
            case "http":
            case "https":
            case "tcp":
                return dockerHost.getHost();
            case "unix":
            case "npipe":
                if (IN_A_CONTAINER) {
                    return getDefaultGateway().orElse("localhost");
                }
                return "localhost";
            default:
                return null;
        }
    }
}
