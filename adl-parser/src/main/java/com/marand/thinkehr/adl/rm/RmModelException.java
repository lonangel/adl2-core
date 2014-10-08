/*
 * Copyright (C) 2014 Marand
 */

package com.marand.thinkehr.adl.rm;

import com.marand.thinkehr.adl.AdlException;

/**
 * @author markopi
 */
public class RmModelException extends AdlException {

    private static final long serialVersionUID = -5462726199786846680L;

    public RmModelException(String message, Object... parameters) {
        super(parameters.length == 0 ? message : String.format(message, parameters));
    }

    public RmModelException(String message, Throwable cause) {
        super(message, cause);
    }
}