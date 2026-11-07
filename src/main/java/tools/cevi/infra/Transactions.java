package tools.cevi.infra;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Helpers around the explicit {@link QuarkusTransaction} demarcation the resources use.
 */
public final class Transactions {
    private Transactions() {
    }

    /**
     * Rolls back the transaction of the current request if one is still open.
     * <p>
     * A failing {@code commit()} has already ended the transaction, and a second {@code rollback()}
     * would throw from inside the error handling and replace the intended answer with an error page
     * (UC-005 A2, UC-002 A6).
     */
    public static void rollbackIfActive() {
        if (QuarkusTransaction.isActive()) {
            QuarkusTransaction.rollback();
        }
    }
}
