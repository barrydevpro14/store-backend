package org.store.common.i18n;

import java.util.Locale;

public interface IMessageSourceService {

    String getMessage(String code);

    String getMessage(String code, Object[] args);

    String getMessage(String code, Class<?> classType);

    String getMessage(String code, Object[] args, Locale locale);
}
