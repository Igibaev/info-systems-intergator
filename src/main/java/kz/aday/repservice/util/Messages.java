package kz.aday.repservice.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Auxiliary class that contains a set of constants with identifiers of resource messages and provides method
 * to get text of messages from resource bundles.
 */
public final class Messages {
    private static final String BUNDLE_NAME = "messages";

    private static final ResourceBundle DEFAULT_RESOURCE_BUNDLE;

    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    static {
        DEFAULT_RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
    }

    private Messages() {
    }

    public static String getText(String key, Object... parameters) {
        try {
            String text;
            if (DEFAULT_RESOURCE_BUNDLE.containsKey(key)) {
                text = DEFAULT_RESOURCE_BUNDLE.getString(key);
            } else {
                text = key;
            }
            return parameters.length == 0 ? text : String.format(text, parameters);
        } catch (Exception e) {
            log.warn(String.format("Resource string for key '%s' is not found", key), e);
            return StringUtils.EMPTY;
        }
    }
}
