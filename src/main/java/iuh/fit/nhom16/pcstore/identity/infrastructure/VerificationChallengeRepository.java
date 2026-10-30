package iuh.fit.nhom16.pcstore.identity.infrastructure;

import iuh.fit.nhom16.pcstore.identity.domain.VerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, UUID> {

    Optional<VerificationChallenge> findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String target, String purpose);
}
