package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

/** Business rule conflicts of service orders and physical devices (HTTP 409). */
public class ServiceOrderConflictException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    private ServiceOrderConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ServiceOrderConflictException closed(int orderId) {
        return new ServiceOrderConflictException(
                ErrorCode.SERVICE_ORDER_CLOSED,
                String.format(
                        "A OS %d está encerrada e não pode ser alterada. Abra uma nova OS para o aparelho.",
                        orderId));
    }

    public static ServiceOrderConflictException notClosed(int orderId) {
        return new ServiceOrderConflictException(
                ErrorCode.SERVICE_ORDER_NOT_CLOSED,
                String.format("A OS %d não está encerrada.", orderId));
    }

    public static ServiceOrderConflictException undoCloseExpired(int orderId) {
        return new ServiceOrderConflictException(
                ErrorCode.UNDO_CLOSE_EXPIRED,
                String.format(
                        "O prazo para desfazer o encerramento da OS %d acabou. Abra uma nova OS para o aparelho.",
                        orderId));
    }

    public static ServiceOrderConflictException disposed(int customerDeviceId) {
        return new ServiceOrderConflictException(
                ErrorCode.CUSTOMER_DEVICE_DISPOSED,
                String.format(
                        "O aparelho AP-%d foi descartado e não pode receber uma nova OS.",
                        customerDeviceId));
    }
}
