package com.payment.app.data.remote.message.communication

class TcpSocketException : Exception {
    var exceptionType: TcpSocketExceptionType? = null
        private set

    constructor() {}
    constructor(message: String?) : super(message) {}
    constructor(cause: Throwable?, exceptionType: TcpSocketExceptionType?) : super(cause) {
        this.exceptionType = exceptionType
    }

    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}