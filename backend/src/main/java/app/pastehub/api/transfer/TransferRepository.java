package app.pastehub.api.transfer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransferRepository extends JpaRepository<Transfer, String> {
    Optional<Transfer> findByPickupCode(String pickupCode);
    List<Transfer> findByExpiresAtLessThanEqual(Instant instant);
}
