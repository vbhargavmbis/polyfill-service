package org.polyfill.controllers;

import org.polyfill.components.Feature;
import org.polyfill.components.Polyfill;
import org.polyfill.components.Query;
import org.polyfill.interfaces.PolyfillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by smo on 3/25/17.
 */
@Controller
public class TestController {

    // supported query params
    private static final String FEATURE = "feature";
    private static final String UA_OVERRIDE = "ua";
    private static final String MODE = "mode";

    @Autowired
    PolyfillService polyfillService;

    /**
     * Params
     * - feature
     *   - feature to run mocha tests
     * - mode
     *   - control: all features are allowed, tests served, no polyfills
     *   - all: all features are allowed, tests and polyfills both served
     *   - targeted: only targeted features are allowed, tests and polyfills both served
     * - ua
     *   - user agent
     *
     * @param headerUA user agent from request header
     * @param params optional query params: mode, ua, and feature
     * @param model data model for runner page
     * @return mocha test runner page
     */
    @RequestMapping(value = "/test/tests", method = RequestMethod.GET)
    public String polyfillsMochaTests(@RequestHeader("User-Agent") String headerUA,
                                      @RequestParam Map<String, String> params,
                                      Model model) {

        String mode = params.getOrDefault(MODE, "all");
        String featureReq = params.getOrDefault(FEATURE, "all");
        String uaString = "targeted".equals(mode) ? params.getOrDefault(UA_OVERRIDE, headerUA) : null;

        List<Feature> reqFeatureList = Collections.singletonList(new Feature(featureReq));
        Query query = new Query(reqFeatureList)
                .setIncludeDependencies(false)
                .setLoadOnUnknownUA(false);

        List<Polyfill> polyfills = getTestPolyfills(uaString, query);
        List<Map<String, Object>> testFeatures = getTestFeatures(polyfills);

        model.addAttribute("featureRequested", featureReq);
        model.addAttribute("loadPolyfill", !"control".equals(mode));
        model.addAttribute("forceAlways", !"targeted".equals(mode));
        model.addAttribute("features", testFeatures);
        model.addAttribute("mode", mode);

        return "tests/browsers/runner";
    }

    private List<Polyfill> getTestPolyfills(String uaString, Query query) {
        Map<String, Polyfill> allPolyfills = polyfillService.getAllPolyfills();
        return polyfillService.getFeatures(query, uaString).stream()
                .map(feature -> allPolyfills.get(feature.getName()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getTestFeatures(List<Polyfill> polyfillList) {
        return polyfillList.stream()
                .filter(polyfill -> !polyfill.getName().startsWith("_"))
                .filter(polyfill -> polyfill.isTestable())
                .map(polyfill -> {
                    Map<String, Object> testFeature = new HashMap<>();
                    testFeature.put("feature", polyfill.getName());

                    String detectSource = polyfill.getDetectSource();
                    testFeature.put("detect", !isEmpty(detectSource) ? detectSource : false);

                    String testsSource = polyfill.getTestsSource();
                    testFeature.put("tests", !isEmpty(testsSource) ? testsSource : false);

                    return testFeature;
                })
                .collect(Collectors.toList());
    }

    private boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }
}
