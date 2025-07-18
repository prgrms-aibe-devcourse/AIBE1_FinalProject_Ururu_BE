package com.ururulab.ururu.global.exception;

import com.ururulab.ururu.global.exception.error.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, Object... args) {
		super(errorCode.formatMessage(args));
		this.errorCode = errorCode;
	}
}
