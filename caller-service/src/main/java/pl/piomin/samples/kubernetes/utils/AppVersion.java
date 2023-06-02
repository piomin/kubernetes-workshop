package pl.piomin.samples.kubernetes.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class AppVersion {

    public String getVersionLabel() {
        try (Stream<String> stream = Files.lines(Paths.get("/etc/podinfo/labels"))) {
            Optional<String> optVersion = stream.filter(it -> it.startsWith("version=")).findFirst();
            return optVersion.map(s -> s.split("=")[1].replace("\"", ""))
                    .orElse("null");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "null";
    }
}
