package pl.piomin.samples.kubernetes.controller;

import java.util.List;

import pl.piomin.samples.kubernetes.utils.AppVersion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/caller")
public class CallerController {

	private RestTemplate restTemplate;

	CallerController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Value("${spring.application.name}")
	private String appName;
	@Value("${POD_NAME}")
	private String podName;
	@Value("${POD_NAMESPACE}")
	private String podNamespace;

	@Autowired
	private AppVersion appVersion;

	@GetMapping("/ping")
	public String ping(@RequestHeader(name = "X-Version", required = false) String version) {
		String callme = callme(version);
		return appName + "(" + appVersion.getVersionLabel() + "): " + podName + " in " + podNamespace
				+ " is calling " + callme;
	}

	private String callme(String version) {
		HttpEntity httpEntity = null;
		if (version != null) {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.put("X-Version", List.of(version));
			httpEntity = new HttpEntity(map);
		}
		ResponseEntity<String> entity = restTemplate
				.exchange("http://callme-service:8080/callme/ping", HttpMethod.GET, httpEntity, String.class);
		return entity.getBody();
	}

}