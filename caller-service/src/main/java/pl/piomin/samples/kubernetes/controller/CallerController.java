package pl.piomin.samples.kubernetes.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import pl.piomin.samples.kubernetes.utils.AppVersion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	private static final Logger LOG = LoggerFactory.getLogger(CallerController.class);
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
	public String ping(@RequestHeader HttpHeaders headers) {
		String callme = callme(headers);
		return appName + "(" + appVersion.getVersionLabel() + "): " + podName + " in " + podNamespace
				+ " is calling " + callme;
	}

	private String callme(HttpHeaders headers) {
		HttpEntity httpEntity = null;
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		Set<String> headerNames = headers.keySet();
		headerNames.forEach(it -> map.put(it, headers.get(it)));
		httpEntity = new HttpEntity(map);
		ResponseEntity<String> entity = restTemplate
				.exchange("http://callme-service.pminkows-serverless.svc.cluster.local/callme/ping", HttpMethod.GET, httpEntity, String.class);
		return entity.getBody();
	}

}
