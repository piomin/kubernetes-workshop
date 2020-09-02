package pl.piomin.samples.kubernetes.controller;

import pl.piomin.samples.kubernetes.utils.AppVersion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/callme")
public class CallmeController {

	@Value("${spring.application.name}")
	private String appName;
	@Value("${POD_NAME}")
	private String podName;
	@Value("${POD_NAMESPACE}")
	private String podNamespace;

	@Autowired
	private AppVersion appVersion;

	@GetMapping("/ping")
	public String ping() {
		return appName + "(" + appVersion.getVersionLabel() + "): " + podName + " in " + podNamespace;
	}

}
