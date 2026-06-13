package org.store.config;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.task.TaskDecorator;

import java.util.Locale;

/**
 * Propagates the caller's LocaleContext into the async thread.
 * Falls back to Locale.FRENCH when no locale is set (e.g. scheduler threads
 * that have no HTTP request context).
 */
public class LocaleAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        LocaleContext callerLocaleContext = LocaleContextHolder.getLocaleContext();
        Locale locale = callerLocaleContext != null ? callerLocaleContext.getLocale() : null;
        Locale effectiveLocale = (locale != null) ? locale : Locale.FRENCH;

        return () -> {
            try {
                LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(effectiveLocale));
                runnable.run();
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
        };
    }
}
