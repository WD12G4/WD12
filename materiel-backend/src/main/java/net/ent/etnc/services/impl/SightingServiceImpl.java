package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Room;
import net.ent.etnc.models.Sighting;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.repositories.SightingRepository;
import net.ent.etnc.services.SightingService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Service des {@link Sighting} : constats de terrain partagés entre les téléphones de la PWA RECON.
 */
@Slf4j
@Service
public class SightingServiceImpl extends AbstractService<Sighting, SightingRepository> implements SightingService {

    /**
     * Repository des salles, pour rattacher un constat à sa salle.
     */
    private final RoomRepository roomRepository;

    /**
     * Constructeur du service.
     */
    @Autowired
    public SightingServiceImpl(final SightingRepository repository, final RoomRepository roomRepository) {
        super(repository);
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sighting> findAll() {
        return repository.findAll(Sort.by("room.id", "serialNumber"));
    }

    @Override
    @Transactional
    public Sighting record(final String serialNumber, final Long roomId, final Instant seenAt,
                           final String seenBy, final String replaces) {
        String serial = normalize(serialNumber);
        log.debug("Demande de constat [serialNumber={}, roomId={}, seenBy={}]", serial, roomId, seenBy);
        if (serial.isEmpty()) {
            throw new ServiceException("Le numéro de série doit être renseigné.");
        }
        if (serial.length() > 100) {
            throw new ServiceException("La taille du numéro de série ne doit pas dépasser 100 caractères.");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + roomId));

        // Numéro lu puis corrigé par l'agent : l'ancien constat ne correspond à aucun matériel.
        if (replaces != null && !normalize(replaces).equals(serial)) {
            repository.findBySerialNumberAndRoom(normalize(replaces), room).ifPresent(repository::delete);
        }

        // Un scan hors ligne arrive plus tard : sa date est gardée, sans dépasser l'heure du serveur.
        Instant now = Instant.now();
        Instant at = seenAt == null || seenAt.isAfter(now) ? now : seenAt;
        Sighting sighting = repository.findBySerialNumberAndRoom(serial, room)
                .orElseGet(() -> new Sighting(serial, room));
        if (sighting.getSeenAt() == null || at.isAfter(sighting.getSeenAt())) {
            sighting.setSeenAt(at);
            sighting.setSeenBy(seenBy);
        }

        Sighting saved = repository.saveAndFlush(sighting);
        log.info("Constat enregistré [id={}, serialNumber={}, roomId={}]", saved.getId(), serial, roomId);
        return saved;
    }

    @Override
    @Transactional
    public int clearRoom(final Long roomId) {
        int deleted = repository.deleteByRoomId(roomId);
        log.info("Constats de la salle [roomId={}] supprimés : {}", roomId, deleted);
        return deleted;
    }

    /**
     * Même forme que la PWA : sans espaces autour, en majuscules.
     */
    private static String normalize(final String serialNumber) {
        return serialNumber == null ? "" : serialNumber.trim().toUpperCase(Locale.ROOT);
    }
}
