package org.springframework.cloud.dataflow.metrics.collector.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.dataflow.metrics.collector.model.Application;
import org.springframework.cloud.dataflow.metrics.collector.model.Instance;
import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.cloud.dataflow.metrics.collector.services.ApplicationMetricsService;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mark Pollack
 */
@RestController
@RequestMapping("/collector/packages/streams")
public class PackageEndpoint {


	private ApplicationMetricsService service;

	private String[] includes = {"spring.cloud.stream.*", "spring.cloud.dataflow.*"};

	private String[] excludes = {};

	public PackageEndpoint(ApplicationMetricsService service) {
		this.service = service;
	}

	@GetMapping
	public String getPackageForStreams(@RequestParam(value = "name", defaultValue = "") String name) throws JsonProcessingException {

		Collection<StreamMetrics> entries = service.toStreamMetrics(name);

		StringBuffer packageString = new StringBuffer();

		Map<String, Map<String,Object>> deployment = new HashMap<>();
		for (StreamMetrics streamMetrics : entries) {
			List<Application> applications = streamMetrics.getApplications();
			for (Application application : applications) {
				Map<String, Object> templateProps = new HashMap<>();
				//TODO figure out how to handle app instance index...
				templateProps.put("name", application.getName());
				List<Instance> instances = application.getInstances();
				Map<String, String> appProps = new HashMap<>();
				for (Instance instance : instances) {
					Map<String, Object> properties = instance.getProperties();
					templateProps.putIfAbsent("count", properties.get("spring.cloud.stream.instanceCount").toString());
					for (String propertyName : properties.keySet()) {
						if (isMatch(propertyName, includes, excludes)) {
							appProps.putIfAbsent(propertyName, properties.get(propertyName).toString());
						}
					}
					templateProps.putIfAbsent("applicationProperties", appProps);
					if (properties.containsKey("spring.cloud.deployer.resourceUrl")) {
						templateProps.putIfAbsent("resource", properties.get("spring.cloud.deployer.resourceUrl").toString());
					}
					if (properties.containsKey("spring.cloud.deployer.resourceMetadataUrl")) {
						templateProps.putIfAbsent("resourceMetadata",
								properties.get("spring.cloud.deployer.resourceMetadataUrl").toString());
					}


				}
				deployment.put(application.getName(), templateProps );

			}

			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writeValueAsString(deployment);
		}


		return packageString.toString();

	}

	private boolean isMatch(String name, String[] includes, String[] excludes) {
		if (ObjectUtils.isEmpty(includes)
				|| PatternMatchUtils.simpleMatch(includes, name)) {
			return !PatternMatchUtils.simpleMatch(excludes, name);
		}
		return false;
	}
}
