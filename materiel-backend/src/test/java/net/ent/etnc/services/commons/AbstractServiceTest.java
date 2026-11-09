package net.ent.etnc.services.commons;

import net.ent.etnc.models.Building;
import net.ent.etnc.repositories.BuildingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractService Unit Tests")
class AbstractServiceTest {

    @Mock
    private BuildingRepository repository;

    private TestService service;
    private Building sampleEntity;

    static class TestService extends AbstractService<Building, BuildingRepository> {
        TestService(BuildingRepository repository) {
            super(repository);
        }
    }

    @BeforeEach
    void setUp() {
        service = new TestService(repository);
        sampleEntity = new Building();
        sampleEntity.setId(1L);
        sampleEntity.setNumber(101);
    }

    @Test
    @DisplayName("create doit appeler repository.saveAndFlush")
    void create_CallsSaveAndFlush() {
        when(repository.saveAndFlush(sampleEntity)).thenReturn(sampleEntity);

        Building result = service.create(sampleEntity);

        assertThat(result).isEqualTo(sampleEntity);
        verify(repository).saveAndFlush(sampleEntity);
    }

    @Test
    @DisplayName("update doit appeler repository.saveAndFlush quand l'entité existe")
    void update_WhenExists_CallsSaveAndFlush() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.saveAndFlush(sampleEntity)).thenReturn(sampleEntity);

        Building result = service.update(sampleEntity);

        assertThat(result).isEqualTo(sampleEntity);
        verify(repository).saveAndFlush(sampleEntity);
    }

    @Test
    @DisplayName("update doit lever ResourceNotFoundException quand l'id est null")
    void update_WhenIdIsNull_ThrowsResourceNotFoundException() {
        Building entityWithoutId = new Building();
        entityWithoutId.setNumber(101);

        assertThatThrownBy(() -> service.update(entityWithoutId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ressource introuvable");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("update doit lever ResourceNotFoundException quand l'entité n'existe pas en BDD")
    void update_WhenDoesNotExist_ThrowsResourceNotFoundException() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(sampleEntity))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ressource introuvable : 1");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("findById doit appeler repository.findById")
    void findById_CallsRepository() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        Optional<Building> result = service.findById(1L);

        assertThat(result).contains(sampleEntity);
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("findAll(Pageable) doit appeler repository.findAll")
    void findAll_Pageable_CallsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Building> page = new PageImpl<>(List.of(sampleEntity));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<Building> result = service.findAll(pageable);

        assertThat(result).isEqualTo(page);
        verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("findAll(Specification, Pageable) doit appeler repository.findAll")
    void findAll_SpecificationPageable_CallsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Building> spec = (root, query, cb) -> cb.conjunction();
        Page<Building> page = new PageImpl<>(List.of(sampleEntity));
        when(repository.findAll(spec, pageable)).thenReturn(page);

        Page<Building> result = service.findAll(spec, pageable);

        assertThat(result).isEqualTo(page);
        verify(repository).findAll(spec, pageable);
    }

    @Test
    @DisplayName("deleteById doit appeler repository.deleteById")
    void deleteById_CallsRepository() {
        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("existsById doit appeler repository.existsById")
    void existsById_CallsRepository() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(service.existsById(1L)).isTrue();
        verify(repository).existsById(1L);
    }

    @Test
    @DisplayName("count doit appeler repository.count")
    void count_CallsRepository() {
        when(repository.count()).thenReturn(42L);

        assertThat(service.count()).isEqualTo(42L);
        verify(repository).count();
    }
}
