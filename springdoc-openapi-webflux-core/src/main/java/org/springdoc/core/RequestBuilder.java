package org.springdoc.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

@Component
public class RequestBuilder extends AbstractRequestBuilder {

	public Operation build(Components components, HandlerMethod handlerMethod, RequestMethod requestMethod,
			Operation operation, String[] allConsumes) {
		// Documentation
		operation.setOperationId(handlerMethod.getMethod().getName());
		// requests
		LocalVariableTableParameterNameDiscoverer d = new LocalVariableTableParameterNameDiscoverer();
		String[] pNames = d.getParameterNames(handlerMethod.getMethod());
		List<Parameter> operationParameters = new ArrayList<>();
		java.lang.reflect.Parameter[] parameters = handlerMethod.getMethod().getParameters();

		for (int i = 0; i < pNames.length; i++) {
			// check if query param
			Parameter parameter = null;
			io.swagger.v3.oas.annotations.Parameter parameterDoc = getParameterAnnotation(handlerMethod, parameters[i],
					i, io.swagger.v3.oas.annotations.Parameter.class);

			// use documentation as reference
			if (parameterDoc != null) {
				if (parameterDoc.hidden()) {
					continue;
				}
				parameter = parameterBuilder.buildParameterFromDoc(parameterDoc, null);
			}

			parameter = buildParams(pNames[i], components, parameters[i], i, parameter, handlerMethod);
			// By default
			parameter = buildParamDefault(requestMethod, pNames[i], parameters[i], parameter);

			if (parameter != null && parameter.getName() != null) {
				applyBeanValidatorAnnotations(parameter, Arrays.asList(parameters[i].getAnnotations()));
				operationParameters.add(parameter);
			} else if (!RequestMethod.GET.equals(requestMethod)) {
				RequestPart requestPart = getParameterAnnotation(handlerMethod, parameters[i], i, RequestPart.class);
				String paramName = null;
				if (requestPart != null)
					paramName = StringUtils.defaultIfEmpty(requestPart.value(), requestPart.name());
				paramName = StringUtils.defaultIfEmpty(paramName, pNames[i]);
				RequestBody requestBody = buildRequestBody(components, allConsumes, parameters[i], parameterDoc,
						paramName);
				operation.setRequestBody(requestBody);
			}
		}

		if (!CollectionUtils.isEmpty(operationParameters)) {
			operation.setParameters(operationParameters);
		}

		return operation;
	}
}