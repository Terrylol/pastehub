package app.pastehub.api.image;
import java.time.Instant; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface ImageTransferRepository extends JpaRepository<ImageTransfer,String>{ Optional<ImageTransfer> findByPickupCode(String code); List<ImageTransfer> findByExpiresAtLessThanEqual(Instant now); }
