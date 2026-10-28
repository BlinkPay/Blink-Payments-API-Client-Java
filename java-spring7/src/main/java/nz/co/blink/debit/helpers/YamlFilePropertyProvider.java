/**
 * Copyright (c) 2022 BlinkPay
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nz.co.blink.debit.helpers;

import lombok.extern.slf4j.Slf4j;
import nz.co.blink.debit.enums.BlinkPayProperty;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The implementation of {@link PropertyProvider} for {@link Properties} populated from a .yaml/.yml file e.g.
 * <pre>
 * blink:
 *   debit:
 *     url:
 * </pre>
 */
@Slf4j
public class YamlFilePropertyProvider extends PropertyProvider {

    /**
     * Default constructor.
     *
     * @param nextPropertyProvider the next {@link PropertyProvider} in the chain
     */
    public YamlFilePropertyProvider(PropertyProvider nextPropertyProvider) {
        super.nextPropertyProvider = nextPropertyProvider;
    }

    @Override
    public String getProperty(Properties properties, BlinkPayProperty blinkPayProperty) {
        if (blinkPayProperty == null) {
            return null;
        }

        if (properties == null) {
            properties = new Properties();
        }

        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("blinkdebit.yaml");
        if (inputStream == null) {
            log.error("Failed to read properties from blinkdebit.yaml");

            inputStream = getClass().getClassLoader().getResourceAsStream("blinkdebit.yml");
            if (inputStream == null) {
                log.error("Failed to read properties from blinkdebit.yml");
                return nextPropertyProvider.getProperty(properties, blinkPayProperty);
            }
        }

        properties.putAll(getFlattenedMap(yaml.load(inputStream)));

        String property = properties.getProperty(blinkPayProperty.getPropertyName());
        log.debug("{}: {}", blinkPayProperty.getPropertyName(), property);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }

        if (nextPropertyProvider != null) {
            return nextPropertyProvider.getProperty(properties, blinkPayProperty);
        }

        return null;
    }

    private Map<String, String> getFlattenedMap(Map<String, Object> source) {
        Map<String, String> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void buildFlattenedMap(Map<String, String> result, Map<String, Object> source, String path) {
        source.forEach((key, value) -> {
            if (StringUtils.isNotBlank(path)) {
                key = path + (key.startsWith("[") ? key : '.' + key);
            }

            if (value instanceof String) {
                result.put(key, (String) value);
            } else if (value instanceof Integer) {
                result.put(key, String.valueOf(value));
            } else if (value instanceof Map) {
                buildFlattenedMap(result, (Map<String, Object>) value, key);
            } else if (value instanceof Collection) {
                int count = 0;
                for (Object object : (Collection<?>) value) {
                    buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            } else {
                result.put(key, value != null ? value.toString() : "");
            }
        });
    }
}
