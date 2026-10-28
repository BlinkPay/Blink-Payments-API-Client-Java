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

import java.util.Properties;

/**
 * The implementation of {@link PropertyProvider} for system properties e.g. blinkpay.debit.url.
 */
@Slf4j
public class SystemPropertyProvider extends PropertyProvider {

    /**
     * Default constructor.
     *
     * @param nextPropertyProvider the next {@link PropertyProvider} in the chain
     */
    public SystemPropertyProvider(PropertyProvider nextPropertyProvider) {
        super.nextPropertyProvider = nextPropertyProvider;
    }

    @Override
    public String getProperty(Properties properties, BlinkPayProperty blinkPayProperty) {
        if (blinkPayProperty == null) {
            return null;
        }

        String property = System.getProperty(blinkPayProperty.getPropertyName());
        log.debug("{}: {}", blinkPayProperty.getPropertyName(), property);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }

        if (nextPropertyProvider != null) {
            return nextPropertyProvider.getProperty(properties, blinkPayProperty);
        }

        return null;
    }
}
