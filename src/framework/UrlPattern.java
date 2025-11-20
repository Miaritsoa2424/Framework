package src.framework;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlPattern {
    private String pattern;
    private Method method;
    private Pattern regex;
    private java.util.List<String> paramNames;

    public UrlPattern(String pattern, Method method) {
        this.pattern = pattern;
        this.method = method;
        this.paramNames = new java.util.ArrayList<>();
        this.regex = buildRegex(pattern);
    }

    private Pattern buildRegex(String pattern) {
        StringBuilder regexBuilder = new StringBuilder("^");
        String[] segments = pattern.split("/");
        
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            
            regexBuilder.append("/");
            
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String paramName = segment.substring(1, segment.length() - 1);
                paramNames.add(paramName);
                regexBuilder.append("([^/]+)");
            } else {
                regexBuilder.append(Pattern.quote(segment));
            }
        }
        
        regexBuilder.append("$");
        return Pattern.compile(regexBuilder.toString());
    }

    public boolean matches(String url) {
        Matcher matcher = regex.matcher(url);
        return matcher.matches();
    }

    public Map<String, String> extractParams(String url) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = regex.matcher(url);
        
        if (matcher.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), matcher.group(i + 1));
            }
        }
        
        return params;
    }

    public String getPattern() {
        return pattern;
    }

    public Method getMethod() {
        return method;
    }

    public java.util.List<String> getParamNames() {
        return paramNames;
    }
}
