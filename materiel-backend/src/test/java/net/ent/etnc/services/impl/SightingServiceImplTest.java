package net.ent.etnc.services.impl;

import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.models.Sighting;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.repositories.SightingRepository;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SightingServiceImpl Unit Tests")
class SightingServiceImplTest {

    @Mock
    private SightingRepository repository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private SightingServiceImpl sightingService;

    private Room room;

    @BeforeEach
    void setUp() {
        Building building = new Building();
        building.setId(1L);
        building.setNumber(101);

        room = new Room();
        room.setId(10L);
        room.setNumber(12);
        room.setBuilding(building);
    }

    /** Le repository renvoie l'entité sauvegardée telle quelle. */
    private void saveReturnsArgument() {
        when(repository.saveAndFlush(any(Sighting.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Tests d'enregistrement (record)")
    class RecordTests {

        @Test
        @DisplayName("Doit créer un constat au numéro normalisé, daté et signé")
        void record_CreatesNormalizedSighting() {
            Instant seenAt = Instant.now().minus(5, ChronoUnit.MINUTES);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(repository.findBySerialNumberAndRoom("SS4SG0548", room)).thenReturn(Optional.empty());
            saveReturnsArgument();

            Sighting saved = sightingService.record("  ss4sg0548 ", 10L, seenAt, "manager", null);

            assertThat(saved.getSerialNumber()).isEqualTo("SS4SG0548");
            assertThat(saved.getRoom()).isEqualTo(room);
            assertThat(saved.getSeenAt()).isEqualTo(seenAt);
            assertThat(saved.getSeenBy()).isEqualTo("manager");
        }

        @Test
        @DisplayName("Doit garder le scan le plus récent quand un scan hors ligne plus ancien arrive")
        void record_KeepsLatestScan() {
            Instant latest = Instant.now().minus(1, ChronoUnit.MINUTES);
            Sighting existing = new Sighting("SS4SG0548", room);
            existing.setSeenAt(latest);
            existing.setSeenBy("agent1");
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(repository.findBySerialNumberAndRoom("SS4SG0548", room)).thenReturn(Optional.of(existing));
            saveReturnsArgument();

            Sighting saved = sightingService.record("SS4SG0548", 10L, latest.minus(1, ChronoUnit.HOURS), "agent2", null);

            assertThat(saved.getSeenAt()).isEqualTo(latest);
            assertThat(saved.getSeenBy()).isEqualTo("agent1");
        }

        @Test
        @DisplayName("Doit dater à l'heure du serveur un scan sans date ou daté dans le futur")
        void record_ClampsFutureDate() {
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(repository.findBySerialNumberAndRoom("SS4SG0548", room)).thenReturn(Optional.empty());
            saveReturnsArgument();

            Sighting saved = sightingService.record("SS4SG0548", 10L, Instant.now().plus(1, ChronoUnit.DAYS), "manager", null);

            assertThat(saved.getSeenAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("Doit retirer le constat du numéro lu puis corrigé par l'agent")
        void record_RemovesReplacedSighting() {
            Sighting wrong = new Sighting("SGY12", room);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(repository.findBySerialNumberAndRoom("SGY12", room)).thenReturn(Optional.of(wrong));
            when(repository.findBySerialNumberAndRoom("SS4SG0548", room)).thenReturn(Optional.empty());
            saveReturnsArgument();

            sightingService.record("SS4SG0548", 10L, null, "manager", "sgy12");

            verify(repository).delete(wrong);
            ArgumentCaptor<Sighting> captor = ArgumentCaptor.forClass(Sighting.class);
            verify(repository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getSerialNumber()).isEqualTo("SS4SG0548");
        }

        @Test
        @DisplayName("Doit refuser un numéro vide")
        void record_RejectsBlankSerial() {
            assertThatThrownBy(() -> sightingService.record("   ", 10L, null, "manager", null))
                    .isInstanceOf(ServiceException.class);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit signaler une salle inconnue")
        void record_UnknownRoom() {
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sightingService.record("SS4SG0548", 99L, null, "manager", null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("Doit supprimer les constats d'une salle")
    void clearRoom_DeletesRoomSightings() {
        when(repository.deleteByRoomId(10L)).thenReturn(3);

        assertThat(sightingService.clearRoom(10L)).isEqualTo(3);
    }
}
