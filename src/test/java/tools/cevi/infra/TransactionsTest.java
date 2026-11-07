package tools.cevi.infra;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * UC-005 A2, UC-002 A6: the error handling after a failed store must not fail itself. A failing
 * {@code commit()} has already ended the transaction, so the rollback in the handler has to be a
 * no-op rather than a second exception that replaces the intended answer with an error page.
 */
@QuarkusTest
class TransactionsTest {

    @Test
    void rollback_after_ended_transaction_does_not_throw() {
        QuarkusTransaction.begin();
        QuarkusTransaction.commit();

        assertDoesNotThrow(Transactions::rollbackIfActive);
    }

    @Test
    void rollback_ends_an_open_transaction() {
        QuarkusTransaction.begin();

        Transactions.rollbackIfActive();

        assertThat(QuarkusTransaction.isActive(), is(false));
    }
}
