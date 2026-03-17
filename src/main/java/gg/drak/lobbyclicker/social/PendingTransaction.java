package gg.drak.lobbyclicker.social;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter @Setter
public class PendingTransaction {
    private static final Map<String, PendingTransaction> PENDING = new ConcurrentHashMap<>();
    private static final long EXPIRY_MS = 60_000; // 60 seconds

    private final String senderUuid;
    private final String receiverUuid;
    private final BigDecimal amount;
    private final TransactionType type;
    private final long createdAt;

    public PendingTransaction(String senderUuid, String receiverUuid, BigDecimal amount, TransactionType type) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.amount = amount;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRY_MS;
    }

    private static String key(String sender, String receiver) {
        return sender + ":" + receiver;
    }

    public static void add(PendingTransaction tx) {
        PENDING.put(key(tx.senderUuid, tx.receiverUuid), tx);
    }

    public static PendingTransaction get(String senderUuid, String receiverUuid) {
        PendingTransaction tx = PENDING.get(key(senderUuid, receiverUuid));
        if (tx != null && tx.isExpired()) {
            PENDING.remove(key(senderUuid, receiverUuid));
            return null;
        }
        return tx;
    }

    public static PendingTransaction getForReceiver(String receiverUuid) {
        for (PendingTransaction tx : PENDING.values()) {
            if (tx.getReceiverUuid().equals(receiverUuid) && !tx.isExpired()) {
                return tx;
            }
        }
        return null;
    }

    public static void remove(String senderUuid, String receiverUuid) {
        PENDING.remove(key(senderUuid, receiverUuid));
    }

    public static void removeAllFor(String uuid) {
        PENDING.entrySet().removeIf(e ->
                e.getValue().getSenderUuid().equals(uuid) || e.getValue().getReceiverUuid().equals(uuid));
    }

    public static void cleanExpired() {
        PENDING.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
