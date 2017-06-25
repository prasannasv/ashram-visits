package org.ishausa.registration.iii.http;

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapted from http://stackoverflow.com/a/13592567/7247103
 *
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class NameValuePairs {
    public static Map<String, List<String>> splitParams(final String params) {
        if (Strings.isNullOrEmpty(params)) {
            return Collections.emptyMap();
        }

        return Arrays.stream(params.split("&"))
                .map(NameValuePairs::splitParam)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitParam(final String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        String decodedValue;
        try {
            decodedValue = value != null ? URLDecoder.decode(value, "UTF-8") : value;
        } catch (final UnsupportedEncodingException e) {
            decodedValue = value;
        }
        return new AbstractMap.SimpleImmutableEntry<>(key, decodedValue);
    }

    public static String nullSafeGetFirst(final Map<String, List<String>> params, final String fieldName) {
        if (params == null || !params.containsKey(fieldName)) {
            return "";
        }
        final List<String> values = params.get(fieldName);
        return values.size() > 0 ? values.get(0) : "";
    }
}
