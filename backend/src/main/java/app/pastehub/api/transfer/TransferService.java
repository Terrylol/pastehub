package app.pastehub.api.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private static final Duration TRANSFER_TTL = Duration.ofMinutes(10);
    private static final char[] PICKUP_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
    private static final int MAX_CODE_ATTEMPTS = 8;

    private final TransferRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    @Autowired
    TransferService(TransferRepository repository) {
        this(repository, Clock.systemUTC());
    }

    TransferService(TransferRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    CreatedTransfer create(String content) {
        Instant createdAt = clock.instant();
        String deleteToken = randomValue(TOKEN_ALPHABET, 43);
        String deleteTokenHash = sha256(deleteToken);
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            Transfer transfer = new Transfer(
                    UUID.randomUUID().toString(),
                    randomValue(PICKUP_ALPHABET, 6),
                    content,
                    deleteTokenHash,
                    createdAt,
                    createdAt.plus(TRANSFER_TTL));
            try {
                repository.saveAndFlush(transfer);
                return new CreatedTransfer(transfer.id(), transfer.pickupCode(), transfer.expiresAt(), deleteToken);
            } catch (DataIntegrityViolationException exception) {
                // A code collision is retried; UUID collisions and other database failures remain unlikely but visible.
                if (attempt == MAX_CODE_ATTEMPTS - 1) throw exception;
            }
        }
        throw new IllegalStateException("Unable to allocate pickup code");
    }

    @Transactional
    RetrievedTransfer retrieve(String id) {
        Transfer transfer = requireLive(repository.findById(id).orElseThrow(TransferUnavailableException::new));
        return new RetrievedTransfer(transfer.content(), transfer.expiresAt());
    }

    @Transactional
    String resolveCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        Transfer transfer = requireLive(repository.findByPickupCode(code).orElseThrow(TransferUnavailableException::new));
        return transfer.id();
    }

    public boolean pickupCodeExists(String code) { return repository.findByPickupCode(code).isPresent(); }

    @Transactional
    void delete(String id, String token) {
        Transfer transfer = repository.findById(id).orElseThrow(TransferUnavailableException::new);
        if (!matchesToken(transfer.deleteTokenHash(), token)) throw new DeleteTokenInvalidException();
        if (!transfer.isExpired(clock.instant())) transfer.markDeleted(clock.instant());
    }

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    void removeExpiredTransfers() {
        repository.deleteAll(repository.findByExpiresAtLessThanEqual(clock.instant()));
    }

    private Transfer requireLive(Transfer transfer) {
        if (transfer.isDeleted()) throw new TransferUnavailableException();
        if (transfer.isExpired(clock.instant())) {
            repository.delete(transfer);
            throw new TransferUnavailableException();
        }
        return transfer;
    }

    private String randomValue(char[] alphabet, int length) {
        char[] value = new char[length];
        for (int index = 0; index < length; index++) value[index] = alphabet[secureRandom.nextInt(alphabet.length)];
        return new String(value);
    }

    private boolean matchesToken(String expectedHash, String token) {
        if (token == null || token.isBlank()) return false;
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                sha256(token).getBytes(StandardCharsets.US_ASCII));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    record CreatedTransfer(String id, String pickupCode, Instant expiresAt, String deleteToken) { }
    record RetrievedTransfer(String content, Instant expiresAt) { }
}
